package io.github.razziell.freebuddynext.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.razziell.freebuddynext.bluetooth.LowBatteryAlertSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "settings")

/** A physical headset set. Case serial is stable; MAC is used until MBB 1/7 arrives. */
data class HeadphonesIdentity(
    val protocolId: String,
    val address: String? = null,
    val caseSerial: String? = null,
)

/** Stable IDs persisted by the interface editor. Never rename existing entries. */
enum class HomeBlock { HEADPHONES_IMAGE, BATTERY, ANC, DUAL_CONNECT, SETTINGS_BUTTON }

val REQUIRED_HOME_BLOCKS: Set<HomeBlock> = setOf(
    HomeBlock.HEADPHONES_IMAGE,
    HomeBlock.BATTERY,
    HomeBlock.SETTINGS_BUTTON,
)

enum class SettingsBlock {
    EQUALIZER,
    AUTO_PAUSE,
    LOW_LATENCY,
    LDAC,
    FIND_EARBUDS,
    GESTURES_TITLE,
    DOUBLE_TAP,
    TRIPLE_TAP,
    HOLD,
    HOLD_MODES,
    SWIPE,
}

object InterfaceLayoutDefaults {
    const val IMAGE_SCALE_PERCENT = 100
    const val BATTERY_SCALE_PERCENT = 100
    const val ANC_SCALE_PERCENT = 100
    const val BATTERY_ROW_HEIGHT_DP = 56
    const val ANC_BUTTON_SIZE_DP = 64
}

data class InterfaceLayout(
    val homeOrder: List<HomeBlock> = HomeBlock.entries,
    val settingsOrder: List<SettingsBlock> = SettingsBlock.entries,
    val imageScalePercent: Int = InterfaceLayoutDefaults.IMAGE_SCALE_PERCENT,
    val batteryScalePercent: Int = InterfaceLayoutDefaults.BATTERY_SCALE_PERCENT,
    val ancScalePercent: Int = InterfaceLayoutDefaults.ANC_SCALE_PERCENT,
    val hiddenHomeBlocks: Set<HomeBlock> = emptySet(),
    val hiddenSettingsBlocks: Set<SettingsBlock> = emptySet(),
)

/** User-selected appearance settings, persisted via DataStore Preferences. */
object ThemeStore {
    enum class ThemeMode { SYSTEM, LIGHT, DARK }

    private val KEY = stringPreferencesKey("theme_mode")
    private val SHOW_EMOJIS = booleanPreferencesKey("show_emojis")
    private val LOW_BATTERY_ENABLED = booleanPreferencesKey("low_battery_alert_enabled")
    private val LOW_BATTERY_LEFT_ENABLED = booleanPreferencesKey("low_battery_left_enabled")
    private val LOW_BATTERY_LEFT_THRESHOLD = intPreferencesKey("low_battery_left_threshold")
    private val LOW_BATTERY_RIGHT_ENABLED = booleanPreferencesKey("low_battery_right_enabled")
    private val LOW_BATTERY_RIGHT_THRESHOLD = intPreferencesKey("low_battery_right_threshold")
    private val LOW_BATTERY_CASE_ENABLED = booleanPreferencesKey("low_battery_case_enabled")
    private val LOW_BATTERY_CASE_THRESHOLD = intPreferencesKey("low_battery_case_threshold")

    // Legacy global keys (pre-0.3.4). Kept only as migration sources for 5i.
    private val LEGACY_FORCE_HEADPHONES_COLOR = booleanPreferencesKey("force_headphones_color")
    private val LEGACY_HEADPHONES_COLOR = stringPreferencesKey("headphones_color")

    private val LAST_PROTOCOL = stringPreferencesKey("last_headphones_protocol")
    private val LAST_ADDRESS = stringPreferencesKey("last_headphones_address")
    private val LAST_SERIAL = stringPreferencesKey("last_headphones_case_serial")

