package dev.daco.sqp.kiosk

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 解除用パスワードのハッシュ化と照合。
 *
 * 端末を分解して SharedPreferences を読まれても平文が漏れないよう、
 * ソルト付きの PBKDF2 で保存する。保存形式は `反復回数:ソルト(hex):ハッシュ(hex)`。
 *
 * Android に依存しないので、ローカルユニットテストで検証できる。
 */
object PasswordHash {

    /** minSdk 24 で必ず使えるのは SHA1 版のみ。 */
    private const val ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16

    private val random = SecureRandom()

    /**
     * パスワードを保存用の文字列へ変換する。呼び出しごとにソルトが変わるため、
     * 同じパスワードでも結果は一致しない。
     */
    fun hash(password: String): String {
        require(password.isNotEmpty()) { "password must not be empty" }
        val salt = ByteArray(SALT_LENGTH_BYTES)
        random.nextBytes(salt)
        val key = derive(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        return "$ITERATIONS:${salt.toHex()}:${key.toHex()}"
    }

    /**
     * 入力されたパスワードが [stored] と一致するか。
     * [stored] が壊れている場合は false を返すだけで例外は投げない。
     */
    fun verify(password: String, stored: String): Boolean {
        if (password.isEmpty()) {
            return false
        }
        val parts = stored.split(':')
        if (parts.size != 3) {
            return false
        }
        val iterations = parts[0].toIntOrNull() ?: return false
        if (iterations <= 0) {
            return false
        }
        val salt = parts[1].hexToBytesOrNull() ?: return false
        val expected = parts[2].hexToBytesOrNull() ?: return false
        if (salt.isEmpty() || expected.isEmpty()) {
            return false
        }
        val actual = derive(password, salt, iterations, expected.size * Byte.SIZE_BITS)
        // タイミング攻撃を避けるため定数時間で比較する。
        return MessageDigest.isEqual(expected, actual)
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int, keyLengthBits: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLengthBits)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHex(): String {
        val builder = StringBuilder(size * 2)
        forEach { byte -> builder.append(HEX_DIGITS[(byte.toInt() shr 4) and 0xF]).append(HEX_DIGITS[byte.toInt() and 0xF]) }
        return builder.toString()
    }

    private fun String.hexToBytesOrNull(): ByteArray? {
        if (isEmpty() || length % 2 != 0) {
            return null
        }
        val bytes = ByteArray(length / 2)
        for (index in bytes.indices) {
            val high = Character.digit(this[index * 2], 16)
            val low = Character.digit(this[index * 2 + 1], 16)
            if (high < 0 || low < 0) {
                return null
            }
            bytes[index] = ((high shl 4) or low).toByte()
        }
        return bytes
    }

    private const val HEX_DIGITS = "0123456789abcdef"
}
