package dev.daco.sqp

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.KeyEvent
import android.view.WindowManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.daco.sqp.kiosk.KioskModeController
import dev.daco.sqp.kiosk.KioskSettings
import dev.daco.sqp.kiosk.SystemBarBlockerService
import dev.daco.sqp.kiosk.UnlockSequenceDetector

class MainActivity : AppCompatActivity() {

    private val kioskMode = KioskModeController.instance
    private val unlockSequence = UnlockSequenceDetector(UNLOCK_SEQUENCE)

    private lateinit var settings: KioskSettings
    private lateinit var webView: WebView

    private var unlockDialog: AlertDialog? = null

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                continueStartup()
            } else {
                Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show()
            }
        }

    private val openSetup =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // 保存された場合もキャンセルされた場合も、設定が揃っていればキオスクへ戻る。
            val homeUrl = settings.homeUrl
            if (settings.isConfigured && homeUrl != null) {
                startKiosk(homeUrl)
            } else {
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settings = KioskSettings(this)
        webView = findViewById(R.id.webview)
        configureWebView()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            continueStartup()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                // WebView 内でカメラを使うため、アプリに付与済みの権限をそのまま渡す。
                request.grant(request.resources)
            }
        }
        webView.settings.javaScriptEnabled = true
        // Vue 等のフレームワークで使うため DOM Storage を有効化する。
        webView.settings.domStorageEnabled = true
    }

    private fun continueStartup() {
        val uri = intent.data
        if (uri != null) {
            // 既定のブラウザとして呼び出された場合は設定を必要とせず、Lock Task も開始しない。
            enableKioskMode()
            webView.loadUrl(uri.toString())
            warnIfAccessibilityServiceDisabled()
            return
        }

        val homeUrl = settings.homeUrl
        if (!settings.isConfigured || homeUrl == null) {
            // 初回起動時は URL と解除用パスワードを設定してもらう。
            openSetup.launch(SetupActivity.createIntent(this))
            return
        }
        startKiosk(homeUrl)
    }

    private fun startKiosk(url: String) {
        enableKioskMode()
        startLockTaskIfNeeded()
        webView.loadUrl(url)
        warnIfAccessibilityServiceDisabled()
    }

    private fun warnIfAccessibilityServiceDisabled() {
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

    /** 音量ボタンの隠しシーケンスが成立したときの動作。 */
    private fun onUnlockSequenceDetected() {
        if (kioskMode.isEnabled) {
            showUnlockDialog()
        } else {
            // 解除済みの状態で同じ操作をするとキオスクモードへ復帰する。
            enableKioskMode()
            startLockTaskIfNeeded()
            Toast.makeText(this, R.string.kiosk_mode_enabled, Toast.LENGTH_SHORT).show()
        }
    }

    /** パスワードを入力させ、一致したときだけキオスクモードを解除する。 */
    private fun showUnlockDialog() {
        if (unlockDialog?.isShowing == true) {
            return
        }
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setHint(R.string.unlock_password_hint)
        }
        val padding = dpToPx(DIALOG_PADDING_DP)
        val container = FrameLayout(this).apply {
            setPadding(padding, padding, padding, padding)
            addView(input)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.unlock_dialog_title)
            .setView(container)
            .setPositiveButton(R.string.unlock_action, null)
            .setNeutralButton(R.string.unlock_open_setup_action, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.setCanceledOnTouchOutside(false)
        // 入力中のパスワードもスクリーンショットから守る。
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        dialog.setOnShowListener {
            // パスワードを間違えてもダイアログを閉じないよう、既定の動作を上書きする。
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                withVerifiedPassword(input) {
                    dialog.dismiss()
                    disableKioskMode()
                    Toast.makeText(this, R.string.kiosk_mode_disabled, Toast.LENGTH_LONG).show()
                }
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                withVerifiedPassword(input) {
                    dialog.dismiss()
                    disableKioskMode()
                    openSetup.launch(SetupActivity.createIntent(this))
                }
            }
        }
        dialog.setOnDismissListener { unlockDialog = null }
        unlockDialog = dialog
        dialog.show()
    }

    private fun withVerifiedPassword(input: EditText, action: () -> Unit) {
        if (settings.verifyPassword(input.text.toString())) {
            action()
        } else {
            input.text.clear()
            Toast.makeText(this, R.string.unlock_wrong_password, Toast.LENGTH_SHORT).show()
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

    private fun dpToPx(dp: Float): Int = TypedValue
        .applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
        .toInt()

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode in UNLOCK_SEQUENCE) {
            if (unlockSequence.onKeyDown(event.keyCode, event.eventTime)) {
                onUnlockSequenceDetected()
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

    override fun onDestroy() {
        unlockDialog?.dismiss()
        unlockDialog = null
        super.onDestroy()
    }

    companion object {

        private const val DIALOG_PADDING_DP = 24f

        /**
         * キオスクモードの解除操作に使う音量ボタンのシーケンス。
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
