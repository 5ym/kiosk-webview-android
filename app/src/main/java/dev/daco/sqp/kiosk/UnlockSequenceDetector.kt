package dev.daco.sqp.kiosk

/**
 * キオスクモードを解除するための隠しキーシーケンスを検出する。
 *
 * 直近に押されたキーを [sequence] の長さぶんだけ保持し、それが [sequence] と
 * 一致し、かつ最初と最後の入力の間隔が [timeoutMillis] 以内であれば成立とみなす。
 *
 * Android に依存しないので、ローカルユニットテストで検証できる。
 */
class UnlockSequenceDetector(
    sequence: List<Int>,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS
) {

    private val sequence: List<Int> = sequence.toList()
    private val recentKeys = ArrayDeque<KeyPress>()

    init {
        require(this.sequence.isNotEmpty()) { "sequence must not be empty" }
        require(timeoutMillis > 0) { "timeoutMillis must be positive" }
    }

    /** シーケンスの何個目まで入力済みかを返す。UI へ進捗を出したいとき用。 */
    val progress: Int
        get() = recentKeys.size

    /** 入力途中の状態を破棄する。 */
    fun reset() {
        recentKeys.clear()
    }

    /**
     * キーの押下を 1 件通知する。
     *
     * @param keyCode 押されたキーのコード。
     * @param eventTimeMillis 押下時刻。[android.view.KeyEvent.getEventTime] を想定。
     * @return シーケンスが成立したとき true。成立時は内部状態がリセットされる。
     */
    fun onKeyDown(keyCode: Int, eventTimeMillis: Long): Boolean {
        // 古すぎる入力は同一シーケンスの一部とみなさない。
        while (recentKeys.isNotEmpty() && eventTimeMillis - recentKeys.first().timeMillis > timeoutMillis) {
            recentKeys.removeFirst()
        }
        recentKeys.addLast(KeyPress(keyCode, eventTimeMillis))
        while (recentKeys.size > sequence.size) {
            recentKeys.removeFirst()
        }
        if (recentKeys.size < sequence.size) {
            return false
        }
        for (index in sequence.indices) {
            if (recentKeys[index].keyCode != sequence[index]) {
                return false
            }
        }
        reset()
        return true
    }

    private data class KeyPress(val keyCode: Int, val timeMillis: Long)

    companion object {
        /** 最初のキーから最後のキーまでの許容時間。 */
        const val DEFAULT_TIMEOUT_MILLIS = 5000L
    }
}
