package io.doany.lockview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import io.doany.lockview.kiosk.KioskSettings
import io.doany.lockview.kiosk.SetupFormError
import io.doany.lockview.kiosk.SetupFormResult
import io.doany.lockview.kiosk.SetupFormValidator

/**
 * 表示先 URL と解除用パスワードを設定する画面。
 *
 * 初回起動時(未設定のとき)と、キオスクモード解除時にパスワードを入力して開いたときに表示する。
 */
class SetupActivity : AppCompatActivity() {

    private lateinit var settings: KioskSettings

    private lateinit var urlInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var passwordConfirmInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // パスワード入力画面なのでスクリーンショットを禁止する。
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_setup)

        settings = KioskSettings(this)

        urlInput = findViewById(R.id.url_input)
        passwordInput = findViewById(R.id.password_input)
        passwordConfirmInput = findViewById(R.id.password_confirm_input)

        urlInput.setText(settings.homeUrl.orEmpty())
        findViewById<TextView>(R.id.password_hint).setText(
            if (settings.isConfigured) R.string.setup_password_keep_hint else R.string.setup_password_required_hint
        )
        findViewById<Button>(R.id.save_button).setOnClickListener { save() }

        // 未設定のまま戻られるとキオスク端末として動作できないため、戻るを無効化する。
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(!settings.isConfigured) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@SetupActivity, R.string.setup_required, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun save() {
        val result = SetupFormValidator.validate(
            url = urlInput.text.toString(),
            password = passwordInput.text.toString(),
            confirmation = passwordConfirmInput.text.toString(),
            passwordRequired = !settings.isConfigured
        )
        when (result) {
            is SetupFormResult.Invalid -> {
                Toast.makeText(this, messageFor(result.error), Toast.LENGTH_LONG).show()
            }

            is SetupFormResult.Valid -> {
                settings.save(result.url, result.password)
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun messageFor(error: SetupFormError): Int = when (error) {
        SetupFormError.INVALID_URL -> R.string.setup_error_invalid_url
        SetupFormError.PASSWORD_REQUIRED -> R.string.setup_error_password_required
        SetupFormError.PASSWORD_TOO_SHORT -> R.string.setup_error_password_too_short
        SetupFormError.PASSWORD_MISMATCH -> R.string.setup_error_password_mismatch
    }

    companion object {

        fun createIntent(context: Context): Intent = Intent(context, SetupActivity::class.java)
    }
}
