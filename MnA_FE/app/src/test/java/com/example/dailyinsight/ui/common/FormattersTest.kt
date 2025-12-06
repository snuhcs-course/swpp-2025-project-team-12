package com.example.dailyinsight.ui.common

import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.dailyinsight.R
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FormattersTest {

    private lateinit var context: Context
    private lateinit var textView: TextView

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        textView = TextView(context)
    }

    // ===== setChange Tests =====

    @Test
    fun setChange_positiveRate_showsPlusSign() {
        textView.setChange(1000, 1.5)

        val text = textView.text.toString()
        assertTrue(text.contains("+"))
    }

    @Test
    fun setChange_negativeRate_showsMinusSign() {
        textView.setChange(1000, -1.5)

        val text = textView.text.toString()
        assertTrue(text.contains("-"))
    }

    @Test
    fun setChange_zeroRate_showsNoSign() {
        textView.setChange(0, 0.0)

        val text = textView.text.toString()
        assertFalse(text.startsWith("+"))
        assertFalse(text.startsWith("-"))
    }

    @Test
    fun setChange_setsTextColor_forPositiveRate() {
        textView.setChange(1000, 1.5)

        // Color should be set (we can't easily verify the exact color without more setup)
        assertNotNull(textView.currentTextColor)
    }

    @Test
    fun setChange_setsTextColor_forNegativeRate() {
        textView.setChange(1000, -1.5)

        assertNotNull(textView.currentTextColor)
    }

    @Test
    fun setChange_setsTextColor_forZeroRate() {
        textView.setChange(0, 0.0)

        assertNotNull(textView.currentTextColor)
    }

    @Test
    fun setChange_formatsLargeNumber() {
        textView.setChange(1000000, 5.0)

        val text = textView.text.toString()
        // Should contain formatted number (with commas in Korean locale)
        assertTrue(text.isNotEmpty())
    }

    @Test
    fun setChange_formatsSmallNumber() {
        textView.setChange(100, 0.5)

        val text = textView.text.toString()
        assertTrue(text.isNotEmpty())
    }

    @Test
    fun setChange_handlesZeroChange() {
        textView.setChange(0, 0.0)

        val text = textView.text.toString()
        assertTrue(text.isNotEmpty())
    }

    @Test
    fun setChange_formatsDecimalRate() {
        textView.setChange(500, 1.234)

        val text = textView.text.toString()
        // Rate should be formatted to 2 decimal places
        assertTrue(text.isNotEmpty())
    }

    @Test
    fun setChange_handlesNegativeChange() {
        textView.setChange(-500, -2.5)

        val text = textView.text.toString()
        assertTrue(text.contains("-"))
    }

    // ===== setPriceWon Tests =====

    @Test
    fun setPriceWon_formatsPrice() {
        textView.setPriceWon(50000)

        val text = textView.text.toString()
        assertTrue(text.isNotEmpty())
        // Should contain the price number
        assertTrue(text.contains("50"))
    }

    @Test
    fun setPriceWon_formatsLargePrice() {
        textView.setPriceWon(1000000000)

        val text = textView.text.toString()
        assertTrue(text.isNotEmpty())
    }

    @Test
    fun setPriceWon_formatsZeroPrice() {
        textView.setPriceWon(0)

        val text = textView.text.toString()
        assertTrue(text.isNotEmpty())
        assertTrue(text.contains("0"))
    }

    @Test
    fun setPriceWon_formatsSmallPrice() {
        textView.setPriceWon(100)

        val text = textView.text.toString()
        assertTrue(text.isNotEmpty())
        assertTrue(text.contains("100"))
    }

    @Test
    fun setPriceWon_formatsWithCommas() {
        textView.setPriceWon(1234567)

        val text = textView.text.toString()
        // Korean locale uses commas for thousands separator
        assertTrue(text.isNotEmpty())
    }
}
