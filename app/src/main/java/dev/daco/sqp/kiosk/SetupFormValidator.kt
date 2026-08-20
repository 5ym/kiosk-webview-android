package dev.daco.sqp.kiosk

/** 設定画面の入力エラー。 */
enum class SetupFormError {
    INVALID_URL,
    PASSWORD_REQUIRED,
    PASSWORD_TOO_SHORT,
    PASSWORD_MISMATCH
}

/** 設定画面の入力検証結果。 */
sealed class SetupFormResult {

    /**
     * 入力に問題がない状態。
     *
     * @param password 新しいパスワード。null なら現在のパスワードを変更しない。
     */
    data class Valid(val url: String, val password: String?) : SetupFormResult()

    data class Invalid(val error: SetupFormError) : SetupFormResult()
}

/**
 * 設定画面の入力検証。
 *
 * Android に依存しないので、ローカルユニットテストで検証できる。
 */
object SetupFormValidator {

    const val MIN_PASSWORD_LENGTH = 4

    /**
     * @param passwordRequired 初回設定など、パスワードの入力を必須にするか。
     *   false の場合、パスワード欄が空なら現在のパスワードを維持する。
     */
    fun validate(
        url: String,
        password: String,
        confirmation: String,
        passwordRequired: Boolean
    ): SetupFormResult {
        val normalizedUrl = UrlValidator.normalize(url)
            ?: return SetupFormResult.Invalid(SetupFormError.INVALID_URL)

        if (password.isEmpty() && confirmation.isEmpty()) {
            return if (passwordRequired) {
                SetupFormResult.Invalid(SetupFormError.PASSWORD_REQUIRED)
            } else {
                SetupFormResult.Valid(normalizedUrl, null)
            }
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return SetupFormResult.Invalid(SetupFormError.PASSWORD_TOO_SHORT)
        }
        if (password != confirmation) {
            return SetupFormResult.Invalid(SetupFormError.PASSWORD_MISMATCH)
        }
        return SetupFormResult.Valid(normalizedUrl, password)
    }
}
