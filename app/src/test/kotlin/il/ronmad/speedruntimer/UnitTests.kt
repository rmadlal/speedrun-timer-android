package il.ronmad.speedruntimer

import android.graphics.Color

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class UnitTests {

    @Test
    @Throws(Exception::class)
    fun testTimeSplit() {
        val bestTime = (43 + 1000 * 27 + 1000 * 60 * 5).toLong()
        val hours = (bestTime / (3600 * 1000)).toInt()
        var remaining = (bestTime % (3600 * 1000)).toInt()
        val minutes = remaining / (60 * 1000)
        remaining %= (60 * 1000)
        val seconds = remaining / 1000
        val milliseconds = remaining % 1000

        assertEquals(0, hours.toLong())
        assertEquals(5, minutes.toLong())
        assertEquals(27, seconds.toLong())
        assertEquals(43, milliseconds.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testAlpha() {
        println(Color.alpha(Color.argb(255, 255, 255, 255)))
    }
}
