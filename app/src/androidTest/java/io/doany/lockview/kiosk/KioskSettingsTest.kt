package io.doany.lockview.kiosk

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** [KioskSettings] を実際の SharedPreferences 上で検証する。 */
@RunWith(AndroidJUnit4::class)
class KioskSettingsTest {

    private lateinit var context: Context
    private lateinit var settings: KioskSettings

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        settings = KioskSettings(context)
        settings.clear()
    }

    @After
    fun tearDown() {
        settings.clear()
    }

    /** 何も保存していない状態では未設定として扱う。 */
    @Test
    fun isNotConfiguredInitially() {
        assertFalse(settings.isConfigured)
        assertNull(settings.homeUrl)
        assertFalse(settings.verifyPassword("kiosk1234"))
    }

    /** URL とパスワードを保存すると設定済みになる。 */
    @Test
    fun storesUrlAndPassword() {
        settings.save("https://example.com", "kiosk1234")

        assertTrue(settings.isConfigured)
        assertEquals("https://example.com", settings.homeUrl)
        assertTrue(settings.verifyPassword("kiosk1234"))
        assertFalse(settings.verifyPassword("kiosk1235"))
    }

    /** 別インスタンスからも同じ設定を読める。 */
    @Test
    fun persistsAcrossInstances() {
        settings.save("https://example.com", "kiosk1234")

        val reloaded = KioskSettings(context)
        assertEquals("https://example.com", reloaded.homeUrl)
        assertTrue(reloaded.verifyPassword("kiosk1234"))
    }

    /** パスワードに null を渡すと現在のパスワードを維持したまま URL だけ変わる。 */
    @Test
    fun keepsPasswordWhenNullIsGiven() {
        settings.save("https://example.com", "kiosk1234")
        settings.save("https://example.org", null)

        assertEquals("https://example.org", settings.homeUrl)
        assertTrue(settings.verifyPassword("kiosk1234"))
    }

    /** パスワードを変更すると古いパスワードでは解除できない。 */
    @Test
    fun replacesPassword() {
        settings.save("https://example.com", "kiosk1234")
        settings.save("https://example.com", "newpassword")

        assertFalse(settings.verifyPassword("kiosk1234"))
        assertTrue(settings.verifyPassword("newpassword"))
    }

    /** clear() ですべて消える。 */
    @Test
    fun clearRemovesEverything() {
        settings.save("https://example.com", "kiosk1234")
        settings.clear()

        assertFalse(settings.isConfigured)
        assertNull(settings.homeUrl)
        assertFalse(settings.verifyPassword("kiosk1234"))
    }
}
