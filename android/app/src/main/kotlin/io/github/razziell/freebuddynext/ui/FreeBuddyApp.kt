package io.github.razziell.freebuddynext.ui

import androidx.compose.ui.text.withLink

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.runtime.rememberCoroutineScope
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.razziell.freebuddynext.BuildConfig
import io.github.razziell.freebuddynext.R
import io.github.razziell.freebuddynext.data.HeadphonesIdentity
import io.github.razziell.freebuddynext.data.HomeBlock
import io.github.razziell.freebuddynext.data.SettingsBlock
import io.github.razziell.freebuddynext.data.scaleDp
import io.github.razziell.freebuddynext.data.InterfaceLayout
import io.github.razziell.freebuddynext.data.InterfaceLayoutDefaults
import io.github.razziell.freebuddynext.data.REQUIRED_HOME_BLOCKS
import io.github.razziell.freebuddynext.data.InterfaceLayoutFile
import io.github.razziell.freebuddynext.data.ThemeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.math.hypot
import kotlin.math.max
import io.github.razziell.freebuddynext.bluetooth.ConnectionState
import io.github.razziell.freebuddynext.bluetooth.HeadphonesRepository
import io.github.razziell.freebuddynext.bluetooth.LowBatteryAlertSettings
import io.github.razziell.freebuddynext.bluetooth.LowBatteryNotifier
import io.github.razziell.freebuddynext.protocol.productionDate
import io.github.razziell.freebuddynext.protocol.toggleHoldMode
import io.github.razziell.freebuddynext.protocol.SerialComponent
import io.github.razziell.freebuddynext.protocol.ProductionDate
import io.github.razziell.freebuddynext.protocol.AncLevel
import io.github.razziell.freebuddynext.protocol.AncModule
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
import io.github.razziell.freebuddynext.protocol.module
import io.github.razziell.freebuddynext.protocol.AncMode
import io.github.razziell.freebuddynext.protocol.BatteryLevel
import io.github.razziell.freebuddynext.protocol.BatteryLevels
import io.github.razziell.freebuddynext.protocol.DoubleTap
import io.github.razziell.freebuddynext.protocol.CustomEqualizerPreset
import io.github.razziell.freebuddynext.protocol.EqualizerPresetFile
import io.github.razziell.freebuddynext.protocol.EqualizerPresetFileCodec
import io.github.razziell.freebuddynext.protocol.DualConnectDevice
import io.github.razziell.freebuddynext.protocol.DeviceColor
import io.github.razziell.freebuddynext.protocol.DeviceInfo
import io.github.razziell.freebuddynext.protocol.DualConnectionState
import io.github.razziell.freebuddynext.protocol.EqPreset
import io.github.razziell.freebuddynext.protocol.FreeBudsSettings
import io.github.razziell.freebuddynext.protocol.HeadphonesSnapshot
import io.github.razziell.freebuddynext.protocol.HeadphonesProtocolRegistry
import io.github.razziell.freebuddynext.protocol.Hold
import io.github.razziell.freebuddynext.protocol.Swipe
import io.github.razziell.freebuddynext.protocol.TripleTap

private enum class Screen(val depth: Int) {
    HOME(0),
    HEADPHONES_SETTINGS(1),
    FIND_EARBUDS(2),
    ABOUT(1),
    INTERFACE_EDITOR(2),
}


private val AppCardShape = RoundedCornerShape(16.dp)
private val MenuItemShape = RoundedCornerShape(12.dp)
@Composable
private fun stringResource(@StringRes id: Int, vararg formatArgs: Any): String =
    appStringResource(id, *formatArgs)

@Composable
fun FreeBuddyApp(repository: HeadphonesRepository, requestBluetoothPermission: () -> Unit) {
    val context = LocalContext.current
    val showEmojis by remember { ThemeStore.showEmojis(context) }
        .collectAsStateWithLifecycle(initialValue = true)
    CompositionLocalProvider(LocalShowEmojis provides showEmojis) {
        FreeBuddyTheme {
            var screen by remember { mutableStateOf(Screen.HOME) }
            var lastSettingsScreen by remember { mutableStateOf(Screen.ABOUT) }
            var settingsButtonBounds by remember { mutableStateOf<Rect?>(null) }
            var settingsOverlayBounds by remember { mutableStateOf<Rect?>(null) }
            val state by repository.state.collectAsStateWithLifecycle()
            val activeSnapshot = (state as? ConnectionState.Connected)?.snapshot
            val activeIdentity = activeSnapshot?.colorIdentity()
            val settingsOpen = screen == Screen.ABOUT || screen == Screen.INTERFACE_EDITOR
            val density = LocalDensity.current

            BackHandler(enabled = screen != Screen.HOME) {
                screen = when (screen) {
                    Screen.HOME -> Screen.HOME
                    Screen.HEADPHONES_SETTINGS -> Screen.HOME
                    Screen.FIND_EARBUDS -> Screen.HEADPHONES_SETTINGS
                    Screen.ABOUT -> Screen.HOME
                    Screen.INTERFACE_EDITOR -> {
                        lastSettingsScreen = Screen.ABOUT
                        Screen.ABOUT
                    }
                }
            }

            LaunchedEffect(activeIdentity) {
                activeIdentity?.let { ThemeStore.registerHeadphones(context, it) }
            }

            // About and Interface Editor live in a circular settings container above HOME.
            // The regular navigation transition is retained for headphone-related screens.
            val baseScreen = when (screen) {
                Screen.ABOUT, Screen.INTERFACE_EDITOR -> Screen.HOME
                else -> screen
            }
            val settingsTransition = updateTransition(settingsOpen, label = "settingsReveal")
            val settingsRevealProgress by settingsTransition.animateFloat(
                transitionSpec = {
                    if (targetState) {
                        tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    } else {
                        tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    }
                },
                label = "settingsRevealProgress",
            ) { open -> if (open) 1f else 0f }
            val settingsOverlayVisible = settingsOpen || settingsRevealProgress > 0.001f

            Box(Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = baseScreen,
                    modifier = Modifier.semantics {
                        if (settingsOverlayVisible) invisibleToUser()
                    },
                    transitionSpec = {
                        val forward = targetState.depth >= initialState.depth
                        val slide = if (forward) 1 else -1
                        val spatial = spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessMediumLow,
                            visibilityThreshold = IntOffset.VisibilityThreshold,
                        )
                        (slideInHorizontally(spatial) { it / 8 * slide } + fadeIn())
                            .togetherWith(slideOutHorizontally(spatial) { -it / 8 * slide } + fadeOut())
                    },
                    label = "screen",
                ) { current ->
                    when (current) {
                        Screen.HOME -> HomeScreen(
                            state = state,
                            repository = repository,
                            requestPermission = requestBluetoothPermission,
                            openHeadphonesSettings = { screen = Screen.HEADPHONES_SETTINGS },
                            openAbout = {
                                lastSettingsScreen = Screen.ABOUT
                                screen = Screen.ABOUT
                            },
                            onSettingsButtonBounds = { settingsButtonBounds = it },
                        )
                        Screen.HEADPHONES_SETTINGS -> HeadphonesSettingsScreen(
                            state,
                            repository,
                            openFindEarbuds = { screen = Screen.FIND_EARBUDS },
                            back = { screen = Screen.HOME },
                        )
                        Screen.FIND_EARBUDS -> FindEarbudsScreen(state, repository) {
                            screen = Screen.HEADPHONES_SETTINGS
                        }
                        // Settings screens are rendered in the circular overlay below.
                        Screen.ABOUT, Screen.INTERFACE_EDITOR -> Unit
                    }
                }

                if (settingsOverlayVisible) {
                    val revealShape = GenericShape { size, _ ->
                        val overlay = settingsOverlayBounds
                        val button = settingsButtonBounds
                        val centerX: Float
                        val centerY: Float
                        val startRadius: Float
                        if (overlay != null && button != null) {
                            centerX = button.center.x - overlay.left
                            centerY = button.center.y - overlay.top
                            startRadius = minOf(button.width, button.height) / 2f
                        } else {
                            centerX = size.width
                            centerY = 0f
                            startRadius = 0f
                        }
                        val endRadius = hypot(
                            max(centerX, size.width - centerX),
                            max(centerY, size.height - centerY),
                        )
                        val radius = startRadius +
                            (endRadius - startRadius) * settingsRevealProgress
                        addOval(
                            Rect(
                                centerX - radius,
                                centerY - radius,
                                centerX + radius,
                                centerY + radius,
                            ),
                        )
                    }

                    Box(
                        Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { settingsOverlayBounds = it.boundsInRoot() }
                            .clip(revealShape)
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        AnimatedContent(
                            targetState = lastSettingsScreen,
                            modifier = Modifier.graphicsLayer {
                                alpha = ((settingsRevealProgress - 0.18f) / 0.52f)
                                    .coerceIn(0f, 1f)
                            },
                            transitionSpec = {
                                val forward = targetState.depth >= initialState.depth
                                val slide = if (forward) 1 else -1
                                val spatial = spring(
                                    dampingRatio = 0.86f,
                                    stiffness = Spring.StiffnessMediumLow,
                                    visibilityThreshold = IntOffset.VisibilityThreshold,
                                )
                                (slideInHorizontally(spatial) { it / 10 * slide } + fadeIn())
                                    .togetherWith(
                                        slideOutHorizontally(spatial) { -it / 10 * slide } + fadeOut(),
                                    )
                            },
                            label = "settingsScreen",
                        ) { current ->
                            when (current) {
                                Screen.ABOUT -> AboutScreen(
                                    activeIdentity = activeIdentity,
                                    repository = repository,
                                    openInterfaceEditor = {
                                        lastSettingsScreen = Screen.INTERFACE_EDITOR
                                        screen = Screen.INTERFACE_EDITOR
                                    },
                                    back = { screen = Screen.HOME },
                                )
                                Screen.INTERFACE_EDITOR -> InterfaceEditorScreen(
                                    identity = activeIdentity,
                                    snapshot = activeSnapshot,
                                    repository = repository,
                                    back = {
                                        lastSettingsScreen = Screen.ABOUT
                                        screen = Screen.ABOUT
                                    },
                                )
                                else -> Unit
                            }
                        }

                        // Keep the source icon visible at the reveal origin, enlarge it,
                        // then fade it into the full settings container. During closing
                        // the same icon reappears and shrinks back into the toolbar.
                        val overlay = settingsOverlayBounds
                        val button = settingsButtonBounds
                        if (overlay != null && button != null && settingsRevealProgress < 0.62f) {
                            val iconSizePx = with(density) { 30.dp.roundToPx() }
                            val centerX = button.center.x - overlay.left
                            val centerY = button.center.y - overlay.top
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            (centerX - iconSizePx / 2f).roundToInt(),
                                            (centerY - iconSizePx / 2f).roundToInt(),
                                        )
                                    }
                                    .size(30.dp)
                                    .graphicsLayer {
                                        val phase = (settingsRevealProgress / 0.62f)
                                            .coerceIn(0f, 1f)
                                        scaleX = 1f + phase * 1.8f
                                        scaleY = scaleX
                                        alpha = 1f - phase
                                    },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    state: ConnectionState,
    repository: HeadphonesRepository,
    requestPermission: () -> Unit,
    openHeadphonesSettings: () -> Unit,
    openAbout: () -> Unit,
    onSettingsButtonBounds: (Rect) -> Unit,
) {
    val context = LocalContext.current
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.app_name)) }, actions = {
            IconButton(
                onClick = openAbout,
                modifier = Modifier
                    .size(52.dp)
                    .onGloballyPositioned { onSettingsButtonBounds(it.boundsInRoot()) },
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.about),
                    modifier = Modifier.size(30.dp),
                )
            }
        })
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (state) {
                ConnectionState.PermissionRequired -> StatusContent(R.string.page_home_no_permission, R.string.page_home_no_permission_desc, R.string.grant_access, requestPermission)
                ConnectionState.BluetoothDisabled -> StatusContent(R.string.bluetooth_disabled, R.string.bluetooth_disabled_desc, R.string.open_bluetooth_settings) { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                ConnectionState.NoSupportedDevices -> StatusContent(R.string.not_paired, R.string.not_paired_desc, R.string.open_bluetooth_settings) { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                is ConnectionState.Disconnected -> StatusContent(R.string.disconnected, R.string.disconnected_desc, R.string.open_bluetooth_settings) { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                is ConnectionState.Connecting -> StatusContent(R.string.connecting, R.string.connecting_desc)
                is ConnectionState.Failed -> StatusContent(R.string.connection_failed, R.string.connection_failed_desc, R.string.retry) { repository.reconnect() }
                is ConnectionState.Connected -> ConnectedContent(state.snapshot, repository, openHeadphonesSettings)
            }
        }
    }
}