    fun themeMode(context: Context): Flow<ThemeMode> =
        context.themeDataStore.data.map { prefs ->
            runCatching { ThemeMode.valueOf(prefs[KEY] ?: "") }.getOrDefault(ThemeMode.SYSTEM)
        }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.themeDataStore.edit { it[KEY] = mode.name }
    }

    fun showEmojis(context: Context): Flow<Boolean> =
        context.themeDataStore.data.map { prefs -> prefs[SHOW_EMOJIS] ?: true }

    suspend fun setShowEmojis(context: Context, show: Boolean) {
        context.themeDataStore.edit { it[SHOW_EMOJIS] = show }
    }

    fun lowBatteryAlertSettings(context: Context): Flow<LowBatteryAlertSettings> =
        context.themeDataStore.data.map { prefs ->
            LowBatteryAlertSettings(
                enabled = prefs[LOW_BATTERY_ENABLED] ?: false,
                leftEnabled = prefs[LOW_BATTERY_LEFT_ENABLED] ?: true,
                leftThresholdPercent = LowBatteryAlertSettings.normalizeThreshold(
                    prefs[LOW_BATTERY_LEFT_THRESHOLD] ?: LowBatteryAlertSettings.DEFAULT_THRESHOLD_PERCENT,
                ),
                rightEnabled = prefs[LOW_BATTERY_RIGHT_ENABLED] ?: true,
                rightThresholdPercent = LowBatteryAlertSettings.normalizeThreshold(
                    prefs[LOW_BATTERY_RIGHT_THRESHOLD] ?: LowBatteryAlertSettings.DEFAULT_THRESHOLD_PERCENT,
                ),
                caseEnabled = prefs[LOW_BATTERY_CASE_ENABLED] ?: true,
                caseThresholdPercent = LowBatteryAlertSettings.normalizeThreshold(
                    prefs[LOW_BATTERY_CASE_THRESHOLD] ?: LowBatteryAlertSettings.DEFAULT_THRESHOLD_PERCENT,
                ),
            )
        }

    suspend fun setLowBatteryAlertSettings(context: Context, settings: LowBatteryAlertSettings) {
        val normalized = settings.normalized()
        context.themeDataStore.edit { prefs ->
            prefs[LOW_BATTERY_ENABLED] = normalized.enabled
            prefs[LOW_BATTERY_LEFT_ENABLED] = normalized.leftEnabled
            prefs[LOW_BATTERY_LEFT_THRESHOLD] = normalized.leftThresholdPercent
            prefs[LOW_BATTERY_RIGHT_ENABLED] = normalized.rightEnabled
            prefs[LOW_BATTERY_RIGHT_THRESHOLD] = normalized.rightThresholdPercent
            prefs[LOW_BATTERY_CASE_ENABLED] = normalized.caseEnabled
            prefs[LOW_BATTERY_CASE_THRESHOLD] = normalized.caseThresholdPercent
        }
    }

    /** Effective per-device setting: serial -> remembered MAC mapping -> MAC -> model -> legacy. */
    fun forceHeadphonesColor(context: Context, identity: HeadphonesIdentity): Flow<Boolean> =
        context.themeDataStore.data.map { prefs -> resolveForce(prefs, identity) }

    fun headphonesColor(context: Context, identity: HeadphonesIdentity): Flow<String> =
        context.themeDataStore.data.map { prefs -> resolveColor(prefs, identity) }

    suspend fun setForceHeadphonesColor(
        context: Context,
        identity: HeadphonesIdentity,
        force: Boolean,
    ) {
        context.themeDataStore.edit { prefs ->
            prefs[booleanPreferencesKey(forceColorPreferenceName(resolveWriteScope(prefs, identity)))] = force
            rememberLast(prefs, identity)
        }
    }

    suspend fun setHeadphonesColor(
        context: Context,
        identity: HeadphonesIdentity,
        color: String,
    ) {
        context.themeDataStore.edit { prefs ->
            prefs[stringPreferencesKey(headphonesColorPreferenceName(resolveWriteScope(prefs, identity)))] = color
            rememberLast(prefs, identity)
        }
    }

    /**
     * Saves MAC -> case serial and atomically migrates the effective MAC/model value to serial.
     * Calling this repeatedly is harmless and never overwrites an explicit serial setting.
     */
    suspend fun registerHeadphones(context: Context, identity: HeadphonesIdentity) {
        context.themeDataStore.edit { prefs ->
            val forceBefore = resolveForceOrNull(prefs, identity)
            val colorBefore = resolveColorOrNull(prefs, identity)
            val serial = identity.caseSerial.cleanId()
            val address = identity.address.cleanId()
            if (serial != null && address != null) {
                prefs[stringPreferencesKey(macSerialPreferenceName(identity.protocolId, address))] = serial
                val serialScope = serialScope(identity.protocolId, serial)
                val forceKey = booleanPreferencesKey(forceColorPreferenceName(serialScope))
                val colorKey = stringPreferencesKey(headphonesColorPreferenceName(serialScope))
                if (prefs[forceKey] == null && forceBefore != null) prefs[forceKey] = forceBefore
                if (prefs[colorKey] == null && colorBefore != null) prefs[colorKey] = colorBefore
            }
            rememberLast(prefs, identity)
        }
    }

    fun interfaceLayout(context: Context): Flow<InterfaceLayout> =
        context.themeDataStore.data.map { prefs ->
            InterfaceLayout(
                homeOrder = parseOrder(
                    prefs[stringPreferencesKey("home_layout_order")],
                    HomeBlock.entries,
                ),
                settingsOrder = parseOrder(
                    prefs[stringPreferencesKey("settings_layout_order")],
                    SettingsBlock.entries,
                ),
                imageScalePercent = (prefs[stringPreferencesKey("home_image_scale")]?.toIntOrNull()
                    ?: InterfaceLayoutDefaults.IMAGE_SCALE_PERCENT).coerceIn(70, 140).snap(5),
                batteryScalePercent = (
                    prefs[stringPreferencesKey("home_battery_scale")]?.toIntOrNull()
                        ?: InterfaceLayoutDefaults.BATTERY_SCALE_PERCENT
                ).coerceIn(80, 140).snap(5),
                ancScalePercent = (
                    prefs[stringPreferencesKey("home_anc_scale")]?.toIntOrNull()
                        ?: InterfaceLayoutDefaults.ANC_SCALE_PERCENT
                ).coerceIn(80, 125).snap(5),
                hiddenHomeBlocks = parseSelection<HomeBlock>(
                    prefs[stringPreferencesKey("home_layout_hidden")],
                ) - REQUIRED_HOME_BLOCKS,
                hiddenSettingsBlocks = parseSelection(
                    prefs[stringPreferencesKey("settings_layout_hidden")],
                ),
            )
        }

    suspend fun setHomeOrder(context: Context, order: List<HomeBlock>) {
        context.themeDataStore.edit {
            it[stringPreferencesKey("home_layout_order")] =
                normalizeOrder(order, HomeBlock.entries).joinToString(",") { block -> block.name }
        }
    }

    suspend fun setSettingsOrder(context: Context, order: List<SettingsBlock>) {
        context.themeDataStore.edit {
            it[stringPreferencesKey("settings_layout_order")] =
                normalizeOrder(order, SettingsBlock.entries).joinToString(",") { block -> block.name }
        }
    }

    suspend fun setHomeBlockVisible(context: Context, block: HomeBlock, visible: Boolean) {
        if (block in REQUIRED_HOME_BLOCKS && !visible) return
        context.themeDataStore.edit { prefs ->
            val hidden = parseSelection<HomeBlock>(prefs[stringPreferencesKey("home_layout_hidden")])
                .toMutableSet()
                .apply { if (visible) remove(block) else add(block) }
            prefs[stringPreferencesKey("home_layout_hidden")] =
                hidden.joinToString(",") { it.name }
        }
    }

    suspend fun setSettingsBlockVisible(context: Context, block: SettingsBlock, visible: Boolean) {
        context.themeDataStore.edit { prefs ->
            val hidden = parseSelection<SettingsBlock>(prefs[stringPreferencesKey("settings_layout_hidden")])
                .toMutableSet()
                .apply { if (visible) remove(block) else add(block) }
            prefs[stringPreferencesKey("settings_layout_hidden")] =
                hidden.joinToString(",") { it.name }
        }
    }

    suspend fun setInterfaceScales(
        context: Context,
        imageScalePercent: Int,
        batteryScalePercent: Int,
        ancScalePercent: Int,
    ) {
        context.themeDataStore.edit {
            it[stringPreferencesKey("home_image_scale")] = imageScalePercent.coerceIn(70, 140).snap(5).toString()
            it[stringPreferencesKey("home_battery_scale")] = batteryScalePercent.coerceIn(80, 140).snap(5).toString()
            it[stringPreferencesKey("home_anc_scale")] = ancScalePercent.coerceIn(80, 125).snap(5).toString()
        }
    }

    /** Atomically replaces the shared order and scales after a validated import. */
    suspend fun setInterfaceLayout(context: Context, layout: InterfaceLayout) {
        context.themeDataStore.edit {
            it[stringPreferencesKey("home_layout_order")] =
                normalizeOrder(layout.homeOrder, HomeBlock.entries).joinToString(",") { block -> block.name }
            it[stringPreferencesKey("settings_layout_order")] =
                normalizeOrder(layout.settingsOrder, SettingsBlock.entries).joinToString(",") { block -> block.name }
            it[stringPreferencesKey("home_image_scale")] =
                layout.imageScalePercent.coerceIn(70, 140).snap(5).toString()
            it[stringPreferencesKey("home_battery_scale")] =
                layout.batteryScalePercent.coerceIn(80, 140).snap(5).toString()
            it[stringPreferencesKey("home_anc_scale")] =
                layout.ancScalePercent.coerceIn(80, 125).snap(5).toString()
            it[stringPreferencesKey("home_layout_hidden")] =
                layout.hiddenHomeBlocks.filter { block ->
                    block in HomeBlock.entries && block !in REQUIRED_HOME_BLOCKS
                }.joinToString(",") { block -> block.name }
            it[stringPreferencesKey("settings_layout_hidden")] =
                layout.hiddenSettingsBlocks.filter { block -> block in SettingsBlock.entries }
                    .joinToString(",") { block -> block.name }
        }
    }

    suspend fun resetInterfaceLayout(context: Context) {
        context.themeDataStore.edit {
            listOf(
                "home_layout_order",
                "settings_layout_order",
                "home_image_scale",
                "home_battery_scale",
                "home_anc_scale",
                "home_layout_hidden",
                "settings_layout_hidden",
            ).forEach { key -> it.remove(stringPreferencesKey(key)) }
        }
    }

    fun lastHeadphonesIdentity(context: Context): Flow<HeadphonesIdentity> =
        context.themeDataStore.data.map { prefs ->
            HeadphonesIdentity(
                protocolId = prefs[LAST_PROTOCOL] ?: "freebuds_5i",
                address = prefs[LAST_ADDRESS],
                caseSerial = prefs[LAST_SERIAL],
            )
        }

    private fun rememberLast(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
        identity: HeadphonesIdentity,
    ) {
        prefs[LAST_PROTOCOL] = identity.protocolId
        val address = identity.address.cleanId()
        if (address != null) prefs[LAST_ADDRESS] = address else prefs.remove(LAST_ADDRESS)
        val serial = identity.caseSerial.cleanId() ?: address?.let {
            prefs[stringPreferencesKey(macSerialPreferenceName(identity.protocolId, it))]
        }
        if (serial != null) prefs[LAST_SERIAL] = serial else prefs.remove(LAST_SERIAL)
    }

    private fun resolveForce(prefs: Preferences, identity: HeadphonesIdentity): Boolean =
        resolveForceOrNull(prefs, identity) ?: false

    private fun resolveColor(prefs: Preferences, identity: HeadphonesIdentity): String =
        resolveColorOrNull(prefs, identity) ?: "BLACK"

    private fun resolveForceOrNull(prefs: Preferences, identity: HeadphonesIdentity): Boolean? {
        preferenceScopes(prefs, identity).forEach { scope ->
            prefs[booleanPreferencesKey(forceColorPreferenceName(scope))]?.let { return it }
        }
        return prefs[LEGACY_FORCE_HEADPHONES_COLOR].takeIf { identity.protocolId == "freebuds_5i" }
    }

    private fun resolveColorOrNull(prefs: Preferences, identity: HeadphonesIdentity): String? {
        preferenceScopes(prefs, identity).forEach { scope ->
            prefs[stringPreferencesKey(headphonesColorPreferenceName(scope))]?.let { return it }
        }
        return prefs[LEGACY_HEADPHONES_COLOR].takeIf { identity.protocolId == "freebuds_5i" }
    }

    private fun preferenceScopes(prefs: Preferences, identity: HeadphonesIdentity): List<String> = buildList {
        val address = identity.address.cleanId()
        val directSerial = identity.caseSerial.cleanId()
        val mappedSerial = address?.let {
            prefs[stringPreferencesKey(macSerialPreferenceName(identity.protocolId, it))]
        }
        (directSerial ?: mappedSerial)?.let { add(serialScope(identity.protocolId, it)) }
        address?.let { add(macScope(identity.protocolId, it)) }
        // Previous model-scoped implementation used the raw protocol id as scope.
        add(identity.protocolId.preferenceKeyPart())
        add(protocolScope(identity.protocolId))
    }.distinct()

    private fun resolveWriteScope(prefs: Preferences, identity: HeadphonesIdentity): String {
        val serial = identity.caseSerial.cleanId()
            ?: identity.address.cleanId()?.let {
                prefs[stringPreferencesKey(macSerialPreferenceName(identity.protocolId, it))]
            }
        return when {
            serial != null -> serialScope(identity.protocolId, serial)
            identity.address.cleanId() != null -> macScope(identity.protocolId, identity.address.cleanId()!!)
            else -> protocolScope(identity.protocolId)
        }
    }
}

