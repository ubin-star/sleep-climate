package com.sleepclimate.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.sleepclimate.app.R
import com.sleepclimate.app.data.vehicle.HvacMode
import com.sleepclimate.app.domain.model.AlertType
import com.sleepclimate.app.domain.model.ControlStatus
import com.sleepclimate.app.domain.model.VehicleState
import com.sleepclimate.app.domain.model.VehicleSupportLevel
import com.sleepclimate.app.ui.theme.SC

@StringRes
fun alertLabel(type: AlertType): Int = when (type) {
    AlertType.VEHICLE_DISCONNECTED -> R.string.alert_label_vehicle_disconnected
    AlertType.DATA_STALE           -> R.string.alert_label_data_stale
    AlertType.COMMAND_REJECTED     -> R.string.alert_label_command_rejected
    AlertType.COMMAND_FAILED       -> R.string.alert_label_command_failed
    AlertType.BATTERY_LOW          -> R.string.alert_label_battery_low
    AlertType.FUEL_LOW             -> R.string.alert_label_fuel_low
    AlertType.TEMP_OUT_OF_RANGE    -> R.string.alert_label_temp_out_of_range
    AlertType.NOT_PARKED           -> R.string.alert_label_not_parked
    AlertType.SESSION_ENDING_SOON  -> R.string.alert_label_session_ending_soon
    AlertType.PET_SAFETY_REMINDER  -> R.string.alert_label_pet_safety_reminder
    AlertType.AUTO_CONTROL_STOPPED -> R.string.alert_label_auto_control_stopped
}

fun alertIcon(type: AlertType): String = when (type) {
    AlertType.VEHICLE_DISCONNECTED -> "🔌"
    AlertType.DATA_STALE           -> "⏱"
    AlertType.COMMAND_REJECTED     -> "🚫"
    AlertType.COMMAND_FAILED       -> "❌"
    AlertType.BATTERY_LOW          -> "🔋"
    AlertType.FUEL_LOW             -> "⛽"
    AlertType.TEMP_OUT_OF_RANGE    -> "🌡"
    AlertType.NOT_PARKED           -> "🚗"
    AlertType.SESSION_ENDING_SOON  -> "⏰"
    AlertType.PET_SAFETY_REMINDER  -> "🐾"
    AlertType.AUTO_CONTROL_STOPPED -> "🛑"
}

fun alertColor(type: AlertType): Color = when (type) {
    AlertType.SESSION_ENDING_SOON, AlertType.PET_SAFETY_REMINDER -> SC.Accent
    AlertType.TEMP_OUT_OF_RANGE, AlertType.DATA_STALE -> SC.Warn
    else -> SC.Danger
}

@StringRes
fun hvacLabel(state: VehicleState): Int = when {
    state.isHvacOn == null  -> R.string.hvac_mode_unknown
    state.isHvacOn == false -> R.string.hvac_mode_off
    else -> when (state.hvacMode) {
        HvacMode.COOLING -> R.string.hvac_mode_cooling
        HvacMode.HEATING -> R.string.hvac_mode_heating
        HvacMode.AUTO    -> R.string.hvac_mode_auto
        else             -> R.string.hvac_mode_on
    }
}

@StringRes
fun controlActionLabel(mode: HvacMode): Int = when (mode) {
    HvacMode.COOLING -> R.string.control_action_cooling
    HvacMode.HEATING -> R.string.control_action_heating
    HvacMode.OFF     -> R.string.control_action_off
    HvacMode.AUTO    -> R.string.control_action_auto
}

@StringRes
fun controlStatusLabel(status: ControlStatus): Int = when (status) {
    ControlStatus.PENDING       -> R.string.control_status_pending
    ControlStatus.CONFIRMED     -> R.string.control_status_confirmed
    ControlStatus.REJECTED      -> R.string.control_status_rejected
    ControlStatus.TIMED_OUT     -> R.string.control_status_timed_out
    ControlStatus.NOT_SUPPORTED -> R.string.control_status_not_supported
}

@StringRes
fun supportLabel(level: VehicleSupportLevel): Int = when (level) {
    VehicleSupportLevel.FULL      -> R.string.badge_full_control
    VehicleSupportLevel.READ_ONLY -> R.string.badge_read_only
    VehicleSupportLevel.NONE      -> R.string.badge_no_support
    VehicleSupportLevel.UNKNOWN   -> R.string.status_checking
}

@Composable
fun energyText(state: VehicleState): String = when {
    state.batteryPercent != null -> stringResource(R.string.battery_format, state.batteryPercent)
    state.fuelPercent != null    -> stringResource(R.string.fuel_format, state.fuelPercent)
    else                         -> stringResource(R.string.energy_unavailable)
}
