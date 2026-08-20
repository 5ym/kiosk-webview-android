package dev.daco.sqp.kiosk

import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.view.WindowInsets
import android.view.WindowManager

/**
 * ステータスバー / ナビゲーションバーの高さを求めるヘルパ。
 *
 * オーバーレイを貼るのは [android.accessibilityservice.AccessibilityService] という
 * UI を持たないコンテキストなので、API 30 以降は表示用の window context を作ってから
 * インセットを問い合わせ、取得できない環境ではフレームワークのリソース値へフォールバックする。
 */
object SystemBarMetrics {

    private const val FALLBACK_STATUS_BAR_HEIGHT_DP = 24f
    private const val FALLBACK_NAVIGATION_BAR_HEIGHT_DP = 48f

    fun statusBarHeight(context: Context): Int {
        val fromInsets = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insets(context)?.top ?: 0
        } else {
            0
        }
        if (fromInsets > 0) {
            return fromInsets
        }
        val fromResources = platformDimension(context, "status_bar_height")
        return if (fromResources > 0) fromResources else dpToPx(context, FALLBACK_STATUS_BAR_HEIGHT_DP)
    }

    fun navigationBarHeight(context: Context): Int {
        val fromInsets = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insets(context)?.bottom ?: 0
        } else {
            0
        }
        if (fromInsets > 0) {
            return fromInsets
        }
        val fromResources = platformDimension(context, "navigation_bar_height")
        return if (fromResources > 0) fromResources else dpToPx(context, FALLBACK_NAVIGATION_BAR_HEIGHT_DP)
    }

    /**
     * システムバーのインセット。取得できない場合は null。
     *
     * ジェスチャーナビゲーションの端末ではナビゲーションバーのインセットが
     * ハンドル分しかないため、フォールバックより小さい値になることがある。
     */
    private fun insets(context: Context): android.graphics.Insets? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return null
        }
        return try {
            val windowContext = context.createWindowContext(
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                null
            )
            val windowManager = windowContext.getSystemService(WindowManager::class.java) ?: return null
            windowManager.currentWindowMetrics.windowInsets
                .getInsets(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } catch (e: RuntimeException) {
            // 一部の端末では UI 以外のコンテキストからのメトリクス取得が失敗する。
            null
        }
    }

    private fun platformDimension(context: Context, name: String): Int {
        @Suppress("DiscouragedApi")
        val id = context.resources.getIdentifier(name, "dimen", "android")
        return if (id > 0) context.resources.getDimensionPixelSize(id) else 0
    }

    private fun dpToPx(context: Context, dp: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()
}
