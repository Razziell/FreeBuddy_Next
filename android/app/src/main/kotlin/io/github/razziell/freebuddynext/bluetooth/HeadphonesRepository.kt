package io.github.razziell.freebuddynext.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothCodecStatus
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import io.github.razziell.freebuddynext.protocol.AncLevel
import io.github.razziell.freebuddynext.protocol.AncMode
import io.github.razziell.freebuddynext.protocol.DoubleTap
import io.github.razziell.freebuddynext.protocol.DualConnectDevice
import io.github.razziell.freebuddynext.protocol.EqPreset
import io.github.razziell.freebuddynext.protocol.CustomEqualizerPreset
import io.github.razziell.freebuddynext.protocol.EqualizerDiagnostics
import io.github.razziell.freebuddynext.protocol.HeadphonesProtocol
import io.github.razziell.freebuddynext.protocol.AncModule
import io.github.razziell.freebuddynext.protocol.BatteryModule
import io.github.razziell.freebuddynext.protocol.AutoPauseModule
import io.github.razziell.freebuddynext.protocol.CustomEqualizerModule
import io.github.razziell.freebuddynext.protocol.DoubleTapModule
import io.github.razziell.freebuddynext.protocol.DualConnectModule
import io.github.razziell.freebuddynext.protocol.EqualizerModule
import io.github.razziell.freebuddynext.protocol.EarbudSide
import io.github.razziell.freebuddynext.protocol.FindEarbudsModule
import io.github.razziell.freebuddynext.protocol.HoldModule
import io.github.razziell.freebuddynext.protocol.LdacModule
import io.github.razziell.freebuddynext.protocol.LowLatencyModule
import io.github.razziell.freebuddynext.protocol.SwipeModule
import io.github.razziell.freebuddynext.protocol.SplitHoldAction
import io.github.razziell.freebuddynext.protocol.SplitHoldModule
import io.github.razziell.freebuddynext.protocol.TripleTapModule
import io.github.razziell.freebuddynext.protocol.isDualConnectChangeEvent
import io.github.razziell.freebuddynext.protocol.module
import io.github.razziell.freebuddynext.protocol.HeadphonesProtocolRegistry
import io.github.razziell.freebuddynext.protocol.HeadphonesSnapshot
import io.github.razziell.freebuddynext.protocol.Hold
import io.github.razziell.freebuddynext.protocol.MbbCommand
import io.github.razziell.freebuddynext.protocol.Swipe
import io.github.razziell.freebuddynext.protocol.TripleTap
import io.github.razziell.freebuddynext.protocol.toDiagnosticString
import io.github.razziell.freebuddynext.protocol.validHoldModesOrDefault
import io.github.razziell.freebuddynext.data.ThemeStore
import io.github.razziell.freebuddynext.widget.WidgetStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

sealed interface ConnectionState {
    data object PermissionRequired : ConnectionState
    data object BluetoothDisabled : ConnectionState
    data object NoSupportedDevices : ConnectionState
    data class Disconnected(val devices: List<String>) : ConnectionState
    data class Connecting(val deviceName: String) : ConnectionState
    data class Connected(val snapshot: HeadphonesSnapshot) : ConnectionState
    data class Failed(val deviceName: String, val message: String?) : ConnectionState
}