@Composable
private fun StatusContent(@StringRes title: Int, @StringRes description: Int, @StringRes action: Int? = null, onClick: () -> Unit = {}) {
    Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            stringResource(title),
            modifier = Modifier.semantics {
                heading()
                liveRegion = LiveRegionMode.Polite
            },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(stringResource(description), textAlign = TextAlign.Center)
        action?.let { ExpressiveButton(onClick = onClick) { Text(stringResource(it)) } }
    }
}

@Composable
private fun ConnectedContent(
    snapshot: HeadphonesSnapshot,
    repository: HeadphonesRepository,
    openSettings: () -> Unit,
) {
    val context = LocalContext.current
    val colorIdentity = snapshot.colorIdentity()
    val forceColor by remember(colorIdentity) { ThemeStore.forceHeadphonesColor(context, colorIdentity) }
        .collectAsStateWithLifecycle(initialValue = false)
    val storedColor by remember(colorIdentity) { ThemeStore.headphonesColor(context, colorIdentity) }
        .collectAsStateWithLifecycle(initialValue = DeviceColor.BLACK.name)
    // See HeadphonesSettingsScreen: compose the list only after the persisted
    // order is loaded so LazyColumn does not follow a reordered first item.
    val layoutOrNull by remember { ThemeStore.interfaceLayout(context) }
        .collectAsStateWithLifecycle(initialValue = null)
    val layout = layoutOrNull ?: return
    val selectedColor = storedColor.toDeviceColor()
    val protocol = HeadphonesProtocolRegistry.findById(snapshot.protocolId)
    val ancModule = protocol?.module<AncModule>()
    val dualModule = protocol?.module<DualConnectModule>()
    val activeCodec by repository.activeCodec.collectAsStateWithLifecycle()
    val homeListState = rememberLazyListState()
    var showDeviceInfo by rememberSaveable(snapshot.address) { mutableStateOf(false) }
    var imageBounds by remember { mutableStateOf<Rect?>(null) }
    var overlayOrigin by remember { mutableStateOf<Rect?>(null) }

    BackHandler(enabled = showDeviceInfo) { showDeviceInfo = false }

    // Circular container transform: a circle grows out of the headphones image
    // and reveals the diagnostics card; closing plays the same reveal backwards.
    val infoTransition = updateTransition(showDeviceInfo, label = "deviceInfoReveal")
    val revealProgress by infoTransition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = 380, easing = FastOutSlowInEasing)
            } else {
                tween(durationMillis = 260, easing = FastOutSlowInEasing)
            }
        },
        label = "deviceInfoRevealProgress",
    ) { open -> if (open) 1f else 0f }
    val overlayVisible = showDeviceInfo || revealProgress > 0.001f

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            state = homeListState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.connected),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            snapshot.name,
                            modifier = Modifier.semantics { heading() },
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                        )
                        AnimatedVisibility(
                            visible = activeCodec != null,
                            enter = fadeIn() + scaleIn(initialScale = 0.8f),
                            exit = fadeOut() + scaleOut(targetScale = 0.8f),
                        ) {
                            val name = activeCodec ?: return@AnimatedVisibility
                            val codecDescription = stringResource(R.string.active_codec, name)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    name,
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .semantics { contentDescription = codecDescription },
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
            }
            layout.homeOrder.filterNot { it in layout.hiddenHomeBlocks }.forEach { block ->
                when (block) {
                    HomeBlock.HEADPHONES_IMAGE -> item(key = block.name) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { imageBounds = it.boundsInRoot() },
                            contentAlignment = Alignment.Center,
                        ) {
                            val description = stringResource(R.string.device_info_button, snapshot.name)
                            val scale = layout.imageScalePercent / 100f
                            Image(
                                painter = painterResource(headphonesImage(snapshot, forceColor, selectedColor)),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((180 * scale).dp)
                                    .semantics {
                                        contentDescription = description
                                        role = Role.Button
                                    }
                                    .clickable { showDeviceInfo = true },
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }
                    HomeBlock.BATTERY -> item(key = block.name) {
                        BatteryCard(snapshot.battery, scaleDp(InterfaceLayoutDefaults.BATTERY_ROW_HEIGHT_DP, layout.batteryScalePercent).dp)
                    }
                    HomeBlock.ANC -> if (ancModule != null) item(key = block.name) {
                        AncCard(snapshot, repository, ancModule, scaleDp(InterfaceLayoutDefaults.ANC_BUTTON_SIZE_DP, layout.ancScalePercent).dp)
                    }
                    HomeBlock.DUAL_CONNECT -> if (dualModule != null) item(key = block.name) {
                        DualConnectCard(snapshot, repository, dualModule)
                    }
                    HomeBlock.SETTINGS_BUTTON -> item(key = block.name) {
                        ExpressiveButton(
                            onClick = openSettings,
                            modifier = Modifier.fillMaxWidth(),
                            actionDelayMillis = 170,
                        ) {
                            Text(stringResource(R.string.headphones_settings))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }

        if (overlayVisible) {
            Box(
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { overlayOrigin = it.boundsInRoot() }
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.58f * revealProgress))
                    .pointerInput(Unit) { detectTapGestures { showDeviceInfo = false } },
            ) {
                val revealShape = GenericShape { size, _ ->
                    val origin = overlayOrigin
                    val image = imageBounds
                    val centerX: Float
                    val centerY: Float
                    val startRadius: Float
                    if (origin != null && image != null) {
                        centerX = image.center.x - origin.left
                        centerY = image.center.y - origin.top
                        startRadius = minOf(image.width, image.height) / 2f * 0.55f
                    } else {
                        centerX = size.width / 2f
                        centerY = size.height / 2f
                        startRadius = 0f
                    }
                    val endRadius = hypot(
                        max(centerX, size.width - centerX),
                        max(centerY, size.height - centerY),
                    )
                    val radius = startRadius + (endRadius - startRadius) * revealProgress
                    addOval(
                        Rect(
                            centerX - radius,
                            centerY - radius,
                            centerX + radius,
                            centerY + radius,
                        ),
                    )
                }
                Box(Modifier.fillMaxSize().clip(revealShape)) {
                    DeviceInfoOverlay(
                        snapshot = snapshot,
                        activeCodec = activeCodec,
                        repository = repository,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        onDismiss = { showDeviceInfo = false },
                    )
                }
            }
        }
    }
}

@DrawableRes
private fun headphonesImage(
    snapshot: HeadphonesSnapshot,
    forceColor: Boolean,
    selectedColor: DeviceColor,
): Int {
    val protocol = HeadphonesProtocolRegistry.findById(snapshot.protocolId)
        ?: HeadphonesProtocolRegistry.defaultProfile()
    val color = if (forceColor) selectedColor else protocol.detectColor(snapshot.deviceInfo)
    return when (protocol.id) {
        "freebuds_4i" -> when (color) {
            DeviceColor.WHITE -> R.drawable.freebuds_4i_w
            DeviceColor.RED -> R.drawable.freebuds_4i_r
            DeviceColor.BLACK, DeviceColor.BLUE, DeviceColor.PURPLE, null -> R.drawable.freebuds_4i_b
        }
        "freebuds_se_4_anc" -> when (color) {
            DeviceColor.WHITE -> R.drawable.freebuds_se_4_anc_w
            DeviceColor.BLUE -> R.drawable.freebuds_se_4_anc_bl
            DeviceColor.BLACK, DeviceColor.PURPLE, DeviceColor.RED, null -> R.drawable.freebuds_se_4_anc_b
        }
        "freebuds_6i" -> when (color) {
            DeviceColor.WHITE -> R.drawable.freebuds_6i_w
            DeviceColor.PURPLE -> R.drawable.freebuds_6i_v
            DeviceColor.BLACK, DeviceColor.BLUE, DeviceColor.RED, null -> R.drawable.freebuds_6i_b
        }
        else -> when (color) {
            DeviceColor.WHITE -> R.drawable.freebuds_5i_w
            DeviceColor.BLUE -> R.drawable.freebuds_5i_bl
            DeviceColor.BLACK, DeviceColor.PURPLE, DeviceColor.RED, null -> R.drawable.freebuds_5i_b
        }
    }
}

