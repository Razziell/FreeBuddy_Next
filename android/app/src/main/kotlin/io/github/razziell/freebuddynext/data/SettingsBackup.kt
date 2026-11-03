package io.github.razziell.freebuddynext.data

import io.github.razziell.freebuddynext.bluetooth.LowBatteryAlertSettings
import io.github.razziell.freebuddynext.widget.WidgetColorHex
import io.github.razziell.freebuddynext.widget.WidgetColors
import io.github.razziell.freebuddynext.widget.WidgetFavoriteColors
import org.json.JSONArray
import org.json.JSONObject

internal data class WidgetAppearanceBackup(
    val type: String,
    val colors: WidgetColors = WidgetColors(),
    val backgroundOpacity: Int = 100,
    val elementOpacity: Int = 100,
    val labelMode: Int = 0,
)

internal data class SettingsBackup(
    val layout: InterfaceLayout,
    // null means a legacy layout-only file: do not change widget settings.
    val widgets: List<WidgetAppearanceBackup>? = null,
    val favorites: List<Int>? = null,
    val lowBatteryAlert: LowBatteryAlertSettings? = null,
)

/** Portable settings, never Bluetooth identities, live telemetry or launcher widget IDs. */
internal object SettingsBackupCodec {
    const val FORMAT = "freebuddy-next-settings"
    const val FILE_NAME = "Settings_FBN.json"
    const val MAX_FILE_BYTES = 128 * 1024
    const val MAX_WIDGETS = 128
    val TYPES = setOf("battery", "rows", "compact", "anc")

    fun encode(backup: SettingsBackup): String {
        val root = JSONObject().apply {
            put("format", FORMAT)
            put("version", 1)
            put("layout", JSONObject(InterfaceLayoutFileCodec.encode(backup.layout)))
            put("widgets", encodeWidgets(backup.widgets.orEmpty()))
            put("favorites", JSONArray(backup.favorites.orEmpty().map(WidgetColorHex::format)))
            backup.lowBatteryAlert?.let { put("lowBatteryAlert", encodeLowBatteryAlert(it)) }
        }
        return root.toString(2).also { decode(it) }
    }

