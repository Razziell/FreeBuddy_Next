package io.github.razziell.freebuddynext.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.razziell.freebuddynext.R

internal val LocalShowEmojis = staticCompositionLocalOf { true }
private val EmojiRegex = Regex(
    """[\x{1F000}-\x{1FAFF}\x{2100}-\x{214F}\x{2300}-\x{25FF}\x{2600}-\x{27BF}\uFE0E\uFE0F\u200D]+""",
)

/** Shared by the main settings, companion setup and widget configuration activities. */
@Composable
internal fun appStringResource(@StringRes id: Int, vararg formatArgs: Any): String {
    val value = if (formatArgs.isEmpty()) androidx.compose.ui.res.stringResource(id)
        else androidx.compose.ui.res.stringResource(id, *formatArgs)
    if (!LocalShowEmojis.current) {
        return value.replace(EmojiRegex, "").replace(Regex(" {2,}"), " ").trim()
    }
    val emoji = when (id) {
        R.string.about -> "⚙️"
        R.string.about_section_title -> "ℹ️"
        R.string.companion_section_title -> "🔧"
        R.string.companion_title -> "🔄"
        R.string.low_battery_alert_title -> "🪫"
        R.string.low_battery_left_toggle, R.string.low_battery_right_toggle -> "🎧"
        R.string.low_battery_case_toggle -> "🔋"
        R.string.low_battery_permission_action, R.string.low_battery_test_action -> "🔔"
        R.string.backup_title -> "💾"
        R.string.appearance -> "🎨"
        R.string.theme -> "🌓"
        R.string.theme_system -> "🖥️"
        R.string.theme_light -> "☀️"
        R.string.theme_dark -> "🌙"
        R.string.show_emojis -> "😊"
        R.string.interface_editor -> "🛠️"
        R.string.interface_editor_common_layout -> "📐"
        R.string.interface_editor_home_order -> "🏠"
        R.string.interface_editor_sizes -> "📏"
        R.string.widget_transparency_title -> "🎨"
        R.string.headphones_color_for_model, R.string.headphones_color -> "🎨"
        R.string.force_headphones_color -> "🎧"
        R.string.companion_permission -> "🔓"
        R.string.companion_enable -> "🔗"
        R.string.companion_retry, R.string.companion_refresh -> "🔄"
        R.string.companion_remove -> "🔗"
        R.string.widget_color_source, R.string.widget_color_choose -> "🎨"
        R.string.widget_background_transparency, R.string.widget_elements_transparency -> "🌫️"
        R.string.widget_background_color, R.string.widget_tab_background -> "🎨"
        R.string.widget_elements_color, R.string.widget_tab_elements -> "🧩"
        R.string.widget_text_color, R.string.widget_tab_text -> "✏️"
        R.string.widget_tab_labels, R.string.widget_labels_title -> "🏷️"
        R.string.widget_favorites_title -> "⭐"
        else -> null
    }
    // Some translations already contain decorative emoji; do not duplicate them.
    return if (emoji == null || EmojiRegex.containsMatchIn(value)) value else "$value $emoji"
}