@SuppressLint("MissingPermission")
class HeadphonesRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = RfcommClient()
    private val connectionMutex = Mutex()
    private val widgetRefreshMutex = Mutex()
    private val widgetReports = MutableStateFlow(WidgetReportReceipt())
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected(emptyList()))
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    /**
     * Active A2DP codec name (SBC/AAC/LDAC/...) or null when unknown.
     * Updated from ACTION_CODEC_CONFIG_CHANGED and cached per Bluetooth address.
     */
    private val _activeCodec = MutableStateFlow<String?>(null)
    val activeCodec: StateFlow<String?> = _activeCodec.asStateFlow()

    @Volatile private var a2dp: BluetoothA2dp? = null
    @Volatile private var profileProxyRequested = false
    @Volatile private var activeDevice: BluetoothDevice? = null
    @Volatile private var activeProtocol: HeadphonesProtocol? = null
    @Volatile private var connectJob: Job? = null
    @Volatile private var livenessJob: Job? = null
    @Volatile private var lowBatterySettings = LowBatteryAlertSettings()
    private var receiverRegistered = false
    private val codecPreferences = context.getSharedPreferences(
        BluetoothCodecNames.CACHE_PREFERENCES,
        Context.MODE_PRIVATE,
    )

    private val adapter: BluetoothAdapter?
        get() = context.getSystemService(BluetoothManager::class.java)?.adapter

    private val eventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val device = getDevice(intent)
            val a2dpState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
            val codecName = if (action == BluetoothCodecNames.CODEC_CONFIG_CHANGED_ACTION) {
                codecNameFromIntent(intent)
            } else null
            scope.launch {
                // BLUETOOTH_CONNECT can be revoked while the process is alive;
                // any BluetoothDevice/proxy call then throws SecurityException,
                // which would crash the app from an unhandled coroutine.
                try {
                    handleEvent(action, device, a2dpState, codecName)
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (_: SecurityException) {
                    _state.value = ConnectionState.PermissionRequired
                }
            }
        }
    }

    private suspend fun handleEvent(
        action: String,
        device: BluetoothDevice?,
        a2dpState: Int,
        codecName: String?,
    ) {
        when (action) {
            BluetoothCodecNames.CODEC_CONFIG_CHANGED_ACTION -> {
                if (device != null && (activeDevice == null || device.sameAs(activeDevice))) {
                    codecName?.let { name ->
                        _activeCodec.value = name
                        codecPreferences.edit().putString(device.address, name).apply()
                    }
                }
            }
            BluetoothAdapter.ACTION_STATE_CHANGED -> refresh()
            BluetoothDevice.ACTION_ACL_CONNECTED -> if (device?.protocol() != null) {
                delay(1200)
                connectIfAvailable(device)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED ->
                if (device?.protocol() != null) handlePossibleDisconnect(device)
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                when {
                    // Buds placed in the case: A2DP drops immediately even
                    // though our RFCOMM socket still pins the ACL link.
                    a2dpState == BluetoothProfile.STATE_DISCONNECTED &&
                        device?.protocol() != null -> handlePossibleDisconnect(device)
                    a2dpState == BluetoothProfile.STATE_CONNECTED &&
                        device?.protocol() != null -> {
                        delay(1200)
                        connectIfAvailable(device)
                    }
                }
            }
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> refresh()
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.A2DP) {
                a2dp = proxy as? BluetoothA2dp
                profileProxyRequested = false
                scope.launch { refresh() }
            }
        }
        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) {
                a2dp = null
                profileProxyRequested = false
                scope.launch { disconnectActive(); refresh() }
            }
        }
    }

    // Raw diagnostics live outside the StateFlow: putting them into the
    // snapshot forced a UI recomposition for every incoming packet (including
    // unchanged battery re-sends), plus a payload+CRC re-serialization each time.
    private val diagnosticLog = ArrayDeque<MbbCommand>()

    private fun recordDiagnostic(command: MbbCommand) = synchronized(diagnosticLog) {
        diagnosticLog.addLast(command)
        while (diagnosticLog.size > RAW_RESPONSE_LIMIT) diagnosticLog.removeFirst()
    }

    fun backgroundDiagnostics(): List<String> = BackgroundDiagnostics.read(context)

    /** Formatted lazily, only when the diagnostics dialog/clipboard needs it. */
    fun diagnosticLog(): List<String> = synchronized(diagnosticLog) {
        diagnosticLog.map { it.toDiagnosticString() }
    }

    init {
        scope.launch {
            client.commands.collect { command ->
                recordDiagnostic(command)
                val reportAtMillis = System.currentTimeMillis()
                val reportSession = widgetReports.value.session
                val reportProtocol = activeProtocol
                var batteryChanged = false
                var ancChanged = false
                _state.update { state ->
                    val current = (state as? ConnectionState.Connected)?.snapshot ?: return@update state
                    val protocol = activeProtocol ?: return@update state
                    val updated = protocol.reduce(current, command)
                    batteryChanged = updated.battery != current.battery
                    ancChanged = updated.ancMode != current.ancMode
                    if (updated == current) state else ConnectionState.Connected(updated)
                }
                // Write both values first, then push widgets once: a packet that
                // changes battery AND ANC used to trigger two full updateAll passes.
                val snapshot = (_state.value as? ConnectionState.Connected)?.snapshot
                val batteryReport = reportProtocol?.module<BatteryModule>()?.isStateReport(command) == true
                val ancReport = reportProtocol?.module<AncModule>()?.isStateReport(command) == true
                val widgetsDirty =
                    (if (batteryChanged || batteryReport) snapshot?.battery?.let { WidgetStorage.saveBattery(context, it) } == true else false) or
                        (if (snapshot != null && (ancChanged || ancReport)) WidgetStorage.saveAnc(context, snapshot.ancMode, connected = true) else false)
                if (snapshot != null && reportProtocol === activeProtocol && (batteryReport || ancReport)) {
                    // Count identical replies too; a worker must not mistake cached values
                    // for a fresh report or wait forever because percentages did not change.
                    widgetReports.update { receipt ->
                        if (receipt.session != reportSession) receipt else receipt.copy(
                            battery = receipt.battery + (if (batteryReport) 1 else 0),
                            anc = receipt.anc + (if (ancReport) 1 else 0),
                            batteryAtMillis = if (batteryReport) reportAtMillis else receipt.batteryAtMillis,
                            ancAtMillis = if (ancReport) reportAtMillis else receipt.ancAtMillis,
                        )
                    }
                }
                if (widgetsDirty) WidgetStorage.requestDataUpdate(context)
                if (snapshot != null && (batteryChanged || batteryReport)) {
                    try {
                        LowBatteryNotifier.onSettingsOrBatteryUpdated(context, lowBatterySettings, snapshot)
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (_: Exception) {
                    }
                }
                activeProtocol?.takeIf { it.isDualConnectChangeEvent(command) }?.let { protocol ->
                    protocol.module<DualConnectModule>()?.let { send(it.stateRequest()) }
                    refreshDualConnectDevices()
                }
            }
        }
        // The RFCOMM stream died on its own (no ACL broadcast is guaranteed in
        // that case) - drop the stale Connected state and re-evaluate.
        scope.launch {
            client.connectionLost.collect {
                disconnectActive()
                _state.value = ConnectionState.Disconnected(pairedSupportedNames())
                // The A2DP profile state lags the RFCOMM death by ~50-100ms
                // (seen in field logs); refreshing right away would read a
                // stale "connected" list and re-attach to buds in the case.
                delay(1000)
                refresh()
            }
        }
        scope.launch {
            ThemeStore.lowBatteryAlertSettings(context).collect { alertSettings ->
                lowBatterySettings = alertSettings
                val snapshot = (_state.value as? ConnectionState.Connected)?.snapshot
                LowBatteryNotifier.onSettingsOrBatteryUpdated(context, alertSettings, snapshot)
            }
        }
    }

    fun recheckLowBatteryAlert() {
        scope.launch {
            try {
                val alertSettings = ThemeStore.lowBatteryAlertSettings(context).first()
                lowBatterySettings = alertSettings
                val snapshot = (_state.value as? ConnectionState.Connected)?.snapshot
                LowBatteryNotifier.onSettingsOrBatteryUpdated(context, alertSettings, snapshot)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
            }
        }
    }

    fun sendTestLowBatteryNotification(settings: LowBatteryAlertSettings): Boolean {
        val snapshot = (_state.value as? ConnectionState.Connected)?.snapshot
        return LowBatteryNotifier.sendTestNotification(context, settings, snapshot)
    }

    fun start() {
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                context,
                eventReceiver,
                IntentFilter().apply {
                    addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                    addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                    addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                    addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                    addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
                    addAction(BluetoothCodecNames.CODEC_CONFIG_CHANGED_ACTION)
                },
                // Bluetooth stack runs as its own uid on modern Android; its protected
                // system broadcasts are NOT delivered to non-exported receivers.
                ContextCompat.RECEIVER_EXPORTED,
            )
            receiverRegistered = true
        }
        ensureProfileProxy()
        scope.launch {
            BackgroundDiagnostics.record(context, "repository start")
            if (WidgetStorage.hasPendingPublication(context)) WidgetStorage.requestDataUpdate(context)
            refresh()
        }
    }

    fun refreshAsync() { scope.launch { refresh() } }

    /** Called only while CDM keeps the companion service bound; not an MBB poll. */
    suspend fun reconcileCompanionConnection() = withContext(Dispatchers.IO) {
        // Existing liveness watch handles healthy sessions; do not duplicate its checks.
        if (_state.value !is ConnectionState.Connected && _state.value !is ConnectionState.Connecting) refresh()
    }

    /** Called only by a scheduled worker; completes after fresh widget reports, not after launch(). */
    internal suspend fun refreshForWidgets(event: WidgetRefreshEvent): WidgetRefreshOutcome = withContext(Dispatchers.IO) {
        widgetRefreshMutex.withLock {
            try {
                withTimeoutOrNull(35_000) {
                    if (event.connecting) awaitWidgetReports(event) else reconcileWidgetDisconnect()
                } ?: WidgetRefreshOutcome.RETRY
            } catch (cancel: CancellationException) {
                // A superseding event must not tear down the UI's shared RFCOMM session.
                throw cancel
            } catch (_: SecurityException) {
                disconnectActive()
                _state.value = ConnectionState.PermissionRequired
                WidgetRefreshOutcome.IDLE
            }
        }
    }

    private suspend fun reconcileWidgetDisconnect(): WidgetRefreshOutcome {
        if (!hasPermission()) {
            disconnectActive()
            _state.value = ConnectionState.PermissionRequired
            return WidgetRefreshOutcome.IDLE
        }
        if (adapter?.isEnabled != true) {
            refresh()
            return WidgetRefreshOutcome.IDLE
        }
        // Wait only for the local profile proxy to bind, NOT for a headset to appear.
        // A late disconnect event must not overwrite a newer connected profile state.
        val proxy = withTimeoutOrNull(2_000) {
            while (a2dp == null) {
                ensureProfileProxy()
                delay(100)
            }
            a2dp
        } ?: return WidgetRefreshOutcome.RETRY
        val stillConnected = proxy.connectedDevices.any { it.protocol() != null }
        refresh()
        BackgroundDiagnostics.record(context,
            "disconnect reconciled connected=$stillConnected")
        return WidgetRefreshOutcome.IDLE
    }

    private suspend fun awaitWidgetReports(event: WidgetRefreshEvent): WidgetRefreshOutcome {
        val profileDeadline = SystemClock.elapsedRealtime() + 12_000
        var readAttempts = 0
        while (true) {
            refresh()
            when (_state.value) {
                ConnectionState.PermissionRequired -> {
                    disconnectActive()
                    return WidgetRefreshOutcome.IDLE
                }
                ConnectionState.BluetoothDisabled, ConnectionState.NoSupportedDevices ->
                    return WidgetRefreshOutcome.IDLE
                else -> Unit
            }
            val proxy = a2dp
            val device = activeDevice
            val snapshot = (_state.value as? ConnectionState.Connected)?.snapshot
            val profileReady = proxy?.connectedDevices.orEmpty().any { it.protocol() != null }
            if (!profileReady) {
                // ACL precedes A2DP. Wait rather than interpreting a single empty list
                // as a definitive disconnection; a later A2DP event can replace this job.
                if (SystemClock.elapsedRealtime() >= profileDeadline) {
                    BackgroundDiagnostics.record(context, "a2dp unavailable")
                    return if (proxy == null) WidgetRefreshOutcome.RETRY else WidgetRefreshOutcome.IDLE
                }
            } else if (snapshot != null && device != null &&
                snapshot.address == device.address &&
                proxy?.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED
            ) {
                val protocol = activeProtocol ?: return WidgetRefreshOutcome.RETRY
                val battery = protocol.module<BatteryModule>()
                val anc = protocol.module<AncModule>()
                val baseline = widgetReports.value
                val now = System.currentTimeMillis()
                val needsBattery = battery != null && !baseline.hasFreshBattery(event.sinceMillis, now)
                val needsAnc = anc != null && !baseline.hasFreshAnc(event.sinceMillis, now)
                if (!needsBattery && !needsAnc && widgetReports.value.session == baseline.session &&
                    activeDevice.sameAs(device) && _state.value is ConnectionState.Connected &&
                    a2dp?.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED
                ) {
                    BackgroundDiagnostics.record(context,
                        "fresh battery/anc source=session g=${event.generation}")
                    return WidgetRefreshOutcome.UPDATED
                }
                // The initial request sequence is still sending: let it finish instead
                // of inserting another battery/ANC read into that same connection.
                if (connectJob?.isActive == true) {
                    delay(100)
                    continue
                }
                BackgroundDiagnostics.record(context,
                    "widget read g=${event.generation} battery=$needsBattery anc=$needsAnc")
                val received = withTimeoutOrNull(6_000) {
                    if (needsBattery) battery?.initialRequests?.forEach {
                        client.send(it)
                        delay(35)
                    }
                    if (needsAnc) anc?.let { client.send(it.stateRequest()) }
                    widgetReports.first { receipt ->
                        receipt.session != baseline.session ||
                            receipt.completes(baseline, needsBattery, needsAnc)
                    }
                }
                if (received != null && received.completes(baseline, needsBattery, needsAnc) &&
                    activeDevice.sameAs(device) && _state.value is ConnectionState.Connected &&
                    a2dp?.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED
                ) {
                    BackgroundDiagnostics.record(context, "fresh battery/anc source=request g=${event.generation}")
                    return WidgetRefreshOutcome.UPDATED
                }
                if (++readAttempts >= 2) return WidgetRefreshOutcome.RETRY
                // Retry the reads, but never force-close the UI's shared session:
                // it may be applying settings or playing the earbud-finder sound.
                // Failed/disconnected sessions are retried by refresh() above.
            }
            delay(500)
        }
    }

    fun refreshCodecFromCache(address: String) {
        if (activeDevice?.address == address) {
            _activeCodec.value = codecPreferences.getString(address, null)
        }
    }

    fun reconnect() { scope.launch { activeDevice?.let { connect(it, force = true) } ?: refresh() } }

    private suspend fun refresh() {
        if (!hasPermission()) {
            _state.value = ConnectionState.PermissionRequired
            return
        }
        val bt = adapter
        if (bt == null || !bt.isEnabled) {
            disconnectActive()
            _state.value = ConnectionState.BluetoothDisabled
            return
        }
        ensureProfileProxy()
        val paired = bt.bondedDevices.filter { it.protocol() != null }.sortedBy { it.address }
        if (paired.isEmpty()) {
            disconnectActive()
            _state.value = ConnectionState.NoSupportedDevices
            return
        }
        val connected = a2dp?.connectedDevices.orEmpty().filter { it.protocol() != null }
        val selected = connected.firstOrNull { it.sameAs(activeDevice) } ?: connected.firstOrNull()
        if (selected != null) connectIfAvailable(selected)
        else if (_state.value !is ConnectionState.Connecting) {
            disconnectActive()
            _state.value = ConnectionState.Disconnected(paired.map { it.displayName() })
        }
    }

    private suspend fun connectIfAvailable(device: BluetoothDevice) {
        if ((_state.value as? ConnectionState.Connected)?.snapshot?.address == device.address) return
        connect(device, force = false)
    }

    private suspend fun connect(device: BluetoothDevice, force: Boolean) {
        val protocol = device.protocol() ?: return
        connectionMutex.withLock {
            if (!force && connectJob?.isActive == true) return
            if (!force && activeDevice.sameAs(device) && _state.value is ConnectionState.Connected) return
            connectJob?.cancel()
            connectJob = scope.launch {
                val name = device.displayName()
                ensureProfileProxy()
                val proxy = a2dp
                if (proxy == null || proxy.getConnectionState(device) != BluetoothProfile.STATE_CONNECTED) {
                    // Buds in the case keep accepting RFCOMM; A2DP is the only
                    // signal that they are actually out and in use.
                    if (_state.value !is ConnectionState.Connected) {
                        _state.value = ConnectionState.Disconnected(pairedSupportedNames())
                    }
                    return@launch
                }
                _state.value = ConnectionState.Connecting(name)
                runCatching {
                    client.connect(device, protocol.sppUuid)
                    val established = connectionMutex.withLock {
                        val currentJob = currentCoroutineContext()[Job]
                        val stillConnected = a2dp?.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED
                        if (connectJob !== currentJob || !stillConnected) {
                            false
                        } else {
                            activeDevice = device
                            activeProtocol = protocol
                            widgetReports.update { it.nextSession() }
                            _state.value = ConnectionState.Connected(
                                HeadphonesSnapshot(
                                    protocolId = protocol.id,
                                    name = name,
                                    address = device.address,
                                ),
                            )
                            true
                        }
                    }
                    if (!established) {
                        client.close()
                        if (_state.value is ConnectionState.Connecting) {
                            _state.value = ConnectionState.Disconnected(pairedSupportedNames())
                        }
                        return@launch
                    }
                    ensureActive()
                    BackgroundDiagnostics.record(context, "rfcomm connected")
                    WidgetStorage.saveAncAndUpdate(context, mode = null, connected = true)
                    // MBB pushes updates on its own (battery 1/39, ANC 43/42,
                    // dual-connect 43/54), so no periodic polling is needed.
                    protocol.initialRequests.forEach {
                        client.send(it)
                        delay(35)
                    }
                    // The active codec arrives through ACTION_CODEC_CONFIG_CHANGED.
                    // Use the last event cached for this physical device until the
                    // current connection emits its codec configuration.
                    _activeCodec.value = codecPreferences.getString(device.address, null)
                    startLivenessWatch(device)
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    BackgroundDiagnostics.record(context, "rfcomm error=${error.javaClass.simpleName}")
                    client.close()
                    activeDevice = null
                    activeProtocol = null
                    if (a2dp?.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED) {
                        _state.value = ConnectionState.Failed(name, error.message)
                    } else {
                        // refresh() ignores the Disconnected branch while the state
                        // is still Connecting - reset it first, or the UI would be
                        // stuck on "Connecting..." until the next Bluetooth event.
                        _state.value = ConnectionState.Disconnected(pairedSupportedNames())
                        refresh()
                    }
                }
            }
        }
    }

    /**
     * ACL/A2DP broadcasts are not reliably delivered on every device, and our
     * own RFCOMM socket keeps the ACL link alive, so the buds going into the
     * case can pass silently. Poll the A2DP proxy instead:
     * getConnectionState() is a purely local binder call (no radio, the buds
     * are not woken), so this is battery-free unlike an MBB command watchdog.
     */
    @Suppress("DEPRECATION")
    private fun codecNameFromIntent(intent: Intent): String? {
        val status = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(
                BluetoothCodecStatus.EXTRA_CODEC_STATUS,
                BluetoothCodecStatus::class.java,
            )
        } else {
            intent.getParcelableExtra(BluetoothCodecStatus.EXTRA_CODEC_STATUS)
        } ?: return null
        return BluetoothCodecNames.nameFrom(status)
    }

    private fun startLivenessWatch(device: BluetoothDevice) {
        livenessJob?.cancel()
        livenessJob = scope.launch {
            var proxyMisses = 0
            while (true) {
                delay(4000)
                // The watch is restarted on every successful connect; keeping an
                // orphaned loop alive after Failed/Disconnected would poll forever.
                if (_state.value !is ConnectionState.Connected) break
                ensureProfileProxy()
                val proxy = a2dp
                if (proxy == null) {
                    // Proxy that never comes back (revoked permission, dying BT
                    // stack) would otherwise keep this loop spinning forever
                    // without ever detecting anything.
                    if (!hasPermission() || ++proxyMisses >= 5) break
                    continue
                }
                proxyMisses = 0
                val st = proxy.getConnectionState(device)
                if (st != BluetoothProfile.STATE_CONNECTED) {
                    disconnectActive()
                    refresh()
                    break
                }
                // Codec changes are delivered by ACTION_CODEC_CONFIG_CHANGED.
            }
        }
    }

    private suspend fun handlePossibleDisconnect(device: BluetoothDevice) {
        // activeDevice may already be nulled by a concurrent teardown (seen in
        // field logs), so a disconnect of a supported device is handled even
        // when it no longer matches.
        val active = activeDevice
        if (active == null || device.sameAs(active)) {
            disconnectActive()
            refresh()
        }
    }

    private fun pairedSupportedNames(): List<String> = runCatching {
        adapter?.bondedDevices.orEmpty().filter { it.protocol() != null }
            .sortedBy { it.address }.map { it.displayName() }
    }.getOrDefault(emptyList())

    private suspend fun disconnectActive() {
        // This may run inside connectJob or livenessJob itself (onFailure /
        // liveness-detected drop); cancelling the calling job would make the
        // follow-up refresh() throw CancellationException and skip silently.
        val self = currentCoroutineContext()[Job]
        val (findModule, findState) = connectionMutex.withLock {
            livenessJob?.takeIf { it !== self }?.cancel()
            livenessJob = null
            connectJob?.takeIf { it !== self }?.cancel()
            connectJob = null
            val module = activeProtocol?.module<FindEarbudsModule>()
            val state = (_state.value as? ConnectionState.Connected)?.snapshot?.findEarbuds
            activeDevice = null
            activeProtocol = null
            widgetReports.update { it.nextSession() }
            _activeCodec.value = null
            synchronized(diagnosticLog) { diagnosticLog.clear() }
            module to state
        }
        withContext(NonCancellable) {
            if (findModule != null && findState != null) {
                if (findState.leftSounding) runCatching {
                    client.send(findModule.setSound(EarbudSide.LEFT, false))
                }
                if (findState.rightSounding) runCatching {
                    client.send(findModule.setSound(EarbudSide.RIGHT, false))
                }
            }
            client.close()
            WidgetStorage.saveAncAndUpdate(context, mode = null, connected = false)
        }
    }

    fun setAncMode(mode: AncMode): Boolean {
        val module = activeProtocol?.module<AncModule>() ?: return false
        if (_state.value !is ConnectionState.Connected) return false
        _state.update { state ->
            val connected = state as? ConnectionState.Connected ?: return@update state
            connected.copy(snapshot = connected.snapshot.copy(ancMode = mode))
        }
        scope.launch { WidgetStorage.saveAncAndUpdate(context, mode, connected = true) }
        sendAndRefresh(module.setMode(mode), module.stateRequest(), refreshDelayMs = 400)
        return true
    }

    fun setAncLevel(level: AncLevel) = withModule<AncModule> { module ->
        val mode = (_state.value as? ConnectionState.Connected)?.snapshot?.ancMode
            ?: AncMode.NOISE_CANCELLING
        // Models without configurable levels (e.g. 4i) return null: ignore.
        module.setLevel(mode, level)?.let {
            sendAndRefresh(it, module.stateRequest(), refreshDelayMs = 400)
        }
    }

    fun setEq(value: EqPreset) = withModule<EqualizerModule> {
        sendAndRefresh(it.selectPreset(value), it.stateRequest())
    }

    fun saveCustomEq(preset: CustomEqualizerPreset) = withModule<CustomEqualizerModule> { module ->
        _state.update { state ->
            val connected = state as? ConnectionState.Connected ?: return@update state
            val diagnostics = connected.snapshot.equalizerDiagnostics ?: EqualizerDiagnostics()
            connected.copy(snapshot = connected.snapshot.copy(
                equalizerDiagnostics = diagnostics.copy(
                    activePresetId = preset.id,
                    customPresets = (diagnostics.customPresets.filterNot { it.id == preset.id } + preset).sortedBy { it.id },
                ),
            ))
        }
        sendAndRefresh(module.savePreset(preset), module.stateRequest(), refreshDelayMs = 500)
    }

    fun deleteCustomEq(preset: CustomEqualizerPreset) = withModule<CustomEqualizerModule> { module ->
        _state.update { state ->
            val connected = state as? ConnectionState.Connected ?: return@update state
            val diagnostics = connected.snapshot.equalizerDiagnostics ?: return@update state
            connected.copy(snapshot = connected.snapshot.copy(
                equalizerDiagnostics = diagnostics.copy(
                    activePresetId = if (diagnostics.activePresetId == preset.id) 1 else diagnostics.activePresetId,
                    customPresets = diagnostics.customPresets.filterNot { it.id == preset.id },
                ),
            ))
        }
        sendAndRefresh(module.deletePreset(preset), module.stateRequest(), refreshDelayMs = 500)
    }

    fun replaceCustomEq(presets: List<CustomEqualizerPreset>) = withModule<CustomEqualizerModule> { module ->
        if (presets.isEmpty()) return@withModule
        val current = (_state.value as? ConnectionState.Connected)
            ?.snapshot?.equalizerDiagnostics?.customPresets.orEmpty()
        _state.update { state ->
            val connected = state as? ConnectionState.Connected ?: return@update state
            val diagnostics = connected.snapshot.equalizerDiagnostics ?: EqualizerDiagnostics()
            connected.copy(snapshot = connected.snapshot.copy(
                equalizerDiagnostics = diagnostics.copy(
                    activePresetId = presets.last().id,
                    customPresets = presets,
                ),
            ))
        }
        scope.launch {
            runCatching {
                current.forEach { preset ->
                    client.send(module.deletePreset(preset))
                    delay(100)
                }
                presets.forEach { preset ->
                    client.send(module.savePreset(preset))
                    delay(150)
                }
                delay(500)
                client.send(module.stateRequest())
            }
        }
    }

    fun setAutoPause(value: Boolean) = withModule<AutoPauseModule> {
        sendAndRefresh(it.setEnabled(value), it.stateRequest())
    }
    fun setLowLatency(value: Boolean) = withModule<LowLatencyModule> {
        sendAndRefresh(it.setEnabled(value), it.stateRequest(), refreshDelayMs = 1_000)
    }
    fun setLdac(value: Boolean) = withModule<LdacModule> {
        sendAndRefresh(it.setEnabled(value), it.stateRequest())
    }
    fun setDualConnect(value: Boolean) = withModule<DualConnectModule> {
        sendAndRefresh(it.setEnabled(value), it.stateRequest())
    }

    fun setFindEarbudSound(side: EarbudSide, enabled: Boolean) = withModule<FindEarbudsModule> { module ->
        _state.update { state ->
            val connected = state as? ConnectionState.Connected ?: return@update state
            val find = connected.snapshot.findEarbuds
            connected.copy(snapshot = connected.snapshot.copy(findEarbuds = when (side) {
                EarbudSide.LEFT -> find.copy(leftSounding = enabled)
                EarbudSide.RIGHT -> find.copy(rightSounding = enabled)
            }))
        }
        sendAndRefresh(module.setSound(side, enabled), module.stateRequest(), refreshDelayMs = 500)
    }

    fun stopFindEarbudsSound() = withModule<FindEarbudsModule> { module ->
        _state.update { state ->
            val connected = state as? ConnectionState.Connected ?: return@update state
            connected.copy(snapshot = connected.snapshot.copy(
                findEarbuds = connected.snapshot.findEarbuds.copy(
                    leftSounding = false,
                    rightSounding = false,
                ),
            ))
        }
        scope.launch {
            runCatching {
                client.send(module.setSound(EarbudSide.LEFT, false))
                client.send(module.setSound(EarbudSide.RIGHT, false))
                delay(250)
                client.send(module.stateRequest())
            }
        }
    }

    fun refreshFindEarbuds() = withModule<FindEarbudsModule> { module ->
        send(module.abilityRequest())
        send(module.stateRequest())
    }

    fun refreshDualConnectDevices() = withModule<DualConnectModule> { module ->
        if (_state.value is ConnectionState.Connected) send(module.enumerate())
    }
    fun setDualDeviceConnected(device: DualConnectDevice, connected: Boolean) = withModule<DualConnectModule> {
        sendAndRefreshDual(it.action(device, if (connected) 1 else 2))
    }
    fun setDualDeviceAutoConnect(device: DualConnectDevice, enabled: Boolean) = withModule<DualConnectModule> {
        if (it.supportsAutoConnect) sendAndRefreshDual(it.action(device, if (enabled) 4 else 5))
    }
    fun setDualDevicePreferred(device: DualConnectDevice, enabled: Boolean) = withModule<DualConnectModule> {
        sendAndRefreshDual(it.setPreferred(device, enabled))
    }
    fun unpairDualDevice(device: DualConnectDevice) {
        _state.update { state ->
            val connected = state as? ConnectionState.Connected ?: return@update state
            connected.copy(snapshot = connected.snapshot.copy(
                dualConnectDevices = connected.snapshot.dualConnectDevices.filterNot { it.address == device.address },
            ))
        }
        withModule<DualConnectModule> { sendAndRefreshDual(it.action(device, 3)) }
    }
    fun setDoubleTap(left: DoubleTap? = null, right: DoubleTap? = null) = withModule<DoubleTapModule> {
        sendAndRefresh(it.set(left, right), it.stateRequest())
    }
    fun setTripleTap(left: TripleTap? = null, right: TripleTap? = null) = withModule<TripleTapModule> {
        sendAndRefresh(it.set(left, right), it.stateRequest())
    }
    fun setHold(value: Hold) = withModule<HoldModule> { module ->
        if (value != Hold.CYCLE_ANC) {
            sendAndRefresh(module.setAction(value), module.actionStateRequest())
            return@withModule
        }
        val safeModes = validHoldModesOrDefault(
            (_state.value as? ConnectionState.Connected)?.snapshot?.settings?.holdModes,
        )
        _state.update { state ->
            val connected = state as? ConnectionState.Connected ?: return@update state
            connected.copy(snapshot = connected.snapshot.copy(settings = connected.snapshot.settings.copy(
                hold = Hold.CYCLE_ANC,
                holdModes = safeModes,
            )))
        }
        scope.launch {
            runCatching {
                client.send(module.setAction(Hold.CYCLE_ANC))
                client.send(module.setModes(safeModes))
                delay(500)
                client.send(module.actionStateRequest())
                client.send(module.modesStateRequest())
            }
        }
    }
    fun setHoldModes(value: Set<AncMode>) = withModule<HoldModule> { module ->
        if (value.size < 2) return@withModule
        _state.update { state ->
            val connected = state as? ConnectionState.Connected ?: return@update state
            connected.copy(snapshot = connected.snapshot.copy(
                settings = connected.snapshot.settings.copy(holdModes = value),
            ))
        }
        sendAndRefresh(module.setModes(value), module.modesStateRequest(), refreshDelayMs = 500)
    }

    fun setSplitHoldAction(side: EarbudSide, value: SplitHoldAction) = withModule<SplitHoldModule> { module ->
        val currentSettings = (_state.value as? ConnectionState.Connected)?.snapshot?.settings
        val existingModes = when (side) {
            EarbudSide.LEFT -> currentSettings?.splitHoldModesLeft
            EarbudSide.RIGHT -> currentSettings?.splitHoldModesRight
        }
        val safeModes = if (value == SplitHoldAction.CYCLE_ANC) {
            validHoldModesOrDefault(existingModes)
        } else {
            existingModes
        }
        _state.update { state ->
            val connected = state as? ConnectionState.Connected ?: return@update state
            val settings = connected.snapshot.settings
            connected.copy(snapshot = connected.snapshot.copy(settings = when (side) {
                EarbudSide.LEFT -> settings.copy(splitHoldLeft = value, splitHoldModesLeft = safeModes)
                EarbudSide.RIGHT -> settings.copy(splitHoldRight = value, splitHoldModesRight = safeModes)
            }))
        }
        scope.launch {
            runCatching {
                client.send(module.setAction(side, value))
                if (value == SplitHoldAction.CYCLE_ANC && safeModes != null) {
                    client.send(module.setModes(side, safeModes))
                }
            }
            // Re-read real state even when a write failed, so optimistic UI
            // cannot survive a dropped command on a dying socket.
            delay(500)
            runCatching {
                client.send(module.actionStateRequest())
                client.send(module.modesStateRequest())
            }
        }
    }

    fun setSplitHoldModes(side: EarbudSide, value: Set<AncMode>) = withModule<SplitHoldModule> { module ->
        if (value.size < 2) return@withModule
        _state.update { state ->
            val connected = state as? ConnectionState.Connected ?: return@update state
            val settings = connected.snapshot.settings
            connected.copy(snapshot = connected.snapshot.copy(settings = when (side) {
                EarbudSide.LEFT -> settings.copy(splitHoldModesLeft = value)
                EarbudSide.RIGHT -> settings.copy(splitHoldModesRight = value)
            }))
        }
        sendAndRefresh(module.setModes(side, value), module.modesStateRequest(), refreshDelayMs = 500)
    }

    fun setSwipe(value: Swipe) = withModule<SwipeModule> {
        sendAndRefresh(it.set(value), it.stateRequest())
    }

    private inline fun <reified T : io.github.razziell.freebuddynext.protocol.ProtocolModule> withModule(
        block: (T) -> Unit,
    ) {
        activeProtocol?.module<T>()?.let(block)
    }

    private fun send(command: MbbCommand) { scope.launch { runCatching { client.send(command) } } }
    private fun sendAndRefreshDual(command: MbbCommand) {
        scope.launch {
            runCatching { client.send(command) }
            delay(500)
            refreshDualConnectDevices()
        }
    }
    private fun sendAndRefresh(
        command: MbbCommand,
        refresh: MbbCommand,
        refreshDelayMs: Long = 250,
    ) {
        scope.launch {
            runCatching { client.send(command) }
            delay(refreshDelayMs)
            runCatching { client.send(refresh) }
        }
    }


    @Synchronized
    private fun ensureProfileProxy() {
        if (!hasPermission() || a2dp != null || profileProxyRequested) return
        profileProxyRequested = adapter?.getProfileProxy(
            context,
            profileListener,
            BluetoothProfile.A2DP,
        ) == true
    }

    fun hasPermission(): Boolean = Build.VERSION.SDK_INT < 31 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val RAW_RESPONSE_LIMIT = 32
    }

    private fun BluetoothDevice.protocol(): HeadphonesProtocol? =
        HeadphonesProtocolRegistry.findByBluetoothName(name)

    private fun BluetoothDevice.displayName(): String {
        val deviceAlias = if (Build.VERSION.SDK_INT >= 30) alias else null
        return deviceAlias?.takeIf { it.isNotBlank() } ?: name ?: "FreeBuds"
    }
    private fun BluetoothDevice?.sameAs(other: BluetoothDevice?) = this != null && other != null && address == other.address

    @Suppress("DEPRECATION")
    private fun getDevice(intent: Intent): BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) {
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
}
