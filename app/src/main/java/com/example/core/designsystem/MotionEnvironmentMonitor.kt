package com.example.core.designsystem

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.database.AppUserPreferences
import com.example.domain.models.AnimationMode
import com.example.domain.models.MotionSettingsState

val LocalMotionSettings = staticCompositionLocalOf { MotionSettingsState() }

fun readSystemReducedMotion(context: Context): Boolean {
    return runCatching {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f,
        )
        scale <= 0.05f
    }.getOrDefault(false)
}

fun readLowBatteryOrPowerSave(context: Context, batteryIntent: Intent? = null): Boolean {
    val powerSave = runCatching {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.isPowerSaveMode == true
    }.getOrDefault(false)
    if (powerSave) return true

    val intent = batteryIntent ?: runCatching {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }.getOrNull() ?: return false

    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL

    if (level < 0 || scale <= 0 || isCharging) return false
    val pct = (level * 100f) / scale.toFloat()
    return pct <= 15f
}

@Composable
fun rememberMotionEnvironmentState(
    preferences: AppUserPreferences,
    themeAnimationMode: AnimationMode,
    onAppForegroundChanged: (Boolean) -> Unit = {},
): MotionSettingsState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isAppInForeground by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    var systemReducedMotion by remember(context) { mutableStateOf(readSystemReducedMotion(context)) }
    var isLowBatteryOrPowerSave by remember(context) { mutableStateOf(readLowBatteryOrPowerSave(context)) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    isAppInForeground = true
                    systemReducedMotion = readSystemReducedMotion(context)
                    isLowBatteryOrPowerSave = readLowBatteryOrPowerSave(context)
                    onAppForegroundChanged(true)
                }
                Lifecycle.Event.ON_STOP -> {
                    isAppInForeground = false
                    onAppForegroundChanged(false)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    isLowBatteryOrPowerSave = readLowBatteryOrPowerSave(context, intent)
                } else {
                    isLowBatteryOrPowerSave = readLowBatteryOrPowerSave(context)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        runCatching { context.registerReceiver(receiver, filter) }

        val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                systemReducedMotion = readSystemReducedMotion(context)
            }
        }
        runCatching {
            context.contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                false,
                contentObserver,
            )
        }

        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
            runCatching { context.contentResolver.unregisterContentObserver(contentObserver) }
        }
    }

    LaunchedEffect(isAppInForeground) {
        onAppForegroundChanged(isAppInForeground)
    }

    val themeRequestsReduction = themeAnimationMode == AnimationMode.REDUCED ||
        themeAnimationMode == AnimationMode.BATTERY_SAVER

    return remember(
        preferences.playerVisualStyle,
        preferences.visualizerAnimationIntensity,
        preferences.animationsDisabled,
        preferences.reduceMotion,
        preferences.batterySaverAnimations,
        preferences.reduceEffectsOnLowBattery,
        systemReducedMotion,
        isLowBatteryOrPowerSave,
        isAppInForeground,
        themeRequestsReduction,
    ) {
        MotionSettingsState(
            playerVisualStyle = preferences.playerVisualStyle,
            effectIntensity = preferences.visualizerAnimationIntensity,
            animationsDisabled = preferences.animationsDisabled,
            reduceMotion = preferences.reduceMotion || preferences.batterySaverAnimations || themeRequestsReduction,
            reduceEffectsOnLowBattery = preferences.reduceEffectsOnLowBattery,
            systemReducedMotion = systemReducedMotion,
            isLowBatteryOrPowerSave = isLowBatteryOrPowerSave,
            isAppInForeground = isAppInForeground,
        )
    }
}
