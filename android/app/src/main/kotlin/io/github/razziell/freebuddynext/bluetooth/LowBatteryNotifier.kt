package io.github.razziell.freebuddynext.bluetooth

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.razziell.freebuddynext.MainActivity
import io.github.razziell.freebuddynext.R
import io.github.razziell.freebuddynext.protocol.BatteryLevel
import io.github.razziell.freebuddynext.protocol.HeadphonesSnapshot

internal object LowBatteryNotifier {
    private const val PREFS_FILE = "low_battery_alert_state"
    private const val KEY_ADDRESS = "address"
    private const val KEY_LEFT_THRESHOLD = "left_threshold"
    private const val KEY_RIGHT_THRESHOLD = "right_threshold"
    private const val KEY_CASE_THRESHOLD = "case_threshold"
    private const val KEY_LEFT_ALERTED = "left_alerted"
    private const val KEY_RIGHT_ALERTED = "right_alerted"
    private const val KEY_CASE_ALERTED = "case_alerted"
    private const val KEY_LEFT_PERCENT = "left_percent"
    private const val KEY_RIGHT_PERCENT = "right_percent"
    private const val KEY_CASE_PERCENT = "case_percent"

    private const val CHANNEL_ID = "low_battery_alerts"
    private const val NOTIFICATION_ID = 1001

    @Volatile private var cachedState: LowBatteryTrackerState? = null
    @Volatile private var channelLanguageTag: String? = null

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @Synchronized
    fun onSettingsOrBatteryUpdated(
        context: Context,
        settings: LowBatteryAlertSettings,
        snapshot: HeadphonesSnapshot?,
    ) {
        val normalized = settings.normalized()
        val previous = readState(context)
        val anyTargetEnabled = normalized.leftEnabled || normalized.rightEnabled || normalized.caseEnabled

        if (!normalized.enabled || !anyTargetEnabled) {
            if (previous.leftAlerted || previous.rightAlerted || previous.caseAlerted) {
                writeState(
                    context,
                    LowBatteryTrackerState(
                        address = previous.address,
                        leftThresholdPercent = normalized.leftThresholdPercent,
                        rightThresholdPercent = normalized.rightThresholdPercent,
                        caseThresholdPercent = normalized.caseThresholdPercent,
                    ),
                )
            }
            cancelNotification(context)
            return
        }

        // Wait until the connected session has at least one real battery reading.
        val battery = snapshot?.battery
        val hasAnyReading = battery != null && (
            BatteryLevel.validOrNull(battery.left) != null ||
                BatteryLevel.validOrNull(battery.right) != null ||
                BatteryLevel.validOrNull(battery.case) != null
            )
        if (snapshot == null || !hasAnyReading) {
            // If the user disabled one of the sub-toggles while disconnected, clear its alerted flag.
            val cleaned = previous.copy(
                leftThresholdPercent = normalized.leftThresholdPercent,
                rightThresholdPercent = normalized.rightThresholdPercent,
                caseThresholdPercent = normalized.caseThresholdPercent,
                leftAlerted = previous.leftAlerted && normalized.leftEnabled &&
                    previous.leftThresholdPercent == normalized.leftThresholdPercent,
                rightAlerted = previous.rightAlerted && normalized.rightEnabled &&
                    previous.rightThresholdPercent == normalized.rightThresholdPercent,
                caseAlerted = previous.caseAlerted && normalized.caseEnabled &&
                    previous.caseThresholdPercent == normalized.caseThresholdPercent,
            )
            if (cleaned != previous) {
                writeState(context, cleaned)
                if (!cleaned.leftAlerted && !cleaned.rightAlerted && !cleaned.caseAlerted) {
                    cancelNotification(context)
                }
            }
            return
        }

        // Do not mark components as already alerted before notification permission is granted,
        // so granting permission immediately delivers an alert for currently low batteries.
        if (!hasNotificationPermission(context)) return

        val decision = LowBatteryAlertEvaluator.evaluate(
            previous = previous,
            settings = normalized,
            address = snapshot.address,
            battery = snapshot.battery,
        )
        if (decision.state != previous) {
            writeState(context, decision.state)
        }

        if (decision.shouldCancel) {
            cancelNotification(context)
            BackgroundDiagnostics.record(context, "low battery alert cleared")
        } else if (decision.shouldNotify) {
            showNotification(
                context = context,
                deviceName = snapshot.name,
                leftPercent = decision.leftPercent,
                rightPercent = decision.rightPercent,
                casePercent = decision.casePercent,
            )
            BackgroundDiagnostics.record(
                context,
                "low battery alert shown left=${decision.leftPercent != null} " +
                    "right=${decision.rightPercent != null} case=${decision.casePercent != null}",
            )
        }
    }