    fun decode(json: String): SettingsBackup {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_FILE_BYTES) { "File is too large" }
        val root = JSONObject(json)
        if (root.optString("format") == InterfaceLayoutFileCodec.FORMAT) {
            return SettingsBackup(InterfaceLayoutFileCodec.decode(json).layout)
        }
        require(root.getString("format") == FORMAT) { "Unsupported format" }
        require(integer(root, "version", 1..1) == 1)
        val layout = InterfaceLayoutFileCodec.decode(root.getJSONObject("layout").toString()).layout
        val widgets = decodeWidgets(root.getJSONArray("widgets"))
        val array = root.getJSONArray("favorites")
        require(array.length() <= WidgetFavoriteColors.LIMIT)
        val favorites = List(array.length()) { rgb(array.get(it)) }
        require(favorites.distinct().size == favorites.size) { "Duplicate favorite" }
        val lowBatteryAlert = if (root.has("lowBatteryAlert")) {
            decodeLowBatteryAlert(root.getJSONObject("lowBatteryAlert"))
        } else {
            null
        }
        return SettingsBackup(layout, widgets, favorites, lowBatteryAlert)
    }

    private fun encodeLowBatteryAlert(settings: LowBatteryAlertSettings): JSONObject {
        val normalized = settings.normalized()
        return JSONObject().apply {
            put("enabled", normalized.enabled)
            put("leftEnabled", normalized.leftEnabled)
            put("leftThresholdPercent", normalized.leftThresholdPercent)
            put("rightEnabled", normalized.rightEnabled)
            put("rightThresholdPercent", normalized.rightThresholdPercent)
            put("caseEnabled", normalized.caseEnabled)
            put("caseThresholdPercent", normalized.caseThresholdPercent)
        }
    }

    private fun decodeLowBatteryAlert(obj: JSONObject): LowBatteryAlertSettings {
        val range = LowBatteryAlertSettings.MIN_THRESHOLD_PERCENT..LowBatteryAlertSettings.MAX_THRESHOLD_PERCENT
        val leftEnabled: Boolean
        val leftThreshold: Int
        val rightEnabled: Boolean
        val rightThreshold: Int
        if (obj.has("leftEnabled") || obj.has("rightEnabled")) {
            leftEnabled = boolean(obj, "leftEnabled")
            leftThreshold = integer(obj, "leftThresholdPercent", range)
            rightEnabled = boolean(obj, "rightEnabled")
            rightThreshold = integer(obj, "rightThresholdPercent", range)
        } else {
            val sharedEnabled = boolean(obj, "earbudsEnabled")
            val sharedThreshold = integer(obj, "earbudsThresholdPercent", range)
            leftEnabled = sharedEnabled
            leftThreshold = sharedThreshold
            rightEnabled = sharedEnabled
            rightThreshold = sharedThreshold
        }
        val caseThreshold = integer(obj, "caseThresholdPercent", range)
        require(leftThreshold % LowBatteryAlertSettings.STEP_PERCENT == 0) { "Invalid leftThresholdPercent" }
        require(rightThreshold % LowBatteryAlertSettings.STEP_PERCENT == 0) { "Invalid rightThresholdPercent" }
        require(caseThreshold % LowBatteryAlertSettings.STEP_PERCENT == 0) { "Invalid caseThresholdPercent" }
        return LowBatteryAlertSettings(
            enabled = boolean(obj, "enabled"),
            leftEnabled = leftEnabled,
            leftThresholdPercent = leftThreshold,
            rightEnabled = rightEnabled,
            rightThresholdPercent = rightThreshold,
            caseEnabled = boolean(obj, "caseEnabled"),
            caseThresholdPercent = caseThreshold,
        )
    }

    fun encodeWidgets(widgets: List<WidgetAppearanceBackup>): JSONArray = JSONArray().apply {
        widgets.forEach { item ->
            put(JSONObject().apply {
                put("type", item.type)
                put("backgroundOpacity", item.backgroundOpacity)
                put("elementOpacity", item.elementOpacity)
                put("labelMode", item.labelMode)
                put("backgroundMode", item.colors.backgroundMode)
                put("backgroundRgb", WidgetColorHex.format(item.colors.backgroundRgb))
                put("elementMode", item.colors.elementMode)
                put("elementRgb", WidgetColorHex.format(item.colors.elementRgb))
                put("textMode", item.colors.textMode)
                put("textRgb", WidgetColorHex.format(item.colors.textRgb))
            })
        }
    }

    fun decodeWidgets(array: JSONArray): List<WidgetAppearanceBackup> {
        require(array.length() <= MAX_WIDGETS)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val type = item.getString("type")
            require(type in TYPES) { "Unknown widget type" }
            WidgetAppearanceBackup(
                type = type,
                colors = WidgetColors(
                    integer(item, "backgroundMode", 0..2), rgb(item.get("backgroundRgb")),
                    integer(item, "elementMode", 0..2), rgb(item.get("elementRgb")),
                    integer(item, "textMode", 0..4), rgb(item.get("textRgb")),
                ),
                backgroundOpacity = integer(item, "backgroundOpacity", 0..100),
                elementOpacity = integer(item, "elementOpacity", 0..100),
                labelMode = integer(item, "labelMode", 0..2),
            )
        }
    }

    private fun integer(root: JSONObject, key: String, range: IntRange): Int {
        val value = root.get(key)
        require(value is Int && value in range) { "Invalid $key" }
        return value
    }

    private fun boolean(root: JSONObject, key: String): Boolean {
        val value = root.get(key)
        require(value is Boolean) { "Invalid $key" }
        return value
    }

    private fun rgb(value: Any): Int {
        require(value is String && value.matches(Regex("#[0-9a-fA-F]{6}"))) { "Invalid RGB" }
        return requireNotNull(WidgetColorHex.parse(value))
    }
}
