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

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class LoginUnitTest {
    @Test
    fun login_test() {
        val repo = mock<UserRepo>()
        val viewmodel = UserViewModel(repo)

        doAnswer{ invocation ->
            val callback = invocation.getArgument<(Boolean, String) -> Unit>(2)
            callback(true,"Login Successful")
            null
        }.`when`(repo).login(eq("test2@gmail.com"), eq("123456"), any())

        var successResult = false
        var messageResult = ""

        viewmodel.login("test2@gmail.com","123456"){ success, msg ->
            successResult = success
            messageResult = msg
        }

        assertTrue(successResult)
        assertEquals("Login Successful", messageResult)

        verify(repo).login(eq("test2@gmail.com"), eq("123456"), any())
    }
}