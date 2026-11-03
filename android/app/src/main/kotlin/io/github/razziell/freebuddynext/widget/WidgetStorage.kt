package io.github.razziell.freebuddynext.widget

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import io.github.razziell.freebuddynext.protocol.AncMode
import io.github.razziell.freebuddynext.protocol.BatteryLevel
import io.github.razziell.freebuddynext.protocol.BatteryLevels
import io.github.razziell.freebuddynext.bluetooth.BackgroundDiagnostics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object WidgetStorage {
    private const val FILE = "freebuddy_widget"
    private const val REVISION = "state_revision"
    private const val PUBLISHED_REVISION = "published_revision"
    private const val BATTERY_REVISION = "battery_revision"
    private const val ANC_REVISION = "anc_revision"
    private const val APPEARANCE_REVISION = "appearance_revision"
    private val publicationMutex = Mutex()
    private val dataUpdates = Channel<Context>(Channel.CONFLATED)

    init {
        // Fast path for a live process; the durable worker is always enqueued first.
        // One conflated signal batches nearby battery/ANC reports, with no per-packet
        // Glance work in the RFCOMM collector and no ever-growing coroutine queue.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            for (context in dataUpdates) {
                delay(75)
                publishSafely(context)
            }
        }
    }

    @Synchronized
    fun hasPendingPublication(context: Context): Boolean {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return prefs.getLong(REVISION, 0) != prefs.getLong(PUBLISHED_REVISION, 0)
    }

    @Synchronized
    private fun invalidatePublication(context: Context) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val revision = prefs.getLong(REVISION, 0) + 1
        prefs.edit().putLong(REVISION, revision).putLong(APPEARANCE_REVISION, revision).apply()
    }
    const val LEFT = "left"
    const val RIGHT = "right"
    const val CASE = "case"
    private const val ANC_MODE = "anc_mode"
    private const val ANC_CONNECTED = "anc_connected"
    // Legacy single-opacity value is retained as a migration source for
    // widgets configured before split transparency was introduced.
    private const val LEGACY_OPACITY_PREFIX = "opacity_"
    private const val BACKGROUND_OPACITY_PREFIX = "background_opacity_"
    private const val ELEMENT_OPACITY_PREFIX = "element_opacity_"
    private const val LABEL_MODE_PREFIX = "label_mode_"
    const val DEFAULT_OPACITY = 100
    const val LABELS_FULL = 0
    const val LABELS_SHORT = 1
    const val LABELS_HIDDEN = 2

    // Keys for the per-instance Glance state. Pushing values through Glance
    // state (instead of reading prefs in provideGlance) is what makes updates
    // instant: a live composition session only recomposes when its state
    // changes, so values captured before provideContent would stay stale
    // until the session times out.
    val LEFT_KEY = intPreferencesKey(LEFT)
    val RIGHT_KEY = intPreferencesKey(RIGHT)
    val CASE_KEY = intPreferencesKey(CASE)
    val ANC_MODE_KEY = intPreferencesKey(ANC_MODE)
    val ANC_CONNECTED_KEY = intPreferencesKey(ANC_CONNECTED)
    val BACKGROUND_OPACITY_KEY = intPreferencesKey("background_opacity")
    val ELEMENT_OPACITY_KEY = intPreferencesKey("element_opacity")
    val LABEL_MODE_KEY = intPreferencesKey("label_mode")
    internal val BACKGROUND_COLOR_MODE_KEY = intPreferencesKey("background_color_mode")
    internal val BACKGROUND_RGB_KEY = intPreferencesKey("background_rgb")
    internal val ELEMENT_COLOR_MODE_KEY = intPreferencesKey("element_color_mode")
    internal val ELEMENT_RGB_KEY = intPreferencesKey("element_rgb")
    internal val TEXT_COLOR_MODE_KEY = intPreferencesKey("text_color_mode")
    internal val TEXT_RGB_KEY = intPreferencesKey("text_rgb")

    internal fun readColors(
        context: Context,
        appWidgetId: Int,
        fallbackProfile: io.github.razziell.freebuddynext.data.WidgetAppearanceBackup? = null,
    ): WidgetColors {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val defaults = if (prefs.contains("background_color_mode_$appWidgetId")) WidgetColors()
            else (fallbackProfile ?: WidgetBackupStore.fallback(context, appWidgetId))?.colors ?: WidgetColors()
        return WidgetColors(
            WidgetColorMode.normalize(prefs.getInt("background_color_mode_$appWidgetId", defaults.backgroundMode)),
            WidgetColorHex.opaque(prefs.getInt("background_rgb_$appWidgetId", defaults.backgroundRgb)),
            WidgetColorMode.normalize(prefs.getInt("element_color_mode_$appWidgetId", defaults.elementMode)),
            WidgetColorHex.opaque(prefs.getInt("element_rgb_$appWidgetId", defaults.elementRgb)),
            WidgetTextColorMode.normalize(prefs.getInt("text_color_mode_$appWidgetId", defaults.textMode)),
            WidgetColorHex.opaque(prefs.getInt("text_rgb_$appWidgetId", defaults.textRgb)),
        )
    }

    /** Color changes and their publication revision are persisted in the same edit. */
    @Synchronized
    internal fun writeColors(context: Context, appWidgetId: Int, colors: WidgetColors) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val revision = prefs.getLong(REVISION, 0) + 1
        prefs.edit()
            .putInt("background_color_mode_$appWidgetId", WidgetColorMode.normalize(colors.backgroundMode))
            .putInt("background_rgb_$appWidgetId", WidgetColorHex.opaque(colors.backgroundRgb))
            .putInt("element_color_mode_$appWidgetId", WidgetColorMode.normalize(colors.elementMode))
            .putInt("element_rgb_$appWidgetId", WidgetColorHex.opaque(colors.elementRgb))
            .putInt("text_color_mode_$appWidgetId", WidgetTextColorMode.normalize(colors.textMode))
            .putInt("text_rgb_$appWidgetId", WidgetColorHex.opaque(colors.textRgb))
            .putLong(REVISION, revision).putLong(APPEARANCE_REVISION, revision).apply()
    }

    /** Outer widget container opacity for one instance, percent 0..100. */
    internal fun readBackgroundOpacity(
        context: Context,
        appWidgetId: Int,
        fallbackProfile: io.github.razziell.freebuddynext.data.WidgetAppearanceBackup? = null,
    ): Int = readSplitOpacity(context, BACKGROUND_OPACITY_PREFIX, appWidgetId, fallbackProfile)

    /** Progress bars and ANC button opacity, percent 0..100. */
    internal fun readElementOpacity(
        context: Context,
        appWidgetId: Int,
        fallbackProfile: io.github.razziell.freebuddynext.data.WidgetAppearanceBackup? = null,
    ): Int = readSplitOpacity(context, ELEMENT_OPACITY_PREFIX, appWidgetId, fallbackProfile)

    private fun readSplitOpacity(
        context: Context,
        prefix: String,
        appWidgetId: Int,
        fallbackProfile: io.github.razziell.freebuddynext.data.WidgetAppearanceBackup? = null,
    ): Int {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val key = prefix + appWidgetId
        // Existing widgets preserve their exact appearance: the old single
        // value seeds both new controls until the user saves them separately.
        return if (prefs.contains(key)) {
            prefs.getInt(key, DEFAULT_OPACITY)
        } else {
            if (prefs.contains(LEGACY_OPACITY_PREFIX + appWidgetId)) {
                prefs.getInt(LEGACY_OPACITY_PREFIX + appWidgetId, DEFAULT_OPACITY)
            } else {
                val profile = fallbackProfile ?: WidgetBackupStore.fallback(context, appWidgetId)
                (if (prefix == BACKGROUND_OPACITY_PREFIX) profile?.backgroundOpacity else profile?.elementOpacity)
                    ?: DEFAULT_OPACITY
            }
        }.coerceIn(0, 100)
    }

    fun writeOpacities(
        context: Context,
        appWidgetId: Int,
        backgroundOpacity: Int,
        elementOpacity: Int,
    ) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putInt(BACKGROUND_OPACITY_PREFIX + appWidgetId, backgroundOpacity.coerceIn(0, 100))
            .putInt(ELEMENT_OPACITY_PREFIX + appWidgetId, elementOpacity.coerceIn(0, 100))
            .remove(LEGACY_OPACITY_PREFIX + appWidgetId)
            .apply()
    }

    internal fun readLabelMode(
        context: Context,
        appWidgetId: Int,
        fallbackProfile: io.github.razziell.freebuddynext.data.WidgetAppearanceBackup? = null,
    ): Int {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val key = LABEL_MODE_PREFIX + appWidgetId
        return (if (prefs.contains(key)) prefs.getInt(key, LABELS_FULL)
            else (fallbackProfile ?: WidgetBackupStore.fallback(context, appWidgetId))?.labelMode ?: LABELS_FULL)
            .coerceIn(LABELS_FULL, LABELS_HIDDEN)
    }

    fun writeLabelMode(context: Context, appWidgetId: Int, mode: Int) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putInt(LABEL_MODE_PREFIX + appWidgetId, mode.coerceIn(LABELS_FULL, LABELS_HIDDEN))
            .apply()
    }

    fun removeOpacity(context: Context, appWidgetIds: IntArray) {
        val editor = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
        appWidgetIds.forEach {
            editor.remove("background_color_mode_$it").remove("background_rgb_$it")
                .remove("element_color_mode_$it").remove("element_rgb_$it")
                .remove("text_color_mode_$it").remove("text_rgb_$it")
            editor.remove(LEGACY_OPACITY_PREFIX + it)
            editor.remove(BACKGROUND_OPACITY_PREFIX + it)
            editor.remove(ELEMENT_OPACITY_PREFIX + it)
            editor.remove(LABEL_MODE_PREFIX + it)
        }
        editor.apply()
    }

    /** Returns true for new data OR an earlier publication that still needs repair. */
    @Synchronized
    fun saveAnc(context: Context, mode: AncMode?, connected: Boolean): Boolean {
        val modeCode = mode?.code ?: -1
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (prefs.getInt(ANC_MODE, -1) == modeCode &&
            prefs.getBoolean(ANC_CONNECTED, false) == connected
        ) return hasPendingPublication(context)
        val revision = prefs.getLong(REVISION, 0) + 1
        prefs.edit()
            .putLong(REVISION, revision)
            .putLong(ANC_REVISION, revision)
            .putInt(ANC_MODE, modeCode)
            .putBoolean(ANC_CONNECTED, connected)
            .apply()
        return true
    }

    suspend fun saveAncAndUpdate(context: Context, mode: AncMode?, connected: Boolean) {
        if (saveAnc(context, mode, connected)) requestDataUpdate(context)
    }

    fun readAnc(context: Context): Pair<Int, Boolean> {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return prefs.getInt(ANC_MODE, -1) to prefs.getBoolean(ANC_CONNECTED, false)
    }

    /** Persists battery and its dirty revision together; unchanged reports can repair publication. */
    @Synchronized
    fun saveBattery(context: Context, battery: BatteryLevels): Boolean {
        val left = BatteryLevel.validOrNull(battery.left) ?: -1
        val right = BatteryLevel.validOrNull(battery.right) ?: -1
        val case = BatteryLevel.validOrNull(battery.case) ?: -1
        // Buds re-send battery packets with unchanged values; skip the whole
        // bitmap -> RemoteViews -> Binder round-trip when nothing changed.
        if (read(context) == Triple(left, right, case)) return hasPendingPublication(context)
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val revision = prefs.getLong(REVISION, 0) + 1
        prefs.edit()
            .putLong(REVISION, revision)
            .putLong(BATTERY_REVISION, revision)
            .putInt(LEFT, left)
            .putInt(RIGHT, right)
            .putInt(CASE, case)
            .apply()
        return true
    }

    suspend fun saveAndUpdate(context: Context, battery: BatteryLevels) {
        if (saveBattery(context, battery)) requestDataUpdate(context)
    }

    /** Appearance/configuration changes still invalidate every provider immediately. */
    suspend fun updateAll(context: Context) {
        invalidatePublication(context)
        enqueuePublication(context)
        publishSafely(context)
    }

    /** Data is already saved with a dirty revision; do not increment it a second time. */
    suspend fun requestDataUpdate(context: Context) {
        if (!hasPendingPublication(context)) return
        enqueuePublication(context)
        dataUpdates.trySend(context.applicationContext)
    }

    private suspend fun enqueuePublication(context: Context) {
        try {
            WidgetPublishWorker.enqueue(context)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            BackgroundDiagnostics.record(context, "publish enqueue error=${error.javaClass.simpleName}")
        }
    }

    private suspend fun publishSafely(context: Context) {
        try {
            publishAll(context)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            // A launcher/Glance failure must not kill the MBB collector or turn
            // a successful Bluetooth connection into Failed. The revision stays dirty.
            BackgroundDiagnostics.record(context, "publish immediate error=${error.javaClass.simpleName}")
        }
    }

    /** Acknowledges submission to Glance, not actual rendering by the launcher. */
    internal suspend fun publishAll(context: Context) = publicationMutex.withLock {
        val plan = synchronized(this) {
            if (!hasPendingPublication(context)) return@withLock
            val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            val revision = prefs.getLong(REVISION, 0)
            WidgetPublicationPlan.create(revision, prefs.getLong(PUBLISHED_REVISION, 0),
                prefs.getLong(BATTERY_REVISION, 0), prefs.getLong(ANC_REVISION, 0),
                // A pending revision from the previous implementation needs a full repair.
                prefs.getLong(APPEARANCE_REVISION, revision))
        }
        val manager = GlanceAppWidgetManager(context)
        val (left, right, case) = read(context)
        val (ancMode, ancConnected) = readAnc(context)
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        suspend fun push(widget: GlanceAppWidget, cls: Class<out GlanceAppWidget>) {
            manager.getGlanceIds(cls).forEach { id ->
                val appWidgetId = runCatching { manager.getAppWidgetId(id) }.getOrDefault(0)
                val needsFallback = !prefs.contains(BACKGROUND_OPACITY_PREFIX + appWidgetId) ||
                    !prefs.contains(ELEMENT_OPACITY_PREFIX + appWidgetId) ||
                    !prefs.contains(LABEL_MODE_PREFIX + appWidgetId) ||
                    !prefs.contains("background_color_mode_$appWidgetId")
                val fallbackProfile = if (needsFallback) WidgetBackupStore.fallback(context, appWidgetId) else null
                val backgroundOpacity = readBackgroundOpacity(context, appWidgetId, fallbackProfile)
                val elementOpacity = readElementOpacity(context, appWidgetId, fallbackProfile)
                val labelMode = readLabelMode(context, appWidgetId, fallbackProfile)
                val colors = readColors(context, appWidgetId, fallbackProfile)
                updateAppWidgetState(context, id) { prefs ->
                    prefs[LEFT_KEY] = left
                    prefs[RIGHT_KEY] = right
                    prefs[CASE_KEY] = case
                    prefs[ANC_MODE_KEY] = ancMode
                    prefs[ANC_CONNECTED_KEY] = if (ancConnected) 1 else 0
                    prefs[BACKGROUND_OPACITY_KEY] = backgroundOpacity
                    prefs[ELEMENT_OPACITY_KEY] = elementOpacity
                    prefs[LABEL_MODE_KEY] = labelMode
                    prefs[BACKGROUND_COLOR_MODE_KEY] = colors.backgroundMode
                    prefs[BACKGROUND_RGB_KEY] = colors.backgroundRgb
                    prefs[ELEMENT_COLOR_MODE_KEY] = colors.elementMode
                    prefs[ELEMENT_RGB_KEY] = colors.elementRgb
                    prefs[TEXT_COLOR_MODE_KEY] = colors.textMode
                    prefs[TEXT_RGB_KEY] = colors.textRgb
                }
                widget.update(context, id)
            }
        }
        if (plan.battery) {
            push(BatteryWidget(), BatteryWidget::class.java)
            push(BatteryRowsWidget(), BatteryRowsWidget::class.java)
            push(BatteryCompactWidget(), BatteryCompactWidget::class.java)
        }
        if (plan.anc) push(AncWidget(), AncWidget::class.java)
        synchronized(this) {
            // A change during the suspending Glance calls must remain dirty. Even
            // when data was saved before an interrupted enqueue, a later identical
            // report or process start will retry that newer revision.
            val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            prefs.edit().putLong(PUBLISHED_REVISION, plan.revision)
                .putLong(APPEARANCE_REVISION, prefs.getLong(APPEARANCE_REVISION, 0)).apply()
        }
        BackgroundDiagnostics.record(context,
            "publish submitted revision=${plan.revision} battery=${plan.battery} anc=${plan.anc}")
    }

    suspend fun hasAnyWidget(context: Context): Boolean {
        val manager = GlanceAppWidgetManager(context)
        return manager.getGlanceIds(BatteryWidget::class.java).isNotEmpty() ||
            manager.getGlanceIds(BatteryRowsWidget::class.java).isNotEmpty() ||
            manager.getGlanceIds(BatteryCompactWidget::class.java).isNotEmpty() ||
            manager.getGlanceIds(AncWidget::class.java).isNotEmpty()
    }

    fun read(context: Context): Triple<Int, Int, Int> {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return Triple(
            BatteryLevel.validOrNull(prefs.getInt(LEFT, -1)) ?: -1,
            BatteryLevel.validOrNull(prefs.getInt(RIGHT, -1)) ?: -1,
            BatteryLevel.validOrNull(prefs.getInt(CASE, -1)) ?: -1,
        )
    }
}
