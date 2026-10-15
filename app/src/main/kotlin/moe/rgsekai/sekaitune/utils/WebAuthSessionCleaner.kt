/*
 * Sekai Tune (2026)
 * © Sekai Tune - github.com/rgsekai/sekai-tune
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rgsekai.sekaitune.utils

import android.content.Context
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

fun clearPlaybackWebAuthSession(context: Context) {
    clearWebAuthStorage(context)
    val cookieManager = CookieManager.getInstance()
    cookieManager.removeSessionCookies(null)
    cookieManager.removeAllCookies(null)
    cookieManager.flush()
}

suspend fun clearWebAuthSession(context: Context) {
    withContext(Dispatchers.Main.immediate) {
        clearWebAuthStorage(context)
        val cookieManager = CookieManager.getInstance()
        suspendCancellableCoroutine<Unit> { continuation ->
            cookieManager.removeSessionCookies {
                cookieManager.removeAllCookies {
                    cookieManager.flush()
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
            }
        }
    }
}

fun resetAuthWebViewSession(
    context: Context,
    webView: WebView,
    clearCookies: Boolean = true,
    onReady: () -> Unit,
) {
    webView.stopLoading()
    webView.clearHistory()
    webView.clearFormData()
    if (clearCookies) {
        webView.clearCache(true)
        clearWebAuthStorage(context)
    }

    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(webView, true)

    fun invokeOnReady() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onReady()
        } else {
            webView.post { onReady() }
        }
    }

    if (!clearCookies) {
        invokeOnReady()
        return
    }

    var isReadyCalled = false
    fun dispatchReady() {
        if (!isReadyCalled) {
            isReadyCalled = true
            invokeOnReady()
        }
    }

    cookieManager.removeSessionCookies {
        cookieManager.removeAllCookies {
            cookieManager.flush()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
            dispatchReady()
        }
    }

    // Safety fallback: if cookie removal callback is delayed or swallowed by Chromium,
    // dispatch ready after 350ms so WebView never hangs with a blank screen.
    webView.postDelayed({ dispatchReady() }, 350)
}

private fun clearWebAuthStorage(context: Context) {
    val appContext = context.applicationContext
    WebStorage.getInstance().deleteAllData()
    WebViewDatabase.getInstance(appContext).apply {
        clearFormData()
        clearHttpAuthUsernamePassword()
        clearUsernamePassword()
    }
}




