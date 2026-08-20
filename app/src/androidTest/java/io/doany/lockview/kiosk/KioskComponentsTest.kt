package io.doany.lockview.kiosk

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.doany.lockview.MainActivity
import io.doany.lockview.SetupActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * キオスクモードに必要なコンポーネントがマニフェストに正しく登録されているかを、
 * 実機 / エミュレータ上で検証する。
 */
@RunWith(AndroidJUnit4::class)
class KioskComponentsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun usesExpectedApplicationId() {
        assertEquals("io.doany.lockview", context.packageName)
    }

    /** ユーザー補助サービスがシステムからバインドできる形で公開されている。 */
    @Suppress("DEPRECATION")
    @Test
    fun accessibilityServiceIsDeclaredForSystemBinding() {
        val component = ComponentName(context, SystemBarBlockerService::class.java)
        val serviceInfo = context.packageManager.getServiceInfo(component, PackageManager.GET_META_DATA)

        assertTrue("ユーザー補助サービスは exported である必要がある", serviceInfo.exported)
        assertEquals(
            Manifest.permission.BIND_ACCESSIBILITY_SERVICE,
            serviceInfo.permission
        )
        assertTrue(
            "accessibility-service の設定 XML が meta-data に必要",
            serviceInfo.metaData?.containsKey("android.accessibilityservice") == true
        )
    }

    /** ユーザー補助サービスとしてシステムに認識される intent-filter を持つ。 */
    @Test
    fun accessibilityServiceRespondsToAccessibilityIntent() {
        val intent = Intent("android.accessibilityservice.AccessibilityService")
        intent.setPackage(context.packageName)
        val resolved = context.packageManager.queryIntentServices(intent, 0)

        assertTrue(
            "SystemBarBlockerService が解決できない",
            resolved.any { it.serviceInfo.name == SystemBarBlockerService::class.java.name }
        )
    }

    /** ホームアプリ兼ブラウザとして呼び出せる。 */
    @Test
    fun mainActivityHandlesHomeAndBrowserIntents() {
        val activityName = MainActivity::class.java.name
        val packageManager = context.packageManager

        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .setPackage(context.packageName)
        assertTrue(
            "ホームアプリとして解決できない",
            packageManager.queryIntentActivities(home, 0).any { it.activityInfo.name == activityName }
        )

        val browser = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setPackage(context.packageName)
        assertTrue(
            "ブラウザとして解決できない",
            packageManager.queryIntentActivities(browser, 0).any { it.activityInfo.name == activityName }
        )
    }

    /** 有効判定がシステム設定の内容と一致する。 */
    @Test
    fun accessibilityServiceEnabledStateMatchesSystemSettings() {
        val component = ComponentName(context, SystemBarBlockerService::class.java)
        val enabledFromSettings = Settings.Secure
            .getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty()
            .split(':')
            .any { ComponentName.unflattenFromString(it) == component }

        assertEquals(enabledFromSettings, SystemBarBlockerService.isEnabled(context))
    }

    /** 設定画面がアプリ内からのみ起動できる形で登録されている。 */
    @Suppress("DEPRECATION")
    @Test
    fun setupActivityIsNotExported() {
        val component = ComponentName(context, SetupActivity::class.java)
        val activityInfo = context.packageManager.getActivityInfo(component, 0)

        assertFalse("設定画面は外部から起動できてはいけない", activityInfo.exported)
    }

    /** システムバーの高さは必ず正の値になる。 */
    @Test
    fun systemBarHeightsArePositive() {
        assertTrue(SystemBarMetrics.statusBarHeight(context) > 0)
        assertTrue(SystemBarMetrics.navigationBarHeight(context) > 0)
    }
}
