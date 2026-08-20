package io.doany.lockview.kiosk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [UnlockSequenceDetector] のローカルユニットテスト。
 * キーコードは Android の定数と衝突しない適当な値でよい。
 */
class UnlockSequenceDetectorTest {

    private val up = 24
    private val down = 25
    private val other = 26
    private val sequence = listOf(up, down, up, down, up)

    private fun detector(timeoutMillis: Long = 5000L) =
        UnlockSequenceDetector(sequence, timeoutMillis)

    /** 正しい順番で最後まで入力すると成立する。 */
    @Test
    fun completesWhenFullSequenceIsEntered() {
        val detector = detector()
        var time = 0L
        sequence.dropLast(1).forEach { keyCode ->
            time += 100
            assertFalse(detector.onKeyDown(keyCode, time))
        }
        assertTrue(detector.onKeyDown(sequence.last(), time + 100))
    }

    /** 成立後は状態がリセットされ、もう一度入力すれば再度成立する。 */
    @Test
    fun canBeTriggeredRepeatedly() {
        val detector = detector()
        var time = 0L
        repeat(2) {
            sequence.forEachIndexed { index, keyCode ->
                time += 100
                val completed = detector.onKeyDown(keyCode, time)
                assertEquals(index == sequence.lastIndex, completed)
            }
        }
    }

    /** 途中に関係のないキーが挟まると成立しない。 */
    @Test
    fun doesNotCompleteWhenInterruptedByAnotherKey() {
        val detector = detector()
        var time = 0L
        listOf(up, down, other, up, down, up).forEach { keyCode ->
            time += 100
            assertFalse(detector.onKeyDown(keyCode, time))
        }
    }

    /** シーケンスの前にゴミ入力があっても、直近の入力が揃っていれば成立する。 */
    @Test
    fun ignoresKeysEnteredBeforeTheSequence() {
        val detector = detector()
        var time = 0L
        listOf(other, down, down).forEach { keyCode ->
            time += 100
            assertFalse(detector.onKeyDown(keyCode, time))
        }
        sequence.forEachIndexed { index, keyCode ->
            time += 100
            val completed = detector.onKeyDown(keyCode, time)
            assertEquals(index == sequence.lastIndex, completed)
        }
    }

    /** 最初のキーから最後のキーまでが制限時間を超えると成立しない。 */
    @Test
    fun doesNotCompleteWhenSequenceIsTooSlow() {
        val detector = detector(timeoutMillis = 1000L)
        assertFalse(detector.onKeyDown(up, 0L))
        assertFalse(detector.onKeyDown(down, 100L))
        assertFalse(detector.onKeyDown(up, 200L))
        assertFalse(detector.onKeyDown(down, 300L))
        // 1 件目から 1000ms を超えているので、最後まで押しても成立しない。
        assertFalse(detector.onKeyDown(up, 1500L))
    }

    /** 制限時間を過ぎたあとに入力し直せば成立する。 */
    @Test
    fun completesWhenSequenceIsRestartedAfterTimeout() {
        val detector = detector(timeoutMillis = 1000L)
        assertFalse(detector.onKeyDown(up, 0L))
        assertFalse(detector.onKeyDown(down, 5000L))
        assertFalse(detector.onKeyDown(up, 5100L))
        assertFalse(detector.onKeyDown(down, 5200L))
        assertFalse(detector.onKeyDown(up, 5300L))
        // ここまでで直近 5 件が揃っていないため、残りを入力すると成立する。
        assertFalse(detector.onKeyDown(down, 5400L))
        assertTrue(detector.onKeyDown(up, 5500L))
    }

    /** reset() で入力途中の状態が破棄される。 */
    @Test
    fun resetDiscardsPartialInput() {
        val detector = detector()
        var time = 0L
        sequence.dropLast(1).forEach { keyCode ->
            time += 100
            detector.onKeyDown(keyCode, time)
        }
        assertEquals(sequence.size - 1, detector.progress)
        detector.reset()
        assertEquals(0, detector.progress)
        assertFalse(detector.onKeyDown(sequence.last(), time + 100))
    }

    /** 1 キーだけのシーケンスも扱える。 */
    @Test
    fun supportsSingleKeySequence() {
        val detector = UnlockSequenceDetector(listOf(up))
        assertFalse(detector.onKeyDown(down, 0L))
        assertTrue(detector.onKeyDown(up, 100L))
    }

    /** 同じキーが連続するシーケンスでも取りこぼさない。 */
    @Test
    fun handlesSequencesWithRepeatedKeys() {
        val detector = UnlockSequenceDetector(listOf(up, up, down))
        assertFalse(detector.onKeyDown(up, 0L))
        assertFalse(detector.onKeyDown(up, 100L))
        // 3 回連続で押しても、直近 2 件が up なので次の down で成立する。
        assertFalse(detector.onKeyDown(up, 200L))
        assertTrue(detector.onKeyDown(down, 300L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptySequence() {
        UnlockSequenceDetector(emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveTimeout() {
        UnlockSequenceDetector(sequence, timeoutMillis = 0L)
    }
}
