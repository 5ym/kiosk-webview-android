package dev.daco.sqp.kiosk

import android.content.Context

/** 表示先 URL と解除用パスワードの保存先。 */
class KioskSettings(context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** 起動時に表示する URL。未設定なら null。 */
    val homeUrl: String?
        get() = preferences.getString(KEY_HOME_URL, null)

    private val passwordHash: String?
        get() = preferences.getString(KEY_PASSWORD_HASH, null)

    /** URL とパスワードの両方が保存済みか。 */
    val isConfigured: Boolean
        get() = !homeUrl.isNullOrEmpty() && !passwordHash.isNullOrEmpty()

    /**
     * 設定を保存する。
     *
     * @param password 新しいパスワード。null なら現在のパスワードを変更しない。
     */
    fun save(url: String, password: String?) {
        preferences.edit().apply {
            putString(KEY_HOME_URL, url)
            if (password != null) {
                putString(KEY_PASSWORD_HASH, PasswordHash.hash(password))
            }
        }.apply()
    }

    /** 入力されたパスワードが保存済みのものと一致するか。 */
    fun verifyPassword(input: String): Boolean {
        val stored = passwordHash ?: return false
        return PasswordHash.verify(input, stored)
    }

    /** 設定を消去する。 */
    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "kiosk_settings"
        private const val KEY_HOME_URL = "home_url"
        private const val KEY_PASSWORD_HASH = "password_hash"
    }
}