@Composable
private fun DeviceInfoOverlay(
    snapshot: HeadphonesSnapshot,
    activeCodec: String?,
    repository: HeadphonesRepository,
    modifier: Modifier,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val info = snapshot.deviceInfo
    val eq = snapshot.equalizerDiagnostics
    val locale = LocalConfiguration.current.locales[0]
    val caseProduction = info?.serialNumber?.productionDate(SerialComponent.CASE)
    val leftProduction = info?.leftSerial?.productionDate(SerialComponent.EARBUD)
    val rightProduction = info?.rightSerial?.productionDate(SerialComponent.EARBUD)
    val earbudsProductionMismatch = leftProduction != null && rightProduction != null &&
        leftProduction != rightProduction
    var unknownFieldsExpanded by rememberSaveable(snapshot.address) { mutableStateOf(false) }
    val clipboardLabel = stringResource(R.string.device_info_clipboard_label)
    val detectedColor = HeadphonesProtocolRegistry.findById(snapshot.protocolId)?.detectColor(info)
    val detectedColorLabel = detectedColor?.let { color ->
        stringResource(
            when (color) {
                DeviceColor.BLACK -> R.string.headphones_color_black
                DeviceColor.WHITE -> R.string.headphones_color_white
                DeviceColor.BLUE -> R.string.headphones_color_blue
                DeviceColor.PURPLE -> R.string.headphones_color_purple
                DeviceColor.RED -> R.string.headphones_color_red
            },
        )
    }
    val title = stringResource(R.string.device_info_title)
    val titleFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { titleFocusRequester.requestFocus() }

    Surface(
        modifier = modifier
            .semantics { paneTitle = title }
            .pointerInput(Unit) { detectTapGestures { /* Consume taps inside the card. */ } },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                title,
                modifier = Modifier
                    .focusRequester(titleFocusRequester)
                    .focusable()
                    .semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
            )
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DeviceInfoRow(R.string.device_info_model, info?.model ?: snapshot.name)
                DeviceInfoRow(R.string.device_info_submodel, info?.subModel)
                DeviceInfoRow(R.string.device_info_firmware, info?.firmware)
                DeviceInfoRow(R.string.device_info_hardware, info?.hardwareVersion)
                DeviceInfoRow(R.string.device_info_audio_codec, activeCodec)
                DeviceSerialRow(
                    label = stringResource(R.string.case_label),
                    serial = info?.serialNumber,
                    productionDate = caseProduction?.formatProductionDate(locale),
                )
                DeviceSerialRow(
                    label = stringResource(R.string.left_bud_short),
                    serial = info?.leftSerial,
                    productionDate = leftProduction?.formatProductionDate(locale),
                )
                DeviceSerialRow(
                    label = stringResource(R.string.right_bud_short),
                    serial = info?.rightSerial,
                    productionDate = rightProduction?.formatProductionDate(locale),
                )
                if (earbudsProductionMismatch) {
                    Text(
                        stringResource(R.string.device_info_production_date_warning),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (!info?.unknownFields.isNullOrEmpty()) {
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.device_info_unknown_fields),
                            modifier = Modifier.weight(1f).semantics { heading() },
                            style = MaterialTheme.typography.titleSmall,
                        )
                        IconButton(onClick = { unknownFieldsExpanded = !unknownFieldsExpanded }) {
                            Icon(
                                if (unknownFieldsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = stringResource(if (unknownFieldsExpanded) R.string.collapse else R.string.expand),
                            )
                        }
                    }
                    AnimatedVisibility(visible = unknownFieldsExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            info?.unknownFields?.forEach { (id, value) ->
                                if (id == 25) {
                                    DeviceTlvColorRow(
                                        label = stringResource(R.string.device_info_tlv_field, id),
                                        value = value,
                                        probableColorLabel = stringResource(R.string.device_info_probable_color),
                                        probableColor = detectedColorLabel,
                                    )
                                } else {
                                    DeviceInfoRow(stringResource(R.string.device_info_tlv_field, id), value)
                                }
                            }
                        }
                    }
                }
                if (eq != null) {
                    DiagnosticSectionTitle(R.string.device_info_equalizer)
                    DeviceInfoRow(
                        stringResource(R.string.device_info_eq_active),
                        eq.activePresetId?.toString(),
                    )
                    DeviceInfoRow(
                        stringResource(R.string.device_info_eq_built_in),
                        eq.builtInPresetIds.takeIf { it.isNotEmpty() }?.joinToString(),
                    )
                    if (eq.customPresets.isEmpty()) {
                        DeviceInfoRow(
                            stringResource(R.string.device_info_eq_custom),
                            stringResource(R.string.device_info_eq_none),
                        )
                    } else {
                        eq.customPresets.forEach { preset ->
                            DeviceInfoRow(
                                stringResource(R.string.device_info_eq_preset, preset.id, preset.name),
                                preset.bands.joinToString(),
                            )
                        }
                    }
                    eq.rawFields.forEach { (id, value) ->
                        DeviceInfoRow(stringResource(R.string.device_info_eq_tlv, id), value)
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExpressiveTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val logs = buildString {
                            appendLine("FreeBuddy Next background diagnostics")
                            appendLine("version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                            appendLine("backgroundEvents:")
                            repository.backgroundDiagnostics().forEach { appendLine(it) }
                        }.trimEnd()
                        clipboard.setPrimaryClip(ClipData.newPlainText(clipboardLabel, logs))
                    },
                ) {
                    Text(stringResource(R.string.device_info_copy_logs))
                }
                ExpressiveTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                clipboardLabel,
                                snapshot.toDiagnosticDump(
                                    activeCodec = activeCodec,
                                    rawResponses = repository.diagnosticLog(),
                                ),
                            ),
                        )
                    },
                ) {
                    Text(stringResource(R.string.device_info_copy_raw))
                }
                AppActionButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), filled = true) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

private fun ProductionDate.diagnosticValue(): String = "%04d-%02d".format(Locale.ROOT, year, month)

private fun ProductionDate.formatProductionDate(locale: Locale): String {
    val monthName = Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, locale)
    return monthName.replaceFirstChar { character ->
        if (character.isLowerCase()) character.titlecase(locale) else character.toString()
    } + " " + year
}

@Composable
private fun DiagnosticSectionTitle(@StringRes title: Int) {
    HorizontalDivider(Modifier.padding(vertical = 6.dp))
    Text(
        stringResource(title),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
    )
}

private fun HeadphonesSnapshot.toDiagnosticDump(
    activeCodec: String?,
    rawResponses: List<String>,
): String = buildString {
    appendLine("FreeBuddy Next device diagnostics")
    appendLine("protocol=$protocolId")
    appendLine("name=$name")
    appendLine("address=$address")
    appendLine("audioCodec=${activeCodec.orEmpty()}")
    deviceInfo?.let { info ->
        appendLine("model=${info.model.orEmpty()}")
        appendLine("submodel=${info.subModel.orEmpty()}")
        appendLine("firmware=${info.firmware.orEmpty()}")
        appendLine("hardware=${info.hardwareVersion.orEmpty()}")
        appendLine("caseSerial=${info.serialNumber.orEmpty()}")
        appendLine("caseProduction=${info.serialNumber?.productionDate(SerialComponent.CASE)?.diagnosticValue().orEmpty()}")
        appendLine("leftSerial=${info.leftSerial.orEmpty()}")
        appendLine("leftProduction=${info.leftSerial?.productionDate(SerialComponent.EARBUD)?.diagnosticValue().orEmpty()}")
        appendLine("rightSerial=${info.rightSerial.orEmpty()}")
        appendLine("rightProduction=${info.rightSerial?.productionDate(SerialComponent.EARBUD)?.diagnosticValue().orEmpty()}")
        val detectedColor = HeadphonesProtocolRegistry.findById(protocolId)?.detectColor(info)
        info.unknownFields.forEach { (id, value) ->
            val annotation = if (id == 25) {
                val color = detectedColor?.name?.lowercase()
                if (color == null) " (probable color code)" else " (probable color code: $color)"
            } else ""
            appendLine("deviceInfo.tlv$id=$value$annotation")
        }
    }
    equalizerDiagnostics?.let { eq ->
        appendLine("eq.activePresetId=${eq.activePresetId ?: ""}")
        appendLine("eq.builtInPresetIds=${eq.builtInPresetIds.joinToString()}")
        eq.customPresets.forEach { preset ->
            appendLine("eq.custom.${preset.id}.name=${preset.name}")
            appendLine("eq.custom.${preset.id}.bands=${preset.bands.joinToString()}")
        }
        eq.rawFields.forEach { (id, value) -> appendLine("eq.tlv$id=$value") }
    }
    appendLine("rawResponses:")
    rawResponses.forEach(::appendLine)
}.trimEnd()

