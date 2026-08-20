package dev.daco.sqp.kiosk

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager

/**
 * `TYPE_ACCESSIBILITY_OVERLAY` のウィンドウをステータスバーとナビゲーションバーの
 * 位置に重ねて、そこへのタッチを飲み込むユーザー補助サービス。
 *
 * ProfileOwner / DeviceOwner なしでステータスバーの引き下ろしを塞ぐための実装で、
 * ユーザーが設定 > ユーザー補助からこのサービスを有効にすると動作する。
 * オーバーレイの表示は [KioskModeController] の状態に追従する。
 */
class SystemBarBlockerService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val kioskMode = KioskModeController.instance

    private var windowManager: WindowManager? = null
    private var statusBarOverlay: View? = null
    private var navigationBarOverlay: View? = null

    private val kioskModeListener = KioskModeController.Listener { enabled ->
        mainHandler.post { if (enabled) showOverlays() else hideOverlays() }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WindowManager::class.java)
        // 登録時に現在の状態で通知されるので、ここでオーバーレイの初期状態も決まる。
        kioskMode.addListener(kioskModeListener)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // オーバーレイを出すことだけが目的なのでイベントは使わない。
    }

    override fun onInterrupt() {
        // 読み上げなどを行わないので何もしない。
    }

    override fun onUnbind(intent: Intent?): Boolean {
        kioskMode.removeListener(kioskModeListener)
        hideOverlays()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        kioskMode.removeListener(kioskModeListener)
        hideOverlays()
        super.onDestroy()
    }

    private fun showOverlays() {
        val manager = windowManager ?: return
        if (statusBarOverlay == null) {
            val height = SystemBarMetrics.statusBarHeight(this)
            statusBarOverlay = addOverlay(manager, Gravity.TOP, height)
        }
        if (navigationBarOverlay == null) {
            val height = SystemBarMetrics.navigationBarHeight(this)
            navigationBarOverlay = addOverlay(manager, Gravity.BOTTOM, height)
        }
    }

    private fun hideOverlays() {
        val manager = windowManager ?: return
        statusBarOverlay = removeOverlay(manager, statusBarOverlay)
        navigationBarOverlay = removeOverlay(manager, navigationBarOverlay)
    }

    private fun addOverlay(manager: WindowManager, gravity: Int, height: Int): View? {
        val overlay = TouchBlockingView(this)
        overlay.setBackgroundColor(Color.TRANSPARENT)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = gravity or Gravity.START
        return try {
            manager.addView(overlay, params)
            overlay
        } catch (e: WindowManager.BadTokenException) {
            null
        } catch (e: IllegalStateException) {
            null
        }
    }

    private fun removeOverlay(manager: WindowManager, overlay: View?): View? {
        if (overlay != null) {
            try {
                manager.removeView(overlay)
            } catch (e: IllegalArgumentException) {
                // すでに取り外されている場合は無視してよい。
            }
        }
        return null
    }

    /** タッチをすべて消費するだけのビュー。 */
    private class TouchBlockingView(context: Context) : View(context) {

        override fun onTouchEvent(event: MotionEvent): Boolean = true

        override fun performClick(): Boolean {
            super.performClick()
            return true
        }
    }

    companion object {

        /** このユーザー補助サービスが有効になっているか。 */
        fun isEnabled(context: Context): Boolean {
            val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
            val expected = ComponentName(context, SystemBarBlockerService::class.java)
            return manager
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { ComponentName.unflattenFromString(it.id) == expected }
        }
    }
}
