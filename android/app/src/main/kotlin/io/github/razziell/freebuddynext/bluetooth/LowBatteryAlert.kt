package io.github.razziell.freebuddynext.bluetooth

import io.github.razziell.freebuddynext.protocol.BatteryLevel
import io.github.razziell.freebuddynext.protocol.BatteryLevels

data class LowBatteryAlertSettings(
    val enabled: Boolean = false,
    val leftEnabled: Boolean = true,
    val leftThresholdPercent: Int = DEFAULT_THRESHOLD_PERCENT,
    val rightEnabled: Boolean = true,
    val rightThresholdPercent: Int = DEFAULT_THRESHOLD_PERCENT,
    val caseEnabled: Boolean = true,
    val caseThresholdPercent: Int = DEFAULT_THRESHOLD_PERCENT,
) {
    fun normalized(): LowBatteryAlertSettings = copy(
        leftThresholdPercent = normalizeThreshold(leftThresholdPercent),
        rightThresholdPercent = normalizeThreshold(rightThresholdPercent),
        caseThresholdPercent = normalizeThreshold(caseThresholdPercent),
    )

    companion object {
        const val MIN_THRESHOLD_PERCENT = 5
        const val MAX_THRESHOLD_PERCENT = 80
        const val STEP_PERCENT = 5
        const val DEFAULT_THRESHOLD_PERCENT = 20

        fun normalizeThreshold(value: Int): Int {
            val clamped = value.coerceIn(MIN_THRESHOLD_PERCENT, MAX_THRESHOLD_PERCENT)
            return ((clamped + STEP_PERCENT / 2) / STEP_PERCENT * STEP_PERCENT)
                .coerceIn(MIN_THRESHOLD_PERCENT, MAX_THRESHOLD_PERCENT)
        }
    }
}

internal data class LowBatteryTrackerState(
    val address: String? = null,
    val leftThresholdPercent: Int = LowBatteryAlertSettings.DEFAULT_THRESHOLD_PERCENT,
    val rightThresholdPercent: Int = LowBatteryAlertSettings.DEFAULT_THRESHOLD_PERCENT,
    val caseThresholdPercent: Int = LowBatteryAlertSettings.DEFAULT_THRESHOLD_PERCENT,
    val leftAlerted: Boolean = false,
    val rightAlerted: Boolean = false,
    val caseAlerted: Boolean = false,
    val lastLeftPercent: Int? = null,
    val lastRightPercent: Int? = null,
    val lastCasePercent: Int? = null,
)

internal data class LowBatteryDecision(
    val state: LowBatteryTrackerState,
    val shouldNotify: Boolean = false,
    val shouldCancel: Boolean = false,
    val leftPercent: Int? = null,
    val rightPercent: Int? = null,
    val casePercent: Int? = null,
)

internal object LowBatteryAlertEvaluator {

