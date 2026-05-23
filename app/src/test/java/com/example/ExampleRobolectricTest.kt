package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.CarpoolViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    assertEquals("VeiliuCarpool", appName)
  }

  @Test
  fun testViewModelRegisterUser() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = CarpoolViewModel(application)
    
    viewModel.registerUser(
        fullName = "Test User",
        email = "test@gmail.com",
        phoneWhatsapp = "12345678",
        gender = "Male",
        role = "Rider"
    )
    
    // Idle the main looper and wait for background threads
    println("Idling main looper in a poll loop...")
    var attempts = 0
    while (viewModel.currentUser.value == null && viewModel.signupError.value == null && attempts < 30) {
        try {
            Thread.sleep(100)
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
        } catch (t: Throwable) {
            println("Exception during idle: ${t.message}")
        }
        attempts++
    }
    
    val signupError = viewModel.signupError.value
    println("Signup Error state: $signupError")
    
    val currentUser = viewModel.currentUser.value
    println("Current User state: $currentUser")
    
    assertNotNull("Current user is null! Signup error is: $signupError", currentUser)
    assertEquals("test@gmail.com", currentUser?.email)
  }
}
