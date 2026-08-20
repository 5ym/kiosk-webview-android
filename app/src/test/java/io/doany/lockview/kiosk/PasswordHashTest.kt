package io.doany.lockview.kiosk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** [PasswordHash] のローカルユニットテスト。 */
class PasswordHashTest {

    /** 正しいパスワードで照合できる。 */
    @Test
    fun verifiesCorrectPassword() {
        val stored = PasswordHash.hash("kiosk1234")
        assertTrue(PasswordHash.verify("kiosk1234", stored))
    }

    /** 違うパスワードは拒否する。 */
    @Test
    fun rejectsWrongPassword() {
        val stored = PasswordHash.hash("kiosk1234")
        assertFalse(PasswordHash.verify("kiosk1235", stored))
        assertFalse(PasswordHash.verify("KIOSK1234", stored))
        assertFalse(PasswordHash.verify("", stored))
    }

    /** ソルトが毎回変わるので、同じパスワードでも保存値は一致しない。 */
    @Test
    fun producesDifferentHashForSamePassword() {
        val first = PasswordHash.hash("kiosk1234")
        val second = PasswordHash.hash("kiosk1234")
        assertNotEquals(first, second)
        assertTrue(PasswordHash.verify("kiosk1234", first))
        assertTrue(PasswordHash.verify("kiosk1234", second))
    }

    /** 平文が保存値へそのまま含まれない。 */
    @Test
    fun doesNotStorePlainTextPassword() {
        val stored = PasswordHash.hash("kiosk1234")
        assertFalse(stored.contains("kiosk1234"))
    }

    /** 保存形式は「反復回数:ソルト:ハッシュ」。 */
    @Test
    fun usesExpectedStorageFormat() {
        val parts = PasswordHash.hash("kiosk1234").split(':')
        assertEquals(3, parts.size)
        assertTrue(parts[0].toInt() > 0)
        // 16バイトのソルトと32バイトの鍵を16進数で保存する。
        assertEquals(32, parts[1].length)
        assertEquals(64, parts[2].length)
        assertTrue(parts[1].all { it in "0123456789abcdef" })
        assertTrue(parts[2].all { it in "0123456789abcdef" })
    }

    /** 記号や日本語を含むパスワードも扱える。 */
    @Test
    fun supportsNonAsciiPassword() {
        val password = "パス word #1!"
        val stored = PasswordHash.hash(password)
        assertTrue(PasswordHash.verify(password, stored))
        assertFalse(PasswordHash.verify("パス word #1", stored))
    }

    /** 壊れた保存値を渡しても例外にせず false を返す。 */
    @Test
    fun rejectsMalformedStoredValue() {
        assertFalse(PasswordHash.verify("kiosk1234", ""))
        assertFalse(PasswordHash.verify("kiosk1234", "not-a-hash"))
        assertFalse(PasswordHash.verify("kiosk1234", "120000:abcd"))
        assertFalse(PasswordHash.verify("kiosk1234", "abc:abcd:abcd"))
        assertFalse(PasswordHash.verify("kiosk1234", "0:abcd:abcd"))
        assertFalse(PasswordHash.verify("kiosk1234", "-1:abcd:abcd"))
        assertFalse(PasswordHash.verify("kiosk1234", "120000:zzzz:abcd"))
        assertFalse(PasswordHash.verify("kiosk1234", "120000:abc:abcd"))
        assertFalse(PasswordHash.verify("kiosk1234", "120000::abcd"))
        assertFalse(PasswordHash.verify("kiosk1234", "120000:abcd:"))
    }

    /** 保存値のソルトを書き換えると照合に失敗する。 */
    @Test
    fun failsWhenStoredSaltIsTampered() {
        val parts = PasswordHash.hash("kiosk1234").split(':')
        val tamperedSalt = parts[1].replaceRange(0, 1, if (parts[1][0] == 'a') "b" else "a")
        assertFalse(PasswordHash.verify("kiosk1234", "${parts[0]}:$tamperedSalt:${parts[2]}"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyPasswordOnHash() {
        PasswordHash.hash("")
    }
}
