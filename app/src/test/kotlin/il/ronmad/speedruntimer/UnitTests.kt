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
        val time = (43 + 1000 * 27 + 1000 * 60 * 5).toLong()

        val (hours, minutes, seconds, milliseconds) = time.getTimeUnits()

        assertEquals(0, hours.toLong())
        assertEquals(5, minutes.toLong())
        assertEquals(27, seconds.toLong())
        assertEquals(43, milliseconds.toLong())
    }
}
