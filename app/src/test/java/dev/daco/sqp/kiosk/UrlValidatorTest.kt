package dev.daco.sqp.kiosk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** [UrlValidator] のローカルユニットテスト。 */
class UrlValidatorTest {

    @Test
    fun keepsValidHttpAndHttpsUrls() {
        assertEquals("https://sqp.sub.daco.dev", UrlValidator.normalize("https://sqp.sub.daco.dev"))
        assertEquals("http://192.168.0.10:8080/app", UrlValidator.normalize("http://192.168.0.10:8080/app"))
        assertEquals(
            "https://example.com/path?a=1&b=2#top",
            UrlValidator.normalize("https://example.com/path?a=1&b=2#top")
        )
    }

    /** スキームが省略された場合は https を補う。 */
    @Test
    fun addsHttpsWhenSchemeIsMissing() {
        assertEquals("https://example.com", UrlValidator.normalize("example.com"))
        assertEquals("https://example.com/path", UrlValidator.normalize("example.com/path"))
    }

    /** 前後の空白は無視する。 */
    @Test
    fun trimsSurroundingWhitespace() {
        assertEquals("https://example.com", UrlValidator.normalize("  https://example.com  "))
        assertEquals("https://example.com", UrlValidator.normalize("\texample.com\n"))
    }

    @Test
    fun rejectsBlankInput() {
        assertNull(UrlValidator.normalize(""))
        assertNull(UrlValidator.normalize("   "))
    }

    /** http/https 以外のスキームは受け付けない。 */
    @Test
    fun rejectsNonHttpSchemes() {
        assertNull(UrlValidator.normalize("javascript://alert(1)"))
        assertNull(UrlValidator.normalize("file:///sdcard/index.html"))
        assertNull(UrlValidator.normalize("ftp://example.com"))
        assertNull(UrlValidator.normalize("content://media/external/images"))
    }

    /** ホスト名が無いものは受け付けない。 */
    @Test
    fun rejectsUrlsWithoutHost() {
        assertNull(UrlValidator.normalize("https://"))
        assertNull(UrlValidator.normalize("http:///path"))
    }

    /** 構文として壊れているものは受け付けない。 */
    @Test
    fun rejectsMalformedUrls() {
        assertNull(UrlValidator.normalize("https://exa mple.com"))
        assertNull(UrlValidator.normalize("https://[example"))
    }
}