    @Synchronized
    fun sendTestNotification(
        context: Context,
        settings: LowBatteryAlertSettings,
        snapshot: HeadphonesSnapshot?,
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        val normalized = settings.normalized()
        val battery = snapshot?.battery
        val left = if (normalized.leftEnabled) {
            BatteryLevel.validOrNull(battery?.left) ?: normalized.leftThresholdPercent
        } else null
        val right = if (normalized.rightEnabled) {
            BatteryLevel.validOrNull(battery?.right) ?: normalized.rightThresholdPercent
        } else null
        val case = if (normalized.caseEnabled) {
            BatteryLevel.validOrNull(battery?.case) ?: normalized.caseThresholdPercent
        } else null

        val hasAny = left != null || right != null || case != null
        showNotification(
            context = context,
            deviceName = snapshot?.name.orEmpty(),
            leftPercent = left ?: if (!hasAny) normalized.leftThresholdPercent else null,
            rightPercent = right,
            casePercent = case,
        )
        BackgroundDiagnostics.record(context, "low battery test notification sent")
        return true
    }

    private fun showNotification(
        context: Context,
        deviceName: String,
        leftPercent: Int?,
        rightPercent: Int?,
        casePercent: Int?,
    ) {
        ensureChannel(context)
        val parts = buildList {
            leftPercent?.let { add(context.getString(R.string.low_battery_item_left, it)) }
            rightPercent?.let { add(context.getString(R.string.low_battery_item_right, it)) }
            casePercent?.let { add(context.getString(R.string.low_battery_item_case, it)) }
        }
        if (parts.isEmpty()) return

        val title = if (deviceName.isNotBlank()) {
            context.getString(R.string.low_battery_notification_title_device, deviceName)
        } else {
            context.getString(R.string.low_battery_notification_title)
        }
        val body = parts.joinToString(" · ")

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.left_earbud)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission was revoked concurrently.
        }
    }

    private fun cancelNotification(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }

    private fun ensureChannel(context: Context) {
        val currentTag = context.resources.configuration.locales[0]?.toLanguageTag().orEmpty()
        if (channelLanguageTag == currentTag) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.low_battery_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.low_battery_channel_description)
        }
        manager.createNotificationChannel(channel)
        channelLanguageTag = currentTag
    }

    private fun readState(context: Context): LowBatteryTrackerState {
        cachedState?.let { return it }
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return LowBatteryTrackerState(
            address = prefs.getString(KEY_ADDRESS, null),
            leftThresholdPercent = prefs.getInt(
                KEY_LEFT_THRESHOLD,
                LowBatteryAlertSettings.DEFAULT_THRESHOLD_PERCENT,
            ),
            rightThresholdPercent = prefs.getInt(
                KEY_RIGHT_THRESHOLD,
                LowBatteryAlertSettings.DEFAULT_THRESHOLD_PERCENT,
            ),
            caseThresholdPercent = prefs.getInt(
                KEY_CASE_THRESHOLD,
                LowBatteryAlertSettings.DEFAULT_THRESHOLD_PERCENT,
            ),
            leftAlerted = prefs.getBoolean(KEY_LEFT_ALERTED, false),
            rightAlerted = prefs.getBoolean(KEY_RIGHT_ALERTED, false),
            caseAlerted = prefs.getBoolean(KEY_CASE_ALERTED, false),
            lastLeftPercent = prefs.getInt(KEY_LEFT_PERCENT, -1).takeIf { it in 1..100 },
            lastRightPercent = prefs.getInt(KEY_RIGHT_PERCENT, -1).takeIf { it in 1..100 },
            lastCasePercent = prefs.getInt(KEY_CASE_PERCENT, -1).takeIf { it in 1..100 },
        ).also { cachedState = it }
    }

    private fun writeState(context: Context, state: LowBatteryTrackerState) {
        cachedState = state
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE).edit()
            .putString(KEY_ADDRESS, state.address)
            .putInt(KEY_LEFT_THRESHOLD, state.leftThresholdPercent)
            .putInt(KEY_RIGHT_THRESHOLD, state.rightThresholdPercent)
            .putInt(KEY_CASE_THRESHOLD, state.caseThresholdPercent)
            .putBoolean(KEY_LEFT_ALERTED, state.leftAlerted)
            .putBoolean(KEY_RIGHT_ALERTED, state.rightAlerted)
            .putBoolean(KEY_CASE_ALERTED, state.caseAlerted)
            .putInt(KEY_LEFT_PERCENT, state.lastLeftPercent ?: -1)
            .putInt(KEY_RIGHT_PERCENT, state.lastRightPercent ?: -1)
            .putInt(KEY_CASE_PERCENT, state.lastCasePercent ?: -1)
            .apply()
    }
}
