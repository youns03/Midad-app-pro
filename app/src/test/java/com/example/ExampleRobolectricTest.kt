package com.example

import android.content.Context
import android.graphics.Typeface
import android.text.TextPaint
import androidx.test.core.app.ApplicationProvider
import com.example.kashida.KashidaEngine
import com.example.model.KashidaLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("مِداد", appName)
  }

  @Test
  fun `verify kashida engine connection points`() {
    val word = "جميل"
    val points = KashidaEngine.findConnectionPointsInWord(word)
    assertTrue("Word جميل should have connection points", points.isNotEmpty())
  }

  @Test
  fun `verify smart kashida insertion`() {
    val text = "بسم الله الرحمن الرحيم الحمد لله رب العالمين"
    val paint = TextPaint().apply {
      typeface = Typeface.DEFAULT
      textSize = 18f
    }
    val kashidaText = KashidaEngine.shapeLine(text, 200f, KashidaLevel.MEDIUM, paint)
    assertTrue("Should insert tatweel character", kashidaText.contains("ـ"))
    assertEquals(text, KashidaEngine.stripKashida(kashidaText))
  }
}
