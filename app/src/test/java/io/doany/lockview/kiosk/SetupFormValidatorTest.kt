package io.doany.lockview.kiosk

import org.junit.Assert.assertEquals
import org.junit.Test

/** [SetupFormValidator] のローカルユニットテスト。 */
class SetupFormValidatorTest {

    private fun validate(
        url: String = "https://example.com",
        password: String = "1234",
        confirmation: String = "1234",
        passwordRequired: Boolean = true
    ) = SetupFormValidator.validate(url, password, confirmation, passwordRequired)

    /** 正しい入力では正規化した URL とパスワードが返る。 */
    @Test
    fun acceptsValidInput() {
        val result = validate(url = "example.com", password = "kiosk", confirmation = "kiosk")
        assertEquals(SetupFormResult.Valid("https://example.com", "kiosk"), result)
    }

    @Test
    fun rejectsInvalidUrl() {
        assertEquals(
            SetupFormResult.Invalid(SetupFormError.INVALID_URL),
            validate(url = "file:///sdcard/index.html")
        )
    }

    /** URL の検証はパスワードより先に行う。 */
    @Test
    fun reportsUrlErrorBeforePasswordError() {
        assertEquals(
            SetupFormResult.Invalid(SetupFormError.INVALID_URL),
            validate(url = "", password = "", confirmation = "")
        )
    }

    /** 初回設定ではパスワードが必須。 */
    @Test
    fun requiresPasswordOnFirstSetup() {
        assertEquals(
            SetupFormResult.Invalid(SetupFormError.PASSWORD_REQUIRED),
            validate(password = "", confirmation = "", passwordRequired = true)
        )
    }

    /** 設定済みならパスワード欄が空でも維持扱いになる。 */
    @Test
    fun keepsCurrentPasswordWhenLeftBlank() {
        assertEquals(
            SetupFormResult.Valid("https://example.com", null),
            validate(password = "", confirmation = "", passwordRequired = false)
        )
    }

    @Test
    fun rejectsShortPassword() {
        assertEquals(
            SetupFormResult.Invalid(SetupFormError.PASSWORD_TOO_SHORT),
            validate(password = "123", confirmation = "123")
        )
    }

    @Test
    fun rejectsMismatchedConfirmation() {
        assertEquals(
            SetupFormResult.Invalid(SetupFormError.PASSWORD_MISMATCH),
            validate(password = "kiosk", confirmation = "kiosk1")
        )
    }

    /** 確認欄だけ入力された場合も変更扱いとして検証する。 */
    @Test
    fun validatesWhenOnlyConfirmationIsFilled() {
        assertEquals(
            SetupFormResult.Invalid(SetupFormError.PASSWORD_TOO_SHORT),
            validate(password = "", confirmation = "kiosk", passwordRequired = false)
        )
    }

    /** パスワードの最小長は 4 文字。 */
    @Test
    fun acceptsPasswordAtMinimumLength() {
        val password = "a".repeat(SetupFormValidator.MIN_PASSWORD_LENGTH)
        assertEquals(
            SetupFormResult.Valid("https://example.com", password),
            validate(password = password, confirmation = password)
        )
    }
}
