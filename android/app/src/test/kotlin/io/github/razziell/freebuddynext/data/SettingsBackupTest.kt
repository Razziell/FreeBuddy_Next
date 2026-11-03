package io.github.razziell.freebuddynext.data

import io.github.razziell.freebuddynext.bluetooth.LowBatteryAlertSettings
import io.github.razziell.freebuddynext.widget.WidgetColorHex
import io.github.razziell.freebuddynext.widget.WidgetColors
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SettingsBackupTest {
    private fun sample() = SettingsBackup(
        InterfaceLayout(hiddenHomeBlocks = setOf(HomeBlock.DUAL_CONNECT)),
        listOf(WidgetAppearanceBackup("battery",
            WidgetColors(2, 0xFF112233.toInt(), 1, 0xFFABCDEF.toInt(), 4, 0xFF778899.toInt()),
            backgroundOpacity = 35, elementOpacity = 80, labelMode = 2)),
        listOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt()),
    )
    private fun json() = JSONObject(SettingsBackupCodec.encode(sample()))

    @Test fun `round trip includes layout all colors opacity labels and favorites`() {
        assertEquals(sample(), SettingsBackupCodec.decode(SettingsBackupCodec.encode(sample())))
    }

    @Test fun `legacy layout preserves widget settings by leaving them absent`() {
        val legacy = SettingsBackupCodec.decode(InterfaceLayoutFileCodec.encode(sample().layout))
        assertEquals(sample().layout, legacy.layout)
        assertNull(legacy.widgets)
        assertNull(legacy.favorites)
    }

    @Test fun `empty favorites are explicit replacement not missing legacy state`() {
        val source = SettingsBackup(InterfaceLayout(), emptyList(), emptyList())
        assertEquals(source, SettingsBackupCodec.decode(SettingsBackupCodec.encode(source)))
    }

    @Test fun `multiple profiles of same type retain distinct order`() {
        val a = sample().widgets!!.single()
        val b = a.copy(backgroundOpacity = 95)
        val source = sample().copy(widgets = listOf(a, b, a.copy(type = "anc")))
        assertEquals(source, SettingsBackupCodec.decode(SettingsBackupCodec.encode(source)))
    }

    @Test fun `all four widget types supported`() {
        val profiles = SettingsBackupCodec.TYPES.map { WidgetAppearanceBackup(it) }
        assertEquals(profiles, SettingsBackupCodec.decodeWidgets(SettingsBackupCodec.encodeWidgets(profiles)))
    }

    @Test fun `rejects unknown format version and widget type`() {
        for ((key, value) in listOf("format" to "other", "version" to 2)) {
            assertThrows(IllegalArgumentException::class.java) {
                SettingsBackupCodec.decode(json().put(key, value).toString())
            }
        }
        val root = json()
        root.getJSONArray("widgets").getJSONObject(0).put("type", "unknown")
        assertThrows(IllegalArgumentException::class.java) { SettingsBackupCodec.decode(root.toString()) }
    }

    @Test fun `rejects invalid opacity modes and noninteger numbers`() {
        for ((key, value) in listOf(
            "backgroundOpacity" to -1, "elementOpacity" to 101,
            "backgroundMode" to 3, "elementMode" to -1, "textMode" to 5, "labelMode" to 3,
            "backgroundOpacity" to 50.5, "textMode" to "2",
        )) {
            val root = json()
            root.getJSONArray("widgets").getJSONObject(0).put(key, value)
            assertThrows(IllegalArgumentException::class.java) { SettingsBackupCodec.decode(root.toString()) }
        }
    }

    @Test fun `rejects malformed RGB rather than silently losing entries`() {
        for (value in listOf("#FFF", "#00112233", "blue", "#GG1122", "  #112233")) {
            val root = json()
            root.getJSONArray("widgets").getJSONObject(0).put("textRgb", value)
            assertThrows(IllegalArgumentException::class.java) { SettingsBackupCodec.decode(root.toString()) }
        }
    }

    @Test fun `rejects duplicates and more than 24 favorites`() {
        for (favorites in listOf(listOf("#112233", "#112233"), List(25) { "#%06X".format(it) })) {
            assertThrows(IllegalArgumentException::class.java) {
                SettingsBackupCodec.decode(json().put("favorites", JSONArray(favorites)).toString())
            }
        }
    }

    @Test fun `accepts 24 favorites preserving order and opaque RGB`() {
        val favorites = List(24) { WidgetColorHex.opaque(it * 12345) }
        val source = sample().copy(favorites = favorites)
        assertEquals(favorites, SettingsBackupCodec.decode(SettingsBackupCodec.encode(source)).favorites)
    }

    @Test fun `rejects oversized file and too many widget profiles`() {
        assertThrows(IllegalArgumentException::class.java) {
            SettingsBackupCodec.decode(" ".repeat(SettingsBackupCodec.MAX_FILE_BYTES + 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            SettingsBackupCodec.encode(sample().copy(widgets = List(SettingsBackupCodec.MAX_WIDGETS + 1) {
                WidgetAppearanceBackup("rows")
            }))
        }
    }

    @Test fun `missing sections are not treated as empty replacement`() {
        for (key in listOf("layout", "widgets", "favorites")) {
            val root = json().apply { remove(key) }
            assertThrows(Exception::class.java) { SettingsBackupCodec.decode(root.toString()) }
        }
    }

    @Test fun `round trip preserves low battery alert settings and rejects invalid thresholds`() {
        val withAlert = sample().copy(
            lowBatteryAlert = LowBatteryAlertSettings(
                enabled = true,
                leftEnabled = true,
                leftThresholdPercent = 15,
                rightEnabled = true,
                rightThresholdPercent = 45,
                caseEnabled = false,
                caseThresholdPercent = 30,
            ),
        )
        assertEquals(withAlert, SettingsBackupCodec.decode(SettingsBackupCodec.encode(withAlert)))

        val badStep = JSONObject(SettingsBackupCodec.encode(withAlert)).apply {
            getJSONObject("lowBatteryAlert").put("leftThresholdPercent", 17)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SettingsBackupCodec.decode(badStep.toString())
        }
    }
}