@Composable
private fun DeviceSerialRow(label: String, serial: String?, productionDate: String?) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            label,
            modifier = Modifier.weight(0.8f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier.weight(1.6f),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                serial ?: "—",
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (productionDate != null) {
                Text(
                    productionDate,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun DeviceTlvColorRow(
    label: String,
    value: String,
    probableColorLabel: String,
    probableColor: String?,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(0.8f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                probableColorLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Column(
            modifier = Modifier.weight(1.6f),
            horizontalAlignment = Alignment.End,
        ) {
            Text(value, textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium)
            Text(
                probableColor ?: "—",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DeviceInfoRow(@StringRes label: Int, value: String?) =
    DeviceInfoRow(stringResource(label), value)

@Composable
private fun DeviceInfoRow(label: String, value: String?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            label,
            modifier = Modifier.weight(0.8f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value ?: "—",
            modifier = Modifier.weight(1.6f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DualConnectCard(snapshot: HeadphonesSnapshot, repository: HeadphonesRepository, module: DualConnectModule) {
    var expanded by remember { mutableStateOf(false) }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    val selectedDevice = snapshot.dualConnectDevices.firstOrNull { it.address == selectedAddress }
    val headerInteraction = remember { MutableInteractionSource() }
    val headerScale = remember { Animatable(1f) }

    LaunchedEffect(headerInteraction) {
        headerInteraction.interactions.collect { event ->
            if (event is PressInteraction.Press) {
                headerScale.animateTo(0.985f, tween(65))
                headerScale.animateTo(
                    1f,
                    spring(
                        dampingRatio = 0.9f,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
            }
        }
    }

    LaunchedEffect(expanded) {
        if (expanded) repository.refreshDualConnectDevices()
    }

    Card(Modifier.fillMaxWidth(), shape = AppCardShape) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = snapshot.dualConnectEnabled != null,
                        interactionSource = headerInteraction,
                        indication = androidx.compose.foundation.LocalIndication.current,
                    ) { expanded = !expanded }
                    .graphicsLayer { scaleX = headerScale.value; scaleY = headerScale.value }
                    .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.dual_connect),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                )
                Switch(
                    checked = snapshot.dualConnectEnabled ?: false,
                    onCheckedChange = repository::setDualConnect,
                    enabled = snapshot.dualConnectEnabled != null,
                )
                IconButton(onClick = { expanded = !expanded }, enabled = snapshot.dualConnectEnabled != null) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        stringResource(if (expanded) R.string.collapse else R.string.expand),
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                HorizontalDivider()
                if (snapshot.dualConnectDevices.isEmpty()) {
                    Text(
                        stringResource(R.string.dual_connect_no_devices),
                        Modifier.fillMaxWidth().padding(20.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        snapshot.dualConnectDevices.forEach { device ->
                            DualDeviceCard(device) { selectedAddress = device.address }
                        }
                    }
                }
            }
        }
    }

    selectedDevice?.let { device ->
        DualDeviceSheet(
            device = device,
            autoConnectSupported = module.supportsAutoConnect,
            repository = repository,
            onDismiss = { selectedAddress = null },
        )
    }
}

@Composable
private fun DualDeviceCard(device: DualConnectDevice, onClick: () -> Unit) {
    val stateText = when (device.connectionState) {
        DualConnectionState.PLAYING -> R.string.dual_connect_playing
        DualConnectionState.CONNECTED -> R.string.dual_connect_connected
        DualConnectionState.DISCONNECTED -> R.string.dual_connect_disconnected
    }
    val icon = when (device.connectionState) {
        DualConnectionState.PLAYING -> Icons.Default.BluetoothAudio
        DualConnectionState.CONNECTED -> Icons.Default.Bluetooth
        DualConnectionState.DISCONNECTED -> Icons.Default.BluetoothDisabled
    }
    ExpressiveClickableCard(
        onClick = onClick,
        modifier = Modifier
            .width(112.dp)
            .semantics(mergeDescendants = true) { role = Role.Button },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Text(device.name, maxLines = 1, style = MaterialTheme.typography.bodySmall)
            Text(
                stringResource(stateText),
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DualDeviceSheet(
    device: DualConnectDevice,
    autoConnectSupported: Boolean,
    repository: HeadphonesRepository,
    onDismiss: () -> Unit,
) {
    var confirmUnpair by remember(device.address) { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(device.name, style = MaterialTheme.typography.headlineSmall)
            Text(device.address, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (autoConnectSupported) {
                SettingSwitch(
                    stringResource(R.string.dual_connect_auto_connect),
                    device.autoConnect,
                    true,
                ) { repository.setDualDeviceAutoConnect(device, it) }
            }
            SettingSwitch(
                stringResource(R.string.dual_connect_preferred),
                device.preferred,
                true,
            ) { repository.setDualDevicePreferred(device, it) }
            ExpressiveButton(
                onClick = {
                    repository.setDualDeviceConnected(
                        device,
                        device.connectionState == DualConnectionState.DISCONNECTED,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (device.connectionState == DualConnectionState.DISCONNECTED) {
                            R.string.dual_connect_connect
                        } else {
                            R.string.dual_connect_disconnect
                        },
                    ),
                )
            }
            ExpressiveTextButton(onClick = { confirmUnpair = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Delete, null)
                Text(stringResource(R.string.dual_connect_unpair), Modifier.padding(start = 8.dp))
            }
        }
    }
    if (confirmUnpair) {
        AlertDialog(
            onDismissRequest = { confirmUnpair = false },
            title = { Text(stringResource(R.string.dual_connect_unpair)) },
            text = { Text(stringResource(R.string.dual_connect_unpair_confirmation, device.name)) },
            dismissButton = {
                ExpressiveTextButton(onClick = { confirmUnpair = false }) { Text(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                ExpressiveButton(onClick = {
                    repository.unpairDualDevice(device)
                    confirmUnpair = false
                    onDismiss()
                }) { Text(stringResource(R.string.dual_connect_unpair)) }
            },
        )
    }
}

@Composable
private fun BatteryCard(battery: BatteryLevels, rowHeight: androidx.compose.ui.unit.Dp) {
    Card(Modifier.fillMaxWidth(), shape = AppCardShape) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BatteryRow(R.drawable.left_earbud, R.string.left_bud_short, battery.left, battery.chargingLeft, rowHeight)
            BatteryRow(R.drawable.right_earbud, R.string.right_bud_short, battery.right, battery.chargingRight, rowHeight)
            BatteryRow(R.drawable.earbuds_case, R.string.case_label, battery.case, battery.chargingCase, rowHeight)
        }
    }
}

@Composable
private fun BatteryRow(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    level: Int?,
    charging: Boolean,
    rowHeight: androidx.compose.ui.unit.Dp,
) {
    val validLevel = BatteryLevel.validOrNull(level)
    val fraction = BatteryLevel.fraction(validLevel)
    val shape = RoundedCornerShape(20.dp)
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .clip(shape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
        }
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(30.dp),
            )
            Text(
                stringResource(label),
                Modifier.padding(start = 14.dp).weight(1f),
                color = contentColor,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            // Keep the percentage in a stable right-aligned slot. The charging
            // indicator has its own fixed-width slot on the left, so appearing
            // or disappearing never shifts the digits.
            Text(
                text = if (charging && validLevel != null) "⚡" else "",
                modifier = Modifier.width(24.dp),
                color = contentColor,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            Text(
                text = validLevel?.let { "$it%" } ?: "—",
                modifier = Modifier.width(52.dp),
                color = contentColor,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun AncCard(
    snapshot: HeadphonesSnapshot,
    repository: HeadphonesRepository,
    module: AncModule,
    buttonSize: androidx.compose.ui.unit.Dp,
    interactive: Boolean = true,
) {
    Card(Modifier.fillMaxWidth(), shape = AppCardShape) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AncCardHeader(
                snapshot = snapshot,
                module = module,
                interactive = interactive,
                chooseLevel = repository::setAncLevel,
            )
            AncModeSelector(
                selectedMode = snapshot.ancMode,
                buttonSize = buttonSize,
                enabled = interactive,
                onSelect = repository::setAncMode,
            )
        }
    }
}

@Composable
private fun AncModeSelector(
    selectedMode: AncMode?,
    buttonSize: androidx.compose.ui.unit.Dp,
    enabled: Boolean,
    onSelect: (AncMode) -> Unit,
) {
    val modes = remember {
        listOf(
            Triple(AncMode.NOISE_CANCELLING, R.drawable.ic_anc_noise_control, "ancNoise"),
            Triple(AncMode.OFF, R.drawable.ic_anc_off, "ancOff"),
            Triple(AncMode.TRANSPARENCY, R.drawable.ic_anc_transparency, "ancTransparency"),
        )
    }
    val selectedIndex = modes.indexOfFirst { it.first == selectedMode }

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(buttonSize),
    ) {
        val slotWidth = maxWidth / modes.size
        val targetOffset = if (selectedIndex >= 0) {
            slotWidth * selectedIndex + (slotWidth - buttonSize) / 2
        } else {
            0.dp
        }
        val indicatorOffset by animateDpAsState(
            targetValue = targetOffset,
            animationSpec = spring(
                // About 30% faster than the previous spring, with a small
                // controlled overshoot at the destination.
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessLow * 1.69f,
            ),
            label = "ancIndicatorOffset",
        )

        // Every mode keeps a quiet tonal base. One shared accent circle moves
        // between the slots, making the selected state spatial rather than
        // replacing three independent button backgrounds.
        Row(Modifier.fillMaxSize()) {
            modes.forEach {
                Box(
                    Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(buttonSize)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    )
                }
            }
        }

        if (selectedIndex >= 0) {
            Box(
                Modifier
                    .offset(x = indicatorOffset)
                    .size(buttonSize)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }

        Row(Modifier.fillMaxSize()) {
            modes.forEach { (mode, icon, label) ->
                Box(
                    Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    AncModeButton(
                        mode = mode,
                        selectedMode = selectedMode,
                        icon = icon,
                        buttonSize = buttonSize,
                        enabled = enabled,
                        label = label,
                    ) { onSelect(mode) }
                }
            }
        }
    }
}

@Composable
private fun AncModeButton(
    mode: AncMode,
    selectedMode: AncMode?,
    @DrawableRes icon: Int,
    buttonSize: androidx.compose.ui.unit.Dp,
    enabled: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val isSelected = mode == selectedMode
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        label = "${label}Content",
    )
    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = contentColor,
            disabledContentColor = contentColor.copy(alpha = 0.38f),
        ),
        modifier = Modifier.size(buttonSize).semantics { selected = isSelected },
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(mode.label()),
            modifier = Modifier.size(buttonSize * 0.6f),
        )
    }
}

@Composable
private fun AncCardHeader(
    snapshot: HeadphonesSnapshot,
    module: AncModule,
    interactive: Boolean,
    chooseLevel: (AncLevel) -> Unit,
) {
    val canChooseLevel = module.supportsLevels && snapshot.ancMode == AncMode.NOISE_CANCELLING
    val levels = if (module.supportsDynamicLevel) {
        AncLevel.entries
    } else {
        AncLevel.entries.filterNot { it == AncLevel.DYNAMIC }
    }
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = when {
        canChooseLevel && snapshot.ancLevel != null -> stringResource(snapshot.ancLevel.label())
        snapshot.ancMode != null -> stringResource(snapshot.ancMode.label())
        else -> "—"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.anc_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Box {
            val anchorColor by animateColorAsState(
                targetValue = if (expanded) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                label = "ancLevelAnchorColor",
            )
            Row(
                modifier = Modifier
                    .clip(AppCardShape)
                    .background(anchorColor)
                    .then(
                        if (canChooseLevel && interactive) {
                            Modifier.clickable(
                                role = Role.Button,
                                onClick = { expanded = true },
                            )
                        } else Modifier
                    )
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (canChooseLevel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                if (canChooseLevel) {
                    val chevron by animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        label = "levelChevron",
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = chevron },
                        tint = if (interactive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = AppCardShape,
            ) {
                levels.forEach { level ->
                    val isSelected = level == snapshot.ancLevel
                    ExpressiveDropdownMenuItem(
                        selected = isSelected,
                        text = { Text(stringResource(level.label())) },
                        trailingIcon = {
                            if (isSelected) Icon(Icons.Default.Check, null)
                        },
                        onClick = {
                            expanded = false
                            chooseLevel(level)
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeadphonesSettingsScreen(
    state: ConnectionState,
    repository: HeadphonesRepository,
    openFindEarbuds: () -> Unit,
    back: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.headphones_settings),
                        Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        val snapshot = (state as? ConnectionState.Connected)?.snapshot
        if (snapshot == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.disconnected))
            }
            return@Scaffold
        }
        val context = LocalContext.current
        // Wait for the persisted layout before composing the list: rendering the
        // default order first and reordering afterwards makes LazyColumn follow
        // the moved first item, so the screen would open scrolled mid-page.
        val layoutOrNull by remember { ThemeStore.interfaceLayout(context) }
            .collectAsStateWithLifecycle(initialValue = null)
        val layout = layoutOrNull ?: return@Scaffold
        val settings = snapshot.settings
        val protocol = HeadphonesProtocolRegistry.findById(snapshot.protocolId)
        val equalizer = protocol?.module<EqualizerModule>()
        val customEqualizer = protocol?.module<CustomEqualizerModule>()
        val autoPause = protocol?.module<AutoPauseModule>()
        val lowLatency = protocol?.module<LowLatencyModule>()
        val ldac = protocol?.module<LdacModule>()
        val doubleTap = protocol?.module<DoubleTapModule>()
        val tripleTap = protocol?.module<TripleTapModule>()
        val hold = protocol?.module<HoldModule>()
        val splitHold = protocol?.module<SplitHoldModule>()
        val swipe = protocol?.module<SwipeModule>()
        val findEarbuds = protocol?.module<FindEarbudsModule>()
        val anyGestures = doubleTap != null || tripleTap != null || hold != null || splitHold != null || swipe != null

        LazyColumn(
            Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            layout.settingsOrder.filterNot { it in layout.hiddenSettingsBlocks }.forEach { block ->
                when (block) {
                    SettingsBlock.EQUALIZER -> if (equalizer != null) item(key = block.name) {
                        if (customEqualizer != null) {
                            CustomEqualizerSection(snapshot, repository, protocol.id, customEqualizer)
                        } else {
                            EnumDropdown(
                                R.string.equalizer,
                                equalizer.builtInPresets,
                                settings.eqPreset,
                                { it.eqLabel() },
                                repository::setEq,
                            )
                        }
                    }
                    SettingsBlock.AUTO_PAUSE -> if (autoPause != null) item(key = block.name) {
                        CardSwitch(
                            stringResource(R.string.auto_pause),
                            settings.autoPause ?: false,
                            settings.autoPause != null,
                            stringResource(R.string.auto_pause_desc),
                            repository::setAutoPause,
                        )
                    }
                    SettingsBlock.LOW_LATENCY -> if (lowLatency != null) item(key = block.name) {
                        CardSwitch(
                            stringResource(R.string.low_latency),
                            settings.lowLatency ?: false,
                            settings.lowLatency != null,
                            stringResource(R.string.low_latency_desc),
                            repository::setLowLatency,
                        )
                    }
                    SettingsBlock.LDAC -> if (ldac != null) item(key = block.name) {
                        CardSwitch(
                            stringResource(R.string.ldac),
                            settings.ldac ?: false,
                            settings.ldac != null,
                            stringResource(R.string.ldac_desc),
                            repository::setLdac,
                        )
                    }
                    SettingsBlock.FIND_EARBUDS -> if (findEarbuds != null) item(key = block.name) {
                        ExpressiveClickableCard(
                            onClick = openFindEarbuds,
                            modifier = Modifier.fillMaxWidth(),
                            actionDelayMillis = 170,
                        ) {
                            Column(
                                Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    stringResource(R.string.find_earbuds),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    stringResource(R.string.find_earbuds_description),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    SettingsBlock.GESTURES_TITLE -> if (anyGestures) item(key = block.name) {
                        Text(
                            stringResource(R.string.gestures),
                            modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    SettingsBlock.DOUBLE_TAP -> if (doubleTap != null) item(key = block.name) {
                        GesturePair(
                            R.string.double_tap,
                            DoubleTap.entries,
                            settings.doubleTapLeft,
                            settings.doubleTapRight,
                            { it.doubleTapLabel() },
                            { repository.setDoubleTap(left = it) },
                            { repository.setDoubleTap(right = it) },
                        )
                    }
                    SettingsBlock.TRIPLE_TAP -> if (tripleTap != null) item(key = block.name) {
                        GesturePair(
                            R.string.triple_tap,
                            TripleTap.entries,
                            settings.tripleTapLeft,
                            settings.tripleTapRight,
                            { it.tripleTapLabel() },
                            { repository.setTripleTap(left = it) },
                            { repository.setTripleTap(right = it) },
                        )
                    }
                    SettingsBlock.HOLD -> when {
                        splitHold != null -> item(key = block.name) {
                            SplitHoldActionsCard(settings, repository, splitHold)
                        }
                        hold != null -> item(key = block.name) {
                            EnumDropdown(
                                R.string.hold,
                                Hold.entries,
                                settings.hold,
                                { it.holdLabel() },
                                repository::setHold,
                            )
                        }
                    }
                    SettingsBlock.HOLD_MODES -> when {
                        splitHold != null -> item(key = block.name) {
                            SplitHoldModesCard(settings, repository)
                        }
                        hold != null -> item(key = block.name) {
                            HoldModesCard(settings, repository)
                        }
                    }
                    SettingsBlock.SWIPE -> if (swipe != null) item(key = block.name) {
                        EnumDropdown(
                            R.string.swipe,
                            Swipe.entries,
                            settings.swipe,
                            { it.swipeLabel() },
                            repository::setSwipe,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FindEarbudsScreen(
    state: ConnectionState,
    repository: HeadphonesRepository,
    back: () -> Unit,
) {
    val snapshot = (state as? ConnectionState.Connected)?.snapshot
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingSide by remember { mutableStateOf<EarbudSide?>(null) }

    LaunchedEffect(Unit) { repository.refreshFindEarbuds() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) repository.stopFindEarbudsSound()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            repository.stopFindEarbudsSound()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.find_earbuds), Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        if (snapshot == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.disconnected))
            }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.find_earbuds_local_only),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FindEarbudCard(EarbudSide.LEFT, snapshot, repository) { pendingSide = it }
            FindEarbudCard(EarbudSide.RIGHT, snapshot, repository) { pendingSide = it }
        }
    }

    pendingSide?.let { side ->
        AlertDialog(
            onDismissRequest = { pendingSide = null },
            title = { Text(stringResource(R.string.find_earbuds_warning_title)) },
            text = { Text(stringResource(R.string.find_earbuds_warning)) },
            confirmButton = {
                ExpressiveTextButton(onClick = {
                    repository.setFindEarbudSound(side, true)
                    pendingSide = null
                }) { Text(stringResource(R.string.find_earbuds_confirm)) }
            },
            dismissButton = {
                ExpressiveTextButton(onClick = { pendingSide = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun FindEarbudCard(
    side: EarbudSide,
    snapshot: HeadphonesSnapshot,
    repository: HeadphonesRepository,
    requestStart: (EarbudSide) -> Unit,
) {
    val state = snapshot.findEarbuds
    val sounding = state.sounding(side)
    val title = stringResource(
        if (side == EarbudSide.LEFT) R.string.find_earbuds_left else R.string.find_earbuds_right,
    )
    Card(Modifier.fillMaxWidth(), shape = AppCardShape) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            when {
                sounding -> ExpressiveButton(onClick = { repository.setFindEarbudSound(side, false) }) {
                    Text(stringResource(R.string.find_earbuds_stop))
                }
                else -> ExpressiveButton(onClick = { requestStart(side) }) {
                    Text(stringResource(R.string.find_earbuds_play))
                }
            }
        }
    }
}

private sealed interface EqualizerChoice {
    data class BuiltIn(val preset: EqPreset) : EqualizerChoice
    data class Custom(val preset: CustomEqualizerPreset) : EqualizerChoice
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomEqualizerSection(
    snapshot: HeadphonesSnapshot,
    repository: HeadphonesRepository,
    protocolId: String,
    module: CustomEqualizerModule,
) {
    val diagnostics = snapshot.equalizerDiagnostics
    val customPresets = diagnostics?.customPresets.orEmpty()
    val builtIns = module.builtInPresets.map { EqualizerChoice.BuiltIn(it) }
    val customs = customPresets.map { EqualizerChoice.Custom(it) }
    val choices = builtIns + customs
    val activeId = diagnostics?.activePresetId
    val selected = choices.firstOrNull {
        when (it) {
            is EqualizerChoice.BuiltIn -> it.preset.code == activeId
            is EqualizerChoice.Custom -> it.preset.id == activeId
        }
    }
    var expanded by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CustomEqualizerPreset?>(null) }
    val canCreate = customPresets.size < (module.lastPresetId - module.firstPresetId + 1)
    val defaultCustomName = stringResource(R.string.eq_custom_default_name)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportDone = stringResource(R.string.eq_export_done)
    val exportFailed = stringResource(R.string.eq_export_failed)
    val importFailed = stringResource(R.string.eq_import_failed)
    var pendingImport by remember { mutableStateOf<EqualizerPresetFile?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) scope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use {
                        it.write(EqualizerPresetFileCodec.encode(protocolId, customPresets))
                    }
                }.isSuccess
            }
            Toast.makeText(context, if (success) exportDone else exportFailed, Toast.LENGTH_LONG).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) scope.launch {
            val decoded = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)!!.use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            require(output.size() + read <= EqualizerPresetFileCodec.MAX_FILE_BYTES) {
                                "File is too large"
                            }
                            output.write(buffer, 0, read)
                        }
                        output.toByteArray()
                    }
                    EqualizerPresetFileCodec.decode(bytes.toString(Charsets.UTF_8), protocolId, module)
                }
            }
            decoded.onSuccess { pendingImport = it }
                .onFailure { Toast.makeText(context, importFailed, Toast.LENGTH_LONG).show() }
        }
    }
    val currentLabel = when (selected) {
        is EqualizerChoice.BuiltIn -> selected.preset.eqLabel()
        is EqualizerChoice.Custom -> selected.preset.name
        null -> activeId?.toString().orEmpty()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = currentLabel,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text(stringResource(R.string.equalizer)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                shape = AppCardShape,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = AppCardShape,
            ) {
                choices.forEach { choice ->
                    val label = when (choice) {
                        is EqualizerChoice.BuiltIn -> choice.preset.eqLabel()
                        is EqualizerChoice.Custom -> choice.preset.name
                    }
                    ExpressiveDropdownMenuItem(
                        selected = choice == selected,
                        text = { Text(label) },
                        trailingIcon = {
                            if (choice == selected) Icon(Icons.Default.Check, null)
                        },
                        onClick = {
                            expanded = false
                            when (choice) {
                                is EqualizerChoice.BuiltIn -> repository.setEq(choice.preset)
                                is EqualizerChoice.Custom -> repository.saveCustomEq(choice.preset)
                            }
                        },
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExpressiveTextButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/json", "text/plain", "application/octet-stream")) },
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.eq_import))
            }
            ExpressiveTextButton(
                onClick = { exportLauncher.launch(EqualizerPresetFileCodec.suggestedFileName(protocolId)) },
                enabled = customPresets.isNotEmpty(),
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.eq_export))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            val activeCustom = (selected as? EqualizerChoice.Custom)?.preset
            if (activeCustom != null) {
                ExpressiveTextButton(onClick = { editing = activeCustom }) {
                    Icon(Icons.Default.Edit, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.eq_custom_edit))
                }
            }
            if (canCreate) {
                ExpressiveTextButton(
                    onClick = {
                        val used = customPresets.mapTo(mutableSetOf()) { it.id }
                        val id = (module.firstPresetId..module.lastPresetId)
                            .first { it !in used }
                        editing = CustomEqualizerPreset(
                            id = id,
                            name = "$defaultCustomName ${id - module.firstPresetId + 1}",
                            bands = List(module.bandCount) { 0 },
                        )
                    },
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.eq_custom_create))
                }
            }
        }
    }

    pendingImport?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.eq_import_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.eq_import_confirmation, file.presets.size))
                    file.presets.forEach { preset ->
                        Text("${preset.id}: ${preset.name}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                ExpressiveTextButton(onClick = {
                    repository.replaceCustomEq(file.presets)
                    pendingImport = null
                }) { Text(stringResource(R.string.eq_import_replace)) }
            },
            dismissButton = {
                ExpressiveTextButton(onClick = { pendingImport = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    editing?.let { preset ->
        CustomEqualizerEditor(
            original = preset,
            module = module,
            allowDelete = customPresets.any { it.id == preset.id },
            onDismiss = { editing = null },
            onSave = {
                repository.saveCustomEq(it)
                editing = null
            },
            onDelete = {
                repository.deleteCustomEq(preset)
                editing = null
            },
        )
    }
}

@Composable
private fun CustomEqualizerEditor(
    original: CustomEqualizerPreset,
    module: CustomEqualizerModule,
    allowDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (CustomEqualizerPreset) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(original.id) { mutableStateOf(original.name) }
    var bands by remember(original.id) { mutableStateOf(original.bands) }
    var confirmDelete by remember { mutableStateOf(false) }
    val nameBytes = name.trim().toByteArray(Charsets.UTF_8).size
    val validName = name.isNotBlank() && nameBytes <= module.maxNameBytes
    val frequencies = listOf("60", "125", "250", "500", "1K", "2K", "4K", "8K", "12K", "16K")
    val frequenciesHz = listOf(60, 125, 250, 500, 1_000, 2_000, 4_000, 8_000, 12_000, 16_000)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.eq_custom_editor_title)) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.eq_custom_name)) },
                    supportingText = {
                        Text(stringResource(R.string.eq_custom_name_bytes, nameBytes, module.maxNameBytes))
                    },
                    isError = !validName,
                    singleLine = true,
                    shape = AppCardShape,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    ExpressiveTextButton(onClick = { bands = List(10) { 0 } }) {
                        Text(stringResource(R.string.eq_custom_reset))
                    }
                }
                frequencies.forEachIndexed { index, frequency ->
                    val raw = bands.getOrElse(index) { 0 }
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.eq_custom_frequency, frequency))
                            Text(stringResource(R.string.eq_custom_db_value, raw / 10f))
                        }
                        val bandDescription = stringResource(R.string.eq_band_accessibility, frequenciesHz[index])
                        val bandState = stringResource(R.string.eq_custom_db_value, raw / 10f)
                        Slider(
                            value = raw.toFloat(),
                            modifier = Modifier.semantics {
                                contentDescription = bandDescription
                                stateDescription = bandState
                            },
                            onValueChange = { value ->
                                val snapped = ((value / module.step).roundToInt() * module.step)
                                    .coerceIn(module.minimum, module.maximum)
                                bands = bands.toMutableList().also { it[index] = snapped }
                            },
                            valueRange = module.minimum.toFloat()..module.maximum.toFloat(),
                            steps = 23,
                        )
                    }
                }
            }
        },
        confirmButton = {
            ExpressiveTextButton(
                onClick = { onSave(original.copy(name = name.trim(), bands = bands)) },
                enabled = validName,
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row {
                if (allowDelete) {
                    ExpressiveTextButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.eq_custom_delete))
                    }
                }
                ExpressiveTextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.eq_custom_delete_title)) },
            text = { Text(stringResource(R.string.eq_custom_delete_confirmation, original.name)) },
            confirmButton = { ExpressiveTextButton(onDelete) { Text(stringResource(R.string.eq_custom_delete)) } },
            dismissButton = { ExpressiveTextButton({ confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun <T> GesturePair(@StringRes title: Int, values: List<T>, left: T?, right: T?, label: @Composable (T) -> String, setLeft: (T) -> Unit, setRight: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GestureColumn(R.string.left_bud_short, values, left, label, setLeft, Modifier.weight(1f))
            GestureColumn(R.string.right_bud_short, values, right, label, setRight, Modifier.weight(1f))
        }
    }
}

@Composable
private fun <T> GestureColumn(@StringRes title: Int, values: List<T>, selected: T?, label: @Composable (T) -> String, choose: (T) -> Unit, modifier: Modifier) {
    Card(modifier, shape = AppCardShape) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(title), fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            values.forEach { value ->
                val isSelected = selected == value
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { choose(value) },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label(value), Modifier.weight(1f), maxLines = 2)
                    RadioButton(selected = isSelected, onClick = null)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(@StringRes title: Int, values: List<T>, selected: T?, label: @Composable (T) -> String, choose: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val current = selected?.let { label(it) }.orEmpty()
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = current,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(title)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = AppCardShape,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = AppCardShape,
            modifier = Modifier.fillMaxWidth(),
        ) {
            values.forEach { value ->
                val isSelected = value == selected
                ExpressiveDropdownMenuItem(
                    selected = isSelected,
                    text = { Text(label(value)) },
                    trailingIcon = {
                        if (isSelected) Icon(Icons.Default.Check, null)
                    },
                    onClick = {
                        expanded = false
                        choose(value)
                    },
                )
            }
        }
    }
}

