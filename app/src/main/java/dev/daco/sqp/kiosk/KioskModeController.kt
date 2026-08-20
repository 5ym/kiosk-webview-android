package dev.daco.sqp.kiosk

import java.util.concurrent.CopyOnWriteArrayList

/**
 * キオスクモードの ON/OFF を保持し、変更を購読者へ通知する。
 *
 * [dev.daco.sqp.MainActivity] と [SystemBarBlockerService] は同一プロセス上の
 * 別コンポーネントなので、両者の状態を揃えるためにこのコントローラを共有する。
 */
class KioskModeController {

    /** キオスクモードの状態変化を受け取るリスナ。 */
    fun interface Listener {
        fun onKioskModeChanged(enabled: Boolean)
    }

    private val listeners = CopyOnWriteArrayList<Listener>()

    @Volatile
    var isEnabled: Boolean = false
        private set

    /**
     * リスナを登録する。登録直後に現在の状態で一度呼び出されるため、
     * 購読側は初期状態を別途取得しなくてよい。
     */
    fun addListener(listener: Listener) {
        listeners.add(listener)
        listener.onKioskModeChanged(isEnabled)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    /**
     * キオスクモードを切り替える。
     *
     * @return 状態が実際に変化したとき true。
     */
    @Synchronized
    fun setEnabled(enabled: Boolean): Boolean {
        if (isEnabled == enabled) {
            return false
        }
        isEnabled = enabled
        listeners.forEach { it.onKioskModeChanged(enabled) }
        return true
    }

    /**
     * 現在の状態を反転させる。
     *
     * @return 反転後の状態。
     */
    fun toggle(): Boolean {
        setEnabled(!isEnabled)
        return isEnabled
    }

    companion object {
        /** アプリ全体で共有するインスタンス。 */
        val instance = KioskModeController()
    }
}
