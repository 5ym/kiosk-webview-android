package dev.daco.sqp

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.daco.sqp.kiosk.KioskModeController
import dev.daco.sqp.kiosk.SystemBarBlockerService
import dev.daco.sqp.kiosk.UnlockSequenceDetector

class MainActivity : AppCompatActivity() {

    private val kioskMode = KioskModeController.instance
    private val unlockSequence = UnlockSequenceDetector(UNLOCK_SEQUENCE)

    private lateinit var webView: WebView

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startApp()
            } else {
                Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startApp()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun startApp() {
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                // WebView 内でカメラを使うため、アプリに付与済みの権限をそのまま渡す。
                request.grant(request.resources)
            }
        }
        webView.settings.javaScriptEnabled = true
        // Vue 等のフレームワークで使うため DOM Storage を有効化する。
        webView.settings.domStorageEnabled = true

        enableKioskMode()

        val uri = intent.data
        if (uri != null) {
            // 既定のブラウザとして呼び出された場合は Lock Task を開始しない。
            webView.loadUrl(uri.toString())
        } else {
            startLockTaskIfNeeded()
            webView.loadUrl(HOME_URL)
        }

        if (!SystemBarBlockerService.isEnabled(this)) {
            Toast.makeText(this, R.string.accessibility_service_hint, Toast.LENGTH_LONG).show()
        }
    }

    private fun enableKioskMode() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_SECURE
        )
        hideSystemUI()
        // ユーザー補助サービスにステータスバー/ナビゲーションバーの封鎖を依頼する。
        kioskMode.setEnabled(true)
    }

    private fun disableKioskMode() {
        kioskMode.setEnabled(false)
        stopLockTaskIfNeeded()
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_SECURE
        )
        showSystemUI()
    }

    /** 音量ボタンの隠しシーケンスでキオスクモードを切り替える。 */
    private fun toggleKioskMode() {
        if (kioskMode.isEnabled) {
            disableKioskMode()
            Toast.makeText(this, R.string.kiosk_mode_disabled, Toast.LENGTH_LONG).show()
        } else {
            enableKioskMode()
            startLockTaskIfNeeded()
            Toast.makeText(this, R.string.kiosk_mode_enabled, Toast.LENGTH_SHORT).show()
        }
    }

    private fun startLockTaskIfNeeded() {
        if (!isInLockTaskMode) {
            startLockTask()
        }
    }

    private fun stopLockTaskIfNeeded() {
        if (isInLockTaskMode) {
            stopLockTask()
        }
    }

    private val isInLockTaskMode: Boolean
        get() {
            val activityManager = getSystemService(ActivityManager::class.java) ?: return false
            return activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
        }

    private fun hideSystemUI() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun showSystemUI() {
        WindowCompat.getInsetsController(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode in UNLOCK_SEQUENCE) {
            if (unlockSequence.onKeyDown(event.keyCode, event.eventTime)) {
                toggleKioskMode()
                return true
            }
        }
        // キオスクモード中は音量ボタンとバックキーを含むすべてのキー入力を無効化する。
        return if (kioskMode.isEnabled) true else super.dispatchKeyEvent(event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (kioskMode.isEnabled) {
            hideSystemUI()
        }
    }

    companion object {

        private const val HOME_URL = "https://sqp.sub.daco.dev"

        /**
         * キオスクモードの切り替えに使う音量ボタンのシーケンス。
         * [UnlockSequenceDetector.DEFAULT_TIMEOUT_MILLIS] 以内に入力する必要がある。
         */
        val UNLOCK_SEQUENCE = listOf(
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP
        )
    }
}
