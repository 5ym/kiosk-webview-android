package io.doany.lockview.kiosk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** [KioskModeController] のローカルユニットテスト。 */
class KioskModeControllerTest {

    private val controller = KioskModeController()
    private val received = mutableListOf<Boolean>()
    private val listener = KioskModeController.Listener { enabled -> received.add(enabled) }

    /** 初期状態は無効。 */
    @Test
    fun isDisabledByDefault() {
        assertFalse(controller.isEnabled)
    }

    /** 登録直後に現在の状態が 1 度通知される。 */
    @Test
    fun notifiesCurrentStateOnRegistration() {
        controller.setEnabled(true)
        controller.addListener(listener)
        assertEquals(listOf(true), received)
    }

    /** 状態が変わるとリスナへ通知される。 */
    @Test
    fun notifiesListenerOnChange() {
        controller.addListener(listener)
        assertTrue(controller.setEnabled(true))
        assertTrue(controller.setEnabled(false))
        assertEquals(listOf(false, true, false), received)
    }

    /** 同じ状態を指定した場合は通知しない。 */
    @Test
    fun doesNotNotifyWhenStateIsUnchanged() {
        controller.addListener(listener)
        received.clear()
        assertFalse(controller.setEnabled(false))
        assertTrue(received.isEmpty())
    }

    /** 解除したリスナには通知されない。 */
    @Test
    fun stopsNotifyingRemovedListener() {
        controller.addListener(listener)
        controller.removeListener(listener)
        received.clear()
        controller.setEnabled(true)
        assertTrue(received.isEmpty())
    }

    /** toggle() は状態を反転して結果を返す。 */
    @Test
    fun toggleFlipsState() {
        assertTrue(controller.toggle())
        assertTrue(controller.isEnabled)
        assertFalse(controller.toggle())
        assertFalse(controller.isEnabled)
    }

    /** 複数のリスナすべてに通知される。 */
    @Test
    fun notifiesEveryListener() {
        val other = mutableListOf<Boolean>()
        controller.addListener(listener)
        controller.addListener { enabled -> other.add(enabled) }
        controller.setEnabled(true)
        assertEquals(listOf(false, true), received)
        assertEquals(listOf(false, true), other)
    }

    /** アプリ全体で共有するインスタンスは常に同じもの。 */
    @Test
    fun sharedInstanceIsASingleton() {
        assertTrue(KioskModeController.instance === KioskModeController.instance)
    }
}