@Composable
private fun SplitHoldActionsCard(
    settings: FreeBudsSettings,
    repository: HeadphonesRepository,
    module: SplitHoldModule,
) {
    Card(Modifier.fillMaxWidth(), shape = AppCardShape) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.hold),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            EnumDropdown(
                R.string.find_earbuds_left,
                module.availableActions,
                settings.splitHoldLeft,
                { it.splitHoldLabel() },
                { repository.setSplitHoldAction(EarbudSide.LEFT, it) },
            )
            EnumDropdown(
                R.string.find_earbuds_right,
                module.availableActions,
                settings.splitHoldRight,
                { it.splitHoldLabel() },
                { repository.setSplitHoldAction(EarbudSide.RIGHT, it) },
            )
        }
    }
}

@Composable
private fun SplitHoldModesCard(settings: FreeBudsSettings, repository: HeadphonesRepository) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SplitHoldSideModesCard(
            R.string.left_bud_short,
            EarbudSide.LEFT,
            settings.splitHoldLeft,
            settings.splitHoldModesLeft,
            repository,
        )
        SplitHoldSideModesCard(
            R.string.right_bud_short,
            EarbudSide.RIGHT,
            settings.splitHoldRight,
            settings.splitHoldModesRight,
            repository,
        )
    }
}

