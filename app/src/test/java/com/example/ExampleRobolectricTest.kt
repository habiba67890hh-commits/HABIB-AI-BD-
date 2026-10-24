package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.PasswordSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("HABIB AI", appName)
  }

  @Test
  fun `password hashing and verification works`() {
    val salt = PasswordSecurity.generateSalt()
    val saltBytes = android.util.Base64.decode(salt, android.util.Base64.NO_WRAP)
    val password = "secret_password_123"
    val hash = PasswordSecurity.hashPassword(password.toCharArray(), saltBytes)

    val isMatch = PasswordSecurity.verifyPassword(password, salt, hash)
    assertTrue("Password hash must match original password", isMatch)
  }
}