    fun evaluate(
        previous: LowBatteryTrackerState,
        settings: LowBatteryAlertSettings,
        address: String?,
        battery: BatteryLevels?,
    ): LowBatteryDecision {
        val normalized = settings.normalized()
        val anyTargetEnabled = normalized.leftEnabled || normalized.rightEnabled || normalized.caseEnabled
        if (!normalized.enabled || !anyTargetEnabled || address == null || battery == null) {
            val hadAlert = previous.leftAlerted || previous.rightAlerted || previous.caseAlerted
            return LowBatteryDecision(
                state = LowBatteryTrackerState(
                    address = address,
                    leftThresholdPercent = normalized.leftThresholdPercent,
                    rightThresholdPercent = normalized.rightThresholdPercent,
                    caseThresholdPercent = normalized.caseThresholdPercent,
                ),
                shouldCancel = hadAlert,
            )
        }

        val sameDevice = previous.address == address
        val leftThresholdChanged = !sameDevice || previous.leftThresholdPercent != normalized.leftThresholdPercent
        val rightThresholdChanged = !sameDevice || previous.rightThresholdPercent != normalized.rightThresholdPercent
        val caseThresholdChanged = !sameDevice || previous.caseThresholdPercent != normalized.caseThresholdPercent

        val leftLevel = BatteryLevel.validOrNull(battery.left)
        val rightLevel = BatteryLevel.validOrNull(battery.right)
        val caseLevel = BatteryLevel.validOrNull(battery.case)

        val prevLeftAlerted = if (leftThresholdChanged || !normalized.leftEnabled) false else previous.leftAlerted
        val prevRightAlerted = if (rightThresholdChanged || !normalized.rightEnabled) false else previous.rightAlerted
        val prevCaseAlerted = if (caseThresholdChanged || !normalized.caseEnabled) false else previous.caseAlerted

        val leftLowNow = normalized.leftEnabled && !battery.chargingLeft &&
            leftLevel != null && leftLevel <= normalized.leftThresholdPercent
        val rightLowNow = normalized.rightEnabled && !battery.chargingRight &&
            rightLevel != null && rightLevel <= normalized.rightThresholdPercent
        val caseLowNow = normalized.caseEnabled && !battery.chargingCase &&
            caseLevel != null && caseLevel <= normalized.caseThresholdPercent

        // Keep the alerted flag while a component temporarily reports null (e.g. closed case)
        // so reopening the case at the same low percentage does not re-trigger the alert.
        val nextLeftAlerted = when {
            !normalized.leftEnabled || battery.chargingLeft -> false
            leftLevel == null -> prevLeftAlerted
            leftLevel <= normalized.leftThresholdPercent -> true
            else -> false
        }
        val nextRightAlerted = when {
            !normalized.rightEnabled || battery.chargingRight -> false
            rightLevel == null -> prevRightAlerted
            rightLevel <= normalized.rightThresholdPercent -> true
            else -> false
        }
        val nextCaseAlerted = when {
            !normalized.caseEnabled || battery.chargingCase -> false
            caseLevel == null -> prevCaseAlerted
            caseLevel <= normalized.caseThresholdPercent -> true
            else -> false
        }

        val nextLeftPercent = when {
            !nextLeftAlerted -> null
            leftLowNow -> leftLevel
            else -> previous.lastLeftPercent.takeIf { sameDevice }
        }
        val nextRightPercent = when {
            !nextRightAlerted -> null
            rightLowNow -> rightLevel
            else -> previous.lastRightPercent.takeIf { sameDevice }
        }
        val nextCasePercent = when {
            !nextCaseAlerted -> null
            caseLowNow -> caseLevel
            else -> previous.lastCasePercent.takeIf { sameDevice }
        }

        val newlyTriggered = (leftLowNow && !prevLeftAlerted) ||
            (rightLowNow && !prevRightAlerted) ||
            (caseLowNow && !prevCaseAlerted)

        val anyActiveAlert = nextLeftAlerted || nextRightAlerted || nextCaseAlerted
        val hadActiveAlert = prevLeftAlerted || prevRightAlerted || prevCaseAlerted

        val nextState = LowBatteryTrackerState(
            address = address,
            leftThresholdPercent = normalized.leftThresholdPercent,
            rightThresholdPercent = normalized.rightThresholdPercent,
            caseThresholdPercent = normalized.caseThresholdPercent,
            leftAlerted = nextLeftAlerted,
            rightAlerted = nextRightAlerted,
            caseAlerted = nextCaseAlerted,
            lastLeftPercent = nextLeftPercent,
            lastRightPercent = nextRightPercent,
            lastCasePercent = nextCasePercent,
        )

        return LowBatteryDecision(
            state = nextState,
            shouldNotify = newlyTriggered && anyActiveAlert,
            shouldCancel = !anyActiveAlert && (hadActiveAlert || previous.leftAlerted || previous.rightAlerted || previous.caseAlerted),
            leftPercent = nextLeftPercent,
            rightPercent = nextRightPercent,
            casePercent = nextCasePercent,
        )
    }
}
