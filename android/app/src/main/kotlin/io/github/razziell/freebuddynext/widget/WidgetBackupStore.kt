package io.github.razziell.freebuddynext.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import io.github.razziell.freebuddynext.data.SettingsBackupCodec
import io.github.razziell.freebuddynext.data.WidgetAppearanceBackup
import org.json.JSONArray

/** Maps portable per-type profiles to local instances, without exporting launcher IDs. */
internal object WidgetBackupStore {
    private const val FILE = "widget_backup_profiles"
    private const val PROFILES = "profiles"
    private val providers = linkedMapOf(
        "battery" to BatteryWidgetReceiver::class.java,
        "rows" to BatteryRowsWidgetReceiver::class.java,
        "compact" to BatteryCompactWidgetReceiver::class.java,
        "anc" to AncWidgetReceiver::class.java,
    )

    @Volatile private var cachedProfiles: List<WidgetAppearanceBackup>? = null

    private fun ids(context: Context, type: String): List<Int> =
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, providers.getValue(type))).sorted()

    @Synchronized
    private fun saved(context: Context): List<WidgetAppearanceBackup> {
        cachedProfiles?.let { return it }
        val json = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(PROFILES, null)
            ?: return emptyList<WidgetAppearanceBackup>().also { cachedProfiles = it }
        val parsed = runCatching { SettingsBackupCodec.decodeWidgets(JSONArray(json)) }
            .getOrDefault(emptyList())
        cachedProfiles = parsed
        return parsed
    }

    /** Defaults for new widgets after restore. Explicit per-instance settings always win. */
    fun fallback(context: Context, id: Int): WidgetAppearanceBackup? {
        val profiles = saved(context)
        if (profiles.isEmpty()) return null
        val provider = AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.provider ?: return null
        val type = providers.keys.firstOrNull { ComponentName(context, providers.getValue(it)) == provider }
            ?: return null
        val index = ids(context, type).indexOf(id)
        return profiles.filter { it.type == type }.getOrNull(index)
    }

    fun capture(context: Context): List<WidgetAppearanceBackup> {
        val retained = saved(context)
        return providers.keys.flatMap { type ->
            val current = ids(context, type).map { id ->
                WidgetAppearanceBackup(type, WidgetStorage.readColors(context, id),
                    WidgetStorage.readBackgroundOpacity(context, id), WidgetStorage.readElementOpacity(context, id),
                    WidgetStorage.readLabelMode(context, id))
            }
            // Keep restored profiles for widgets not yet recreated on this launcher.
            current + retained.filter { it.type == type }.drop(current.size)
        }
    }

    fun restore(context: Context, profiles: List<WidgetAppearanceBackup>, favorites: List<Int>) {
        val json = SettingsBackupCodec.encodeWidgets(profiles)
        SettingsBackupCodec.decodeWidgets(json) // Validate everything before writing.
        require(favorites == WidgetFavoriteColors.normalize(favorites))
        check(context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(PROFILES, json.toString()).commit()) { "Cannot save widget profiles" }
        synchronized(this) { cachedProfiles = profiles }
        providers.keys.forEach { type ->
            ids(context, type).zip(profiles.filter { it.type == type }).forEach { (id, item) ->
                WidgetStorage.writeColors(context, id, item.colors)
                WidgetStorage.writeOpacities(context, id, item.backgroundOpacity, item.elementOpacity)
                WidgetStorage.writeLabelMode(context, id, item.labelMode)
            }
        }
        check(context.getSharedPreferences(FAVORITES_FILE, Context.MODE_PRIVATE).edit()
            .putString(FAVORITE_COLORS_KEY, WidgetFavoriteColors.encode(favorites)).commit()) {
            "Cannot save favorites"
        }
    }

    fun favorites(context: Context): List<Int> = WidgetFavoriteColors.decode(
        context.getSharedPreferences(FAVORITES_FILE, Context.MODE_PRIVATE)
            .getString(FAVORITE_COLORS_KEY, "").orEmpty())
}

internal const val FAVORITES_FILE = "widget_favorite_colors"
internal const val FAVORITE_COLORS_KEY = "colors"