@Composable
private fun SplitHoldSideModesCard(
    @StringRes sideLabel: Int,
    side: EarbudSide,
    action: SplitHoldAction?,
    modes: Set<AncMode>?,
    repository: HeadphonesRepository,
) {
    val current = modes ?: emptySet()
    val enabled = action == SplitHoldAction.CYCLE_ANC && modes != null
    Card(Modifier.fillMaxWidth(), shape = AppCardShape) {
        Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${stringResource(R.string.hold_modes)} · ${stringResource(sideLabel)}",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(
                    if (enabled) R.string.hold_modes_minimum_hint else R.string.hold_modes_disabled_hint,
                ),
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            AncMode.entries.forEach { mode ->
                SettingSwitch(
                    stringResource(mode.label()),
                    mode in current,
                    enabled,
                    stringResource(mode.description()),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                ) { checked ->
                    repository.setSplitHoldModes(side, toggleHoldMode(current, mode, checked))
                }
            }
        }
    }
}

@Composable
private fun HoldModesCard(settings: FreeBudsSettings, repository: HeadphonesRepository) {
    val current = settings.holdModes ?: emptySet()
    val enabled = settings.hold == Hold.CYCLE_ANC && settings.holdModes != null
    Card(Modifier.fillMaxWidth(), shape = AppCardShape) {
        Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.hold_modes),
                modifier = Modifier.padding(horizontal = 16.dp).semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(
                    if (enabled) R.string.hold_modes_minimum_hint
                    else R.string.hold_modes_disabled_hint,
                ),
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Column {
                AncMode.entries.forEach { mode ->
                    SettingSwitch(
                        stringResource(mode.label()),
                        mode in current,
                        enabled,
                        stringResource(mode.description()),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    ) { checked ->
                        repository.setHoldModes(toggleHoldMode(current, mode, checked))
                    }
                }
            }
        }
    }
}

@Composable
private fun CardSwitch(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    description: String? = null,
    onChange: (Boolean) -> Unit,
) {
    Card(Modifier.fillMaxWidth(), shape = AppCardShape) {
        // Padding lives inside the toggleable row so the ripple fills the card.
        SettingSwitch(
            label,
            checked,
            enabled,
            description,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            onChange = onChange,
        )
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    description: String? = null,
    contentPadding: PaddingValues = PaddingValues(vertical = 6.dp),
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onChange,
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked, onCheckedChange = null, enabled = enabled)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InterfaceEditorScreen(
    identity: HeadphonesIdentity?,
    snapshot: HeadphonesSnapshot?,
    repository: HeadphonesRepository,
    back: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lastIdentity by remember { ThemeStore.lastHeadphonesIdentity(context) }
        .collectAsStateWithLifecycle(initialValue = HeadphonesIdentity("freebuds_5i"))
    val effectiveIdentity = identity ?: lastIdentity
    val protocol = HeadphonesProtocolRegistry.findById(effectiveIdentity.protocolId)
        ?: HeadphonesProtocolRegistry.defaultProfile()
    val layout by remember { ThemeStore.interfaceLayout(context) }
        .collectAsStateWithLifecycle(initialValue = InterfaceLayout())
    val forcePreviewColor by remember(effectiveIdentity) {
        ThemeStore.forceHeadphonesColor(context, effectiveIdentity)
    }.collectAsStateWithLifecycle(initialValue = false)
    val storedPreviewColor by remember(effectiveIdentity) {
        ThemeStore.headphonesColor(context, effectiveIdentity)
    }.collectAsStateWithLifecycle(initialValue = DeviceColor.BLACK.name)
    val previewColor = storedPreviewColor.toDeviceColor()
    val connectedProtocol = snapshot?.let { HeadphonesProtocolRegistry.findById(it.protocolId) }
    val supportedHome = connectedProtocol?.let(::supportedHomeBlocks) ?: HomeBlock.entries
    val supportedSettings = connectedProtocol?.let(::supportedSettingsBlocks) ?: SettingsBlock.entries
    val homeOrder = layout.homeOrder
    val settingsOrder = layout.settingsOrder
    val previewSnapshot = snapshot?.takeIf { it.protocolId == protocol.id } ?: HeadphonesSnapshot(
        protocolId = protocol.id,
        name = protocol.displayName,
        battery = BatteryLevels(left = 60, right = 85, case = 100),
        ancMode = AncMode.NOISE_CANCELLING,
        ancLevel = AncLevel.ULTRA,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.interface_editor),
                        Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.interface_editor_common_layout),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            item {
                EditorSectionTitle(R.string.interface_editor_home_order)
            }
            items(homeOrder, key = { "home_${it.name}" }) { block ->
                val hasDetails = block == HomeBlock.HEADPHONES_IMAGE ||
                    block == HomeBlock.BATTERY ||
                    block == HomeBlock.ANC
                val supported = block in supportedHome
                val visible = block !in layout.hiddenHomeBlocks
                OrderEditorRow(
                    modifier = Modifier.animateItem(),
                    label = stringResource(block.label()),
                    visible = visible,
                    canChangeVisibility = block !in REQUIRED_HOME_BLOCKS,
                    onVisibilityChange = { show ->
                        scope.launch { ThemeStore.setHomeBlockVisible(context, block, show) }
                    },
                    supported = supported,
                    unavailableText = if (supported) null else stringResource(
                        R.string.interface_editor_unavailable_for_model,
                        connectedProtocol?.displayName.orEmpty(),
                    ),
                    canMoveUp = homeOrder.indexOf(block) > 0,
                    canMoveDown = homeOrder.indexOf(block) < homeOrder.lastIndex,
                    onMoveUp = {
                        scope.launch {
                            ThemeStore.setHomeOrder(
                                context,
                                layout.homeOrder.move(block, -1),
                            )
                        }
                    },
                    onMoveDown = {
                        scope.launch {
                            ThemeStore.setHomeOrder(
                                context,
                                layout.homeOrder.move(block, 1),
                            )
                        }
                    },
                    hasDetails = hasDetails && supported && visible,
                ) {
                    when (block) {
                        HomeBlock.HEADPHONES_IMAGE -> {
                            LayoutSlider(
                                label = stringResource(R.string.interface_editor_image_scale),
                                value = layout.imageScalePercent,
                                defaultValue = InterfaceLayoutDefaults.IMAGE_SCALE_PERCENT,
                                range = 70..140,
                                step = 5,
                                suffix = "%",
                                onValue = { image ->
                                    scope.launch {
                                        ThemeStore.setInterfaceScales(
                                            context,
                                            image,
                                            layout.batteryScalePercent,
                                            layout.ancScalePercent,
                                        )
                                    }
                                },
                            )
                            Spacer(Modifier.height(12.dp))
                            val scale = layout.imageScalePercent / 100f
                            Image(
                                painter = painterResource(
                                    headphonesImage(
                                        previewSnapshot,
                                        forceColor = forcePreviewColor,
                                        selectedColor = previewColor,
                                    ),
                                ),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(
                                    animateDpAsState((180 * scale).dp, label = "imgPreview").value,
                                ),
                                contentScale = ContentScale.Fit,
                            )
                        }

                        HomeBlock.BATTERY -> {
                            LayoutSlider(
                                label = stringResource(R.string.interface_editor_battery_height),
                                value = layout.batteryScalePercent,
                                defaultValue = InterfaceLayoutDefaults.BATTERY_SCALE_PERCENT,
                                range = 80..140,
                                step = 5,
                                suffix = "%",
                                onValue = { scale ->
                                    scope.launch {
                                        ThemeStore.setInterfaceScales(
                                            context,
                                            layout.imageScalePercent,
                                            scale,
                                            layout.ancScalePercent,
                                        )
                                    }
                                },
                            )
                            Spacer(Modifier.height(12.dp))
                            BatteryCard(
                                previewSnapshot.battery,
                                animateDpAsState(
                                    scaleDp(
                                        InterfaceLayoutDefaults.BATTERY_ROW_HEIGHT_DP,
                                        layout.batteryScalePercent,
                                    ).dp,
                                    label = "batPreview",
                                ).value,
                            )
                        }

                        HomeBlock.ANC -> {
                            val ancModule = protocol.module<AncModule>()
                            if (ancModule != null) {
                                LayoutSlider(
                                    label = stringResource(R.string.interface_editor_anc_size),
                                    value = layout.ancScalePercent,
                                    defaultValue = InterfaceLayoutDefaults.ANC_SCALE_PERCENT,
                                    range = 80..125,
                                    step = 5,
                                    suffix = "%",
                                    onValue = { size ->
                                        scope.launch {
                                            ThemeStore.setInterfaceScales(
                                                context,
                                                layout.imageScalePercent,
                                                layout.batteryScalePercent,
                                                size,
                                            )
                                        }
                                    },
                                )
                                Spacer(Modifier.height(12.dp))
                                AncCard(
                                    previewSnapshot,
                                    repository,
                                    ancModule,
                                    animateDpAsState(
                                        scaleDp(
                                            InterfaceLayoutDefaults.ANC_BUTTON_SIZE_DP,
                                            layout.ancScalePercent,
                                        ).dp,
                                        label = "ancPreview",
                                    ).value,
                                    interactive = false,
                                )
                            }
                        }

                        else -> Unit
                    }
                }
            }
            item {
                EditorSectionTitle(R.string.interface_editor_settings_order)
            }
            items(settingsOrder, key = { "settings_${it.name}" }) { block ->
                val supported = block in supportedSettings
                val visible = block !in layout.hiddenSettingsBlocks
                OrderEditorRow(
                    modifier = Modifier.animateItem(),
                    label = stringResource(block.label()),
                    visible = visible,
                    canChangeVisibility = true,
                    onVisibilityChange = { show ->
                        scope.launch { ThemeStore.setSettingsBlockVisible(context, block, show) }
                    },
                    supported = supported,
                    unavailableText = if (supported) null else stringResource(
                        R.string.interface_editor_unavailable_for_model,
                        connectedProtocol?.displayName.orEmpty(),
                    ),
                    canMoveUp = settingsOrder.indexOf(block) > 0,
                    canMoveDown = settingsOrder.indexOf(block) < settingsOrder.lastIndex,
                    onMoveUp = {
                        scope.launch {
                            ThemeStore.setSettingsOrder(
                                context,
                                layout.settingsOrder.move(block, -1),
                            )
                        }
                    },
                    onMoveDown = {
                        scope.launch {
                            ThemeStore.setSettingsOrder(
                                context,
                                layout.settingsOrder.move(block, 1),
                            )
                        }
                    },
                )
            }
            item {
                ExpressiveButton(
                    onClick = {
                        scope.launch { ThemeStore.resetInterfaceLayout(context) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.interface_editor_reset))
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

}

private fun supportedHomeBlocks(
    protocol: io.github.razziell.freebuddynext.protocol.HeadphonesProtocol,
): List<HomeBlock> = HomeBlock.entries.filter { block ->
    when (block) {
        HomeBlock.ANC -> protocol.module<AncModule>() != null
        HomeBlock.DUAL_CONNECT -> protocol.module<DualConnectModule>() != null
        else -> true
    }
}

private fun supportedSettingsBlocks(
    protocol: io.github.razziell.freebuddynext.protocol.HeadphonesProtocol,
): List<SettingsBlock> {
    val gestures = protocol.module<DoubleTapModule>() != null ||
        protocol.module<TripleTapModule>() != null ||
        protocol.module<HoldModule>() != null ||
        protocol.module<SwipeModule>() != null
    return SettingsBlock.entries.filter { block ->
        when (block) {
            SettingsBlock.EQUALIZER -> protocol.module<EqualizerModule>() != null
            SettingsBlock.AUTO_PAUSE -> protocol.module<AutoPauseModule>() != null
            SettingsBlock.LOW_LATENCY -> protocol.module<LowLatencyModule>() != null
            SettingsBlock.LDAC -> protocol.module<LdacModule>() != null
            SettingsBlock.FIND_EARBUDS -> protocol.module<FindEarbudsModule>() != null
            SettingsBlock.GESTURES_TITLE -> gestures
            SettingsBlock.DOUBLE_TAP -> protocol.module<DoubleTapModule>() != null
            SettingsBlock.TRIPLE_TAP -> protocol.module<TripleTapModule>() != null
            SettingsBlock.HOLD, SettingsBlock.HOLD_MODES ->
                protocol.module<HoldModule>() != null || protocol.module<SplitHoldModule>() != null
            SettingsBlock.SWIPE -> protocol.module<SwipeModule>() != null
        }
    }
}

private fun <T> List<T>.move(value: T, offset: Int): List<T> {
    val from = indexOf(value)
    if (from < 0) return this
    val to = (from + offset).coerceIn(indices)
    if (from == to) return this
    return toMutableList().apply {
        this[from] = this[to]
        this[to] = value
    }
}

/** Clickable card with a guaranteed visible press pulse before navigation or opening a layer. */
@Composable
private fun ExpressiveClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    actionDelayMillis: Long = 120,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf(false) }

    LaunchedEffect(interaction) {
        interaction.interactions.collect { event ->
            if (event is PressInteraction.Press) {
                scale.animateTo(0.97f, tween(75))
                scale.animateTo(
                    1f,
                    spring(
                        dampingRatio = 0.86f,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
            }
        }
    }

    Card(
        onClick = {
            if (!pending) {
                pending = true
                scope.launch {
                    try {
                        if (actionDelayMillis > 0) delay(actionDelayMillis)
                        onClick()
                    } finally {
                        pending = false
                    }
                }
            }
        },
        modifier = modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        enabled = enabled,
        shape = AppCardShape,
        interactionSource = interaction,
        content = content,
    )
}

/** Compatibility wrappers; all text actions use the shared outlined style. */
@Composable
private fun ExpressiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    actionDelayMillis: Long = 120,
    content: @Composable RowScope.() -> Unit,
) = AppActionButton(onClick, modifier, enabled, actionDelayMillis, content = content)

@Composable
private fun ExpressiveTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    actionDelayMillis: Long = 120,
    content: @Composable RowScope.() -> Unit,
) = AppActionButton(onClick, modifier, enabled, actionDelayMillis, content = content)

