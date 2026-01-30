package com.example.gharbato

import com.example.gharbato.repository.UserRepo
import com.example.gharbato.viewmodel.UserViewModel
import org.junit.Test
import org.mockito.kotlin.doAnswer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ForgotpasswordUnitTest {
    @Test
    fun forgotPassword_test() {
        val repo = mock<UserRepo>()
        val viewmodel = UserViewModel(repo)

        doAnswer { invocation ->
            val callback = invocation.getArgument<(Boolean, String) -> Unit>(1)
            callback(true, "Password reset link sent successfully")
            null
        }.`when`(repo).forgotPassword(eq("aryanshrestha@gmail.com"), any())

        var successResult = false
        var messageResult = ""

        viewmodel.forgotPassword("aryanshrestha@gmail.com") { success, msg ->
            successResult = success
            messageResult = msg
        }

        assertTrue(successResult)
        assertEquals("Password reset link sent successfully", messageResult)

        verify(repo).forgotPassword(eq("aryanshrestha@gmail.com"), any())
    }
}