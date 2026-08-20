package io.doany.lockview.kiosk

import java.net.URI
import java.net.URISyntaxException

/**
 * 設定画面で入力された表示先 URL の検証。
 *
 * Android に依存しないので、ローカルユニットテストで検証できる。
 */
object UrlValidator {

    /**
     * 入力を WebView へ渡せる形へ整える。
     * スキームが省略された場合は https を補う。
     *
     * @return 正規化した URL。http/https 以外やホスト名が無い場合は null。
     */
    fun normalize(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return null
        }
        val candidate = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        val uri = try {
            URI(candidate)
        } catch (e: URISyntaxException) {
            return null
        }
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") {
            return null
        }
        if (uri.host.isNullOrEmpty()) {
            return null
        }
        return candidate
    }
}