internal fun scaleDp(baseDp: Int, percent: Int): Float = baseDp * percent / 100f

internal inline fun <reified T : Enum<T>> parseOrder(value: String?, defaults: List<T>): List<T> {
    val parsed = value.orEmpty().split(',').mapNotNull { name ->
        enumValues<T>().firstOrNull { it.name == name }
    }
    return normalizeOrder(parsed, defaults)
}

internal fun <T> normalizeOrder(order: List<T>, defaults: List<T>): List<T> =
    (order.filter { it in defaults } + defaults.filterNot { it in order }).distinct()

internal inline fun <reified T : Enum<T>> parseSelection(value: String?): Set<T> =
    value.orEmpty().split(',').mapNotNull { name ->
        enumValues<T>().firstOrNull { it.name == name }
    }.toSet()

internal fun Int.snap(step: Int): Int = ((this + step / 2) / step) * step

internal fun protocolScope(protocolId: String): String =
    "${protocolId.preferenceKeyPart()}_model"

internal fun macScope(protocolId: String, address: String): String =
    "${protocolId.preferenceKeyPart()}_mac_${address.preferenceKeyPart()}"

internal fun serialScope(protocolId: String, serial: String): String =
    "${protocolId.preferenceKeyPart()}_serial_${serial.preferenceKeyPart()}"

internal fun forceColorPreferenceName(scope: String): String =
    "force_headphones_color_$scope"

internal fun headphonesColorPreferenceName(scope: String): String =
    "headphones_color_$scope"

internal fun macSerialPreferenceName(protocolId: String, address: String): String =
    "headphones_serial_${protocolId.preferenceKeyPart()}_${address.preferenceKeyPart()}"

private fun String?.cleanId(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

private fun String.preferenceKeyPart(): String =
    lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_')