/** M3 Expressive menu row with a guaranteed visible press pulse before dismissal. */
@Composable
private fun ExpressiveDropdownMenuItem(
    selected: Boolean,
    text: @Composable () -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf(false) }

    LaunchedEffect(interaction) {
        interaction.interactions.collect { event ->
            if (event is PressInteraction.Press) {
                scale.animateTo(0.97f, tween(75))
                scale.animateTo(
                    1f,
                    spring(
                        dampingRatio = 0.86f,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
            }
        }
    }

    DropdownMenuItem(
        text = text,
        trailingIcon = trailingIcon,
        interactionSource = interaction,
        modifier = Modifier
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .heightIn(min = 52.dp)
            .clip(MenuItemShape)
            .then(
                if (selected) {
                    Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                } else {
                    Modifier
                },
            )
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        contentPadding = PaddingValues(horizontal = 14.dp),
        onClick = {
            if (!pending) {
                pending = true
                scope.launch {
                    try {
                        kotlinx.coroutines.delay(120)
                        onClick()
                    } finally {
                        pending = false
                    }
                }
            }
        },
    )
}

@Composable
private fun EditorSectionTitle(@StringRes title: Int) {
    Text(
        stringResource(title),
        modifier = Modifier.fillMaxWidth().semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
    )
}

@Composable
private fun OrderEditorRow(
    label: String,
    visible: Boolean,
    canChangeVisibility: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    supported: Boolean = true,
    unavailableText: String? = null,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    hasDetails: Boolean = false,
    modifier: Modifier = Modifier,
    details: @Composable ColumnScope.() -> Unit = {},
) {
    // Expressive: dim smoothly when the block is hidden or unsupported.
    val cardAlpha by animateFloatAsState(
        targetValue = if (visible && supported) 1f else 0.55f,
        label = "editorRowAlpha",
    )
    Card(modifier.fillMaxWidth().alpha(cardAlpha), shape = AppCardShape) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (supported && visible) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        },
                    )
                    if (unavailableText != null) {
                        Text(
                            unavailableText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        )
                    }
                }
                if (canChangeVisibility) {
                    IconButton(onClick = { onVisibilityChange(!visible) }) {
                        Icon(
                            if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            stringResource(
                                if (visible) R.string.interface_editor_hide else R.string.interface_editor_show,
                            ),
                        )
                    }
                }
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(Icons.Default.ArrowUpward, stringResource(R.string.interface_editor_move_up))
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(Icons.Default.ArrowDownward, stringResource(R.string.interface_editor_move_down))
                }
            }
            if (hasDetails) {
                Column(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    content = details,
                )
            }
        }
    }
}

