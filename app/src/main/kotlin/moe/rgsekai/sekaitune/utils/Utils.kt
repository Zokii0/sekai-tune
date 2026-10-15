/*
 * Sekai Tune (2026)
 * © Sekai Tune - github.com/rgsekai/sekai-tune
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rgsekai.sekaitune.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

import android.content.pm.PackageManager

fun reportException(throwable: Throwable) {
    throwable.printStackTrace()
}

/**
 * Detects whether the device is an Android TV / Google TV.
 */
fun Context.isTvDevice(): Boolean {
    val isTelevisionUiMode =
        (resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
            Configuration.UI_MODE_TYPE_TELEVISION
    return isTelevisionUiMode ||
        packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
        packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
}

@Suppress("DEPRECATION")
fun setAppLocale(
    context: Context,
    locale: Locale,
) {
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}




