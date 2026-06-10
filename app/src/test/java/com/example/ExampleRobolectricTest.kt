package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MED-PREP", appName)
  }

  @Test
  fun `test database and viewmodel initialization`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val app = context.applicationContext as MedPrepApplication
    assertNotNull(app)
    assertNotNull(app.database)
    assertNotNull(app.repository)
    assertNotNull(app.backupManager)

    val factory = MainViewModelFactory(app.repository, app.backupManager)
    val viewModel = factory.create(MainViewModel::class.java)
    assertNotNull(viewModel)
  }

  @Test
  fun `test main activity launch`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    assertNotNull(activity)
  }
}