@Composable
private fun LayoutSlider(
    label: String,
    value: Int,
    defaultValue: Int,
    range: IntRange,
    step: Int,
    suffix: String,
    onValue: (Int) -> Unit,
) {
    // Local value during drag; persisted once on release to avoid a DataStore
    // write (and preference-file rewrite) for every crossed step.
    var dragValue by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(value) { dragValue = null }
    val shownValue = dragValue ?: value
    val isDefault = shownValue == defaultValue
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(
                "$shownValue$suffix",
                color = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = shownValue.toFloat(),
            onValueChange = { raw ->
                dragValue = ((raw.roundToInt() + step / 2) / step * step)
                    .coerceIn(range.first, range.last)
            },
            onValueChangeFinished = {
                dragValue?.takeIf { it != value }?.let(onValue)
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = ((range.last - range.first) / step - 1).coerceAtLeast(0),
            modifier = Modifier.semantics {
                contentDescription = label
                stateDescription = "$shownValue$suffix"
            },
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutScreen(
    activeIdentity: HeadphonesIdentity?,
    repository: HeadphonesRepository,
    openInterfaceEditor: () -> Unit,
    back: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val themeMode by remember { ThemeStore.themeMode(context) }
        .collectAsStateWithLifecycle(initialValue = ThemeStore.ThemeMode.SYSTEM)
    // Reuse the value already collected at the application UI root. Starting a second
    // DataStore collection here with `true` caused the switch to flash on every entry.
    val showEmojis = LocalShowEmojis.current
    val lowBatterySettings by remember { ThemeStore.lowBatteryAlertSettings(context) }
        .collectAsStateWithLifecycle(initialValue = LowBatteryAlertSettings())
    var notificationsAllowed by remember {
        mutableStateOf(LowBatteryNotifier.hasNotificationPermission(context))
    }
    var pendingTestNotification by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsAllowed = LowBatteryNotifier.hasNotificationPermission(context)
                if (pendingTestNotification && notificationsAllowed) {
                    pendingTestNotification = false
                    repository.sendTestLowBatteryNotification(lowBatterySettings)
                } else {
                    repository.recheckLowBatteryAlert()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        notificationsAllowed = LowBatteryNotifier.hasNotificationPermission(context)
        if (pendingTestNotification && notificationsAllowed) {
            pendingTestNotification = false
            repository.sendTestLowBatteryNotification(lowBatterySettings)
        } else {
            pendingTestNotification = false
            repository.recheckLowBatteryAlert()
        }
    }
    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            runCatching { context.startActivity(intent) }
        }
    }
    val lastIdentity by remember { ThemeStore.lastHeadphonesIdentity(context) }
        .collectAsStateWithLifecycle(initialValue = HeadphonesIdentity("freebuds_5i"))
    val colorIdentity = activeIdentity ?: lastIdentity
    val colorProtocol = HeadphonesProtocolRegistry.findById(colorIdentity.protocolId)
        ?: HeadphonesProtocolRegistry.defaultProfile()
    val forceHeadphonesColor by remember(colorIdentity) { ThemeStore.forceHeadphonesColor(context, colorIdentity) }
        .collectAsStateWithLifecycle(initialValue = false)
    val storedHeadphonesColor by remember(colorIdentity) { ThemeStore.headphonesColor(context, colorIdentity) }
        .collectAsStateWithLifecycle(initialValue = DeviceColor.BLACK.name)
    val selectedHeadphonesColor = storedHeadphonesColor.toDeviceColor()
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about), Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.companion_section_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            ExpressiveClickableCard(
                onClick = {
                    context.startActivity(android.content.Intent(context,
                        io.github.razziell.freebuddynext.companion.CompanionSetupActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth(),
                actionDelayMillis = 170,
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(stringResource(R.string.companion_title), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.companion_summary), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Card(Modifier.fillMaxWidth(), shape = AppCardShape) {
                Column {
                    SettingSwitch(
                        label = stringResource(R.string.low_battery_alert_title),
                        checked = lowBatterySettings.enabled,
                        description = stringResource(R.string.low_battery_alert_description),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    ) { enabled ->
                        if (enabled && !notificationsAllowed && Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        scope.launch {
                            ThemeStore.setLowBatteryAlertSettings(
                                context,
                                lowBatterySettings.copy(enabled = enabled),
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = lowBatterySettings.enabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column(
                            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (!notificationsAllowed) {
                                Text(
                                    stringResource(R.string.low_battery_permission_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                AppActionButton(
                                    onClick = ::requestNotifications,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(stringResource(R.string.low_battery_permission_action))
                                }
                            }
                            HorizontalDivider()
                            SettingSwitch(
                                label = stringResource(R.string.low_battery_left_toggle),
                                checked = lowBatterySettings.leftEnabled,
                            ) { enabled ->
                                scope.launch {
                                    ThemeStore.setLowBatteryAlertSettings(
                                        context,
                                        lowBatterySettings.copy(leftEnabled = enabled),
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = lowBatterySettings.leftEnabled,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut(),
                            ) {
                                LayoutSlider(
                                    label = stringResource(R.string.low_battery_left_threshold),
                                    value = lowBatterySettings.leftThresholdPercent,
                                    defaultValue = LowBatteryAlertSettings.DEFAULT_THRESHOLD_PERCENT,
                                    range = LowBatteryAlertSettings.MIN_THRESHOLD_PERCENT..LowBatteryAlertSettings.MAX_THRESHOLD_PERCENT,
                                    step = LowBatteryAlertSettings.STEP_PERCENT,
                                    suffix = "%",
                                    onValue = { percent ->
                                        scope.launch {
                                            ThemeStore.setLowBatteryAlertSettings(
                                                context,
                                                lowBatterySettings.copy(leftThresholdPercent = percent),
                                            )
                                        }
                                    },
                                )
                            }
                            HorizontalDivider()
                            SettingSwitch(
                                label = stringResource(R.string.low_battery_right_toggle),
                                checked = lowBatterySettings.rightEnabled,
                            ) { enabled ->
                                scope.launch {
                                    ThemeStore.setLowBatteryAlertSettings(
                                        context,
                                        lowBatterySettings.copy(rightEnabled = enabled),
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = lowBatterySettings.rightEnabled,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut(),
                            ) {
                                LayoutSlider(
                                    label = stringResource(R.string.low_battery_right_threshold),
                                    value = lowBatterySettings.rightThresholdPercent,
                                    defaultValue = LowBatteryAlertSettings.DEFAULT_THRESHOLD_PERCENT,
                                    range = LowBatteryAlertSettings.MIN_THRESHOLD_PERCENT..LowBatteryAlertSettings.MAX_THRESHOLD_PERCENT,
                                    step = LowBatteryAlertSettings.STEP_PERCENT,
                                    suffix = "%",
                                    onValue = { percent ->
                                        scope.launch {
                                            ThemeStore.setLowBatteryAlertSettings(
                                                context,
                                                lowBatterySettings.copy(rightThresholdPercent = percent),
                                            )
                                        }
                                    },
                                )
                            }
                            HorizontalDivider()
                            SettingSwitch(
                                label = stringResource(R.string.low_battery_case_toggle),
                                checked = lowBatterySettings.caseEnabled,
                            ) { enabled ->
                                scope.launch {
                                    ThemeStore.setLowBatteryAlertSettings(
                                        context,
                                        lowBatterySettings.copy(caseEnabled = enabled),
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = lowBatterySettings.caseEnabled,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut(),
                            ) {
                                LayoutSlider(
                                    label = stringResource(R.string.low_battery_case_threshold),
                                    value = lowBatterySettings.caseThresholdPercent,
                                    defaultValue = LowBatteryAlertSettings.DEFAULT_THRESHOLD_PERCENT,
                                    range = LowBatteryAlertSettings.MIN_THRESHOLD_PERCENT..LowBatteryAlertSettings.MAX_THRESHOLD_PERCENT,
                                    step = LowBatteryAlertSettings.STEP_PERCENT,
                                    suffix = "%",
                                    onValue = { percent ->
                                        scope.launch {
                                            ThemeStore.setLowBatteryAlertSettings(
                                                context,
                                                lowBatterySettings.copy(caseThresholdPercent = percent),
                                            )
                                        }
                                    },
                                )
                            }
                            HorizontalDivider()
                            AppActionButton(
                                onClick = {
                                    if (!repository.sendTestLowBatteryNotification(lowBatterySettings)) {
                                        pendingTestNotification = true
                                        requestNotifications()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.low_battery_test_action))
                            }
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.appearance),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            ExpressiveClickableCard(
                onClick = { showThemeDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(stringResource(R.string.theme), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(themeMode.label()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            CardSwitch(
                label = stringResource(R.string.show_emojis),
                checked = showEmojis,
                description = stringResource(R.string.show_emojis_description),
            ) { show ->
                scope.launch { ThemeStore.setShowEmojis(context, show) }
            }
            ExpressiveClickableCard(
                onClick = openInterfaceEditor,
                modifier = Modifier.fillMaxWidth(),
                actionDelayMillis = 170,
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        stringResource(R.string.interface_editor),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        stringResource(R.string.interface_editor_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                stringResource(R.string.headphones_color_for_model, colorProtocol.displayName),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Card(Modifier.fillMaxWidth(), shape = AppCardShape) {
                Column {
                    SettingSwitch(
                        label = stringResource(R.string.force_headphones_color),
                        checked = forceHeadphonesColor,
                        description = stringResource(R.string.force_headphones_color_description),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    ) { force ->
                        scope.launch {
                            ThemeStore.setForceHeadphonesColor(context, colorIdentity, force)
                        }
                    }
                    if (forceHeadphonesColor) {
                        Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                            EnumDropdown(
                                title = R.string.headphones_color,
                                values = colorProtocol.availableColors,
                                selected = selectedHeadphonesColor,
                                label = { stringResource(it.label()) },
                                choose = { color ->
                                    scope.launch {
                                        ThemeStore.setHeadphonesColor(context, colorIdentity, color.name)
                                    }
                                },
                            )
                        }
                    }
                }
            }
            BackupSettingsSection()
            Text(
                stringResource(R.string.about_section_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Card(Modifier.fillMaxWidth(), shape = AppCardShape) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val appName = stringResource(R.string.app_name)
                    val linkColor = MaterialTheme.colorScheme.primary
                    Text(
                        androidx.compose.ui.text.buildAnnotatedString {
                            withLink(
                                androidx.compose.ui.text.LinkAnnotation.Url(
                                    "https://github.com/Razziell/FreeBuddy_Next",
                                    styles = androidx.compose.ui.text.TextLinkStyles(
                                        style = androidx.compose.ui.text.SpanStyle(
                                            color = linkColor,
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                        ),
                                    ),
                                ),
                            ) {
                                append(appName)
                            }
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        stringResource(R.string.about_text),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(
                            R.string.version_text,
                            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.theme)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeStore.ThemeMode.entries.forEach { mode ->
                        val selected = themeMode == mode
                        val interaction = remember(mode) { MutableInteractionSource() }
                        val scale = remember(mode) { Animatable(1f) }
                        var pending by remember(mode) { mutableStateOf(false) }
                        LaunchedEffect(interaction) {
                            interaction.interactions.collect { event ->
                                if (event is PressInteraction.Press) {
                                    scale.animateTo(0.97f, tween(75))
                                    scale.animateTo(
                                        1f,
                                        spring(
                                            dampingRatio = 0.86f,
                                            stiffness = Spring.StiffnessMedium,
                                        ),
                                    )
                                }
                            }
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clip(AppCardShape)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                )
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    interactionSource = interaction,
                                    indication = androidx.compose.foundation.LocalIndication.current,
                                    onClick = {
                                        if (!pending) {
                                            pending = true
                                            scope.launch {
                                                try {
                                                    delay(120)
                                                    ThemeStore.setThemeMode(context, mode)
                                                    showThemeDialog = false
                                                } finally {
                                                    pending = false
                                                }
                                            }
                                        }
                                    },
                                )
                                .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(mode.label()),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                ExpressiveTextButton({ showThemeDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

private fun HeadphonesSnapshot.colorIdentity() = HeadphonesIdentity(
    protocolId = protocolId.ifBlank { "freebuds_5i" },
    address = address,
    caseSerial = deviceInfo?.serialNumber,
)

private fun String.toDeviceColor(): DeviceColor =
    runCatching { DeviceColor.valueOf(this) }.getOrDefault(DeviceColor.BLACK)

private fun HomeBlock.label(): Int = when (this) {
    HomeBlock.HEADPHONES_IMAGE -> R.string.interface_editor_block_image
    HomeBlock.BATTERY -> R.string.interface_editor_block_battery
    HomeBlock.ANC -> R.string.interface_editor_block_anc
    HomeBlock.DUAL_CONNECT -> R.string.interface_editor_block_dual_connect
    HomeBlock.SETTINGS_BUTTON -> R.string.interface_editor_block_settings_button
}

private fun SettingsBlock.label(): Int = when (this) {
    SettingsBlock.EQUALIZER -> R.string.equalizer
    SettingsBlock.AUTO_PAUSE -> R.string.auto_pause
    SettingsBlock.LOW_LATENCY -> R.string.low_latency
    SettingsBlock.LDAC -> R.string.ldac
    SettingsBlock.FIND_EARBUDS -> R.string.find_earbuds
    SettingsBlock.GESTURES_TITLE -> R.string.gestures
    SettingsBlock.DOUBLE_TAP -> R.string.double_tap
    SettingsBlock.TRIPLE_TAP -> R.string.triple_tap
    SettingsBlock.HOLD -> R.string.hold
    SettingsBlock.HOLD_MODES -> R.string.hold_modes
    SettingsBlock.SWIPE -> R.string.swipe
}

private fun DeviceColor.label() = when (this) {
    DeviceColor.BLACK -> R.string.headphones_color_black
    DeviceColor.WHITE -> R.string.headphones_color_white
    DeviceColor.BLUE -> R.string.headphones_color_blue
    DeviceColor.PURPLE -> R.string.headphones_color_purple
    DeviceColor.RED -> R.string.headphones_color_red
}

private fun ThemeStore.ThemeMode.label() = when (this) {
    ThemeStore.ThemeMode.SYSTEM -> R.string.theme_system
    ThemeStore.ThemeMode.LIGHT -> R.string.theme_light
    ThemeStore.ThemeMode.DARK -> R.string.theme_dark
}

@Composable private fun AncMode.label() = when (this) { AncMode.OFF -> R.string.anc_off; AncMode.NOISE_CANCELLING -> R.string.anc_noise; AncMode.TRANSPARENCY -> R.string.anc_transparency }
@Composable private fun AncMode.description() = when (this) { AncMode.OFF -> R.string.anc_off_desc; AncMode.NOISE_CANCELLING -> R.string.anc_noise_desc; AncMode.TRANSPARENCY -> R.string.anc_transparency_desc }
@Composable private fun AncLevel.label() = when (this) { AncLevel.NORMAL -> R.string.anc_normal; AncLevel.COMFORT -> R.string.anc_comfort; AncLevel.ULTRA -> R.string.anc_ultra; AncLevel.DYNAMIC -> R.string.anc_dynamic }
@Composable private fun EqPreset.eqLabel() = stringResource(when (this) { EqPreset.DEFAULT -> R.string.eq_default; EqPreset.BASS -> R.string.eq_bass; EqPreset.TREBLE -> R.string.eq_treble; EqPreset.VOICES -> R.string.eq_voices; EqPreset.CLASSICS -> R.string.eq_classics; EqPreset.HIFI -> R.string.eq_hifi })
@Composable private fun DoubleTap.doubleTapLabel() = stringResource(when (this) { DoubleTap.NONE -> R.string.action_none; DoubleTap.ASSISTANT -> R.string.action_assistant; DoubleTap.PLAY_PAUSE -> R.string.action_play_pause; DoubleTap.NEXT -> R.string.action_next; DoubleTap.PREVIOUS -> R.string.action_previous })
@Composable private fun TripleTap.tripleTapLabel() = stringResource(when (this) { TripleTap.NONE -> R.string.action_none; TripleTap.NEXT -> R.string.action_next; TripleTap.PREVIOUS -> R.string.action_previous })

@Composable private fun SplitHoldAction.splitHoldLabel() = stringResource(when (this) {
    SplitHoldAction.NONE -> R.string.action_none
    SplitHoldAction.ASSISTANT -> R.string.action_assistant
    SplitHoldAction.CYCLE_ANC -> R.string.action_cycle_anc
    SplitHoldAction.VOLUME_UP -> R.string.action_volume_up
    SplitHoldAction.VOLUME_DOWN -> R.string.action_volume_down
})

@Composable private fun Hold.holdLabel() = stringResource(if (this == Hold.NONE) R.string.action_none else R.string.action_cycle_anc)
@Composable private fun Swipe.swipeLabel() = stringResource(if (this == Swipe.NONE) R.string.action_none else R.string.action_volume)
