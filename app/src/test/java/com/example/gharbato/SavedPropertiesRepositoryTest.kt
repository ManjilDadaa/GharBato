package com.example.gharbato

import com.example.gharbato.model.PropertyModel
import com.example.gharbato.repository.SavedPropertiesRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class SavedPropertiesRepositoryTest {
    private val database = mock(FirebaseDatabase::class.java)
    private val auth = mock(FirebaseAuth::class.java)
    private val repo = SavedPropertiesRepositoryImpl(database, auth)

    @Test fun guestReadsAreEmptyWithoutUsingASharedAccount() = runBlocking {
        assertTrue(repo.getSavedProperties().isEmpty())
        assertTrue(repo.getSavedPropertiesFlow().first().isEmpty())
        assertFalse(repo.isPropertySaved(1))
        verifyNoInteractions(database)
    }

    @Test fun guestCannotSave() = runBlocking {
        try {
            repo.saveProperty(PropertyModel(id = 1))
            fail("Expected sign-in failure")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Sign in"))
        }
        verifyNoInteractions(database)
    }

    @Test fun saveAndRemoveFailuresReachTheCaller() = runBlocking {
        val user = mock(FirebaseUser::class.java)
        val reference = mock(DatabaseReference::class.java)
        `when`(auth.currentUser).thenReturn(user)
        `when`(user.uid).thenReturn("test-user")
        `when`(database.getReference("users/test-user/UserFavoriteList")).thenReturn(reference)
        `when`(reference.child("1")).thenReturn(reference)
        val failure = IllegalStateException("permission denied")
        `when`(reference.setValue(any())).thenReturn(Tasks.forException(failure))
        `when`(reference.removeValue()).thenReturn(Tasks.forException(failure))
        for (remove in listOf(false, true)) {
            try {
                if (remove) repo.removeSavedProperty(1) else repo.saveProperty(PropertyModel(id = 1))
                fail("Failure must not be swallowed")
            } catch (e: IllegalStateException) { assertSame(failure, e) }
        }
    }
    @Test fun cancelledListenerReachesCollectorAndDetaches() = runBlocking {
        val user = mock(FirebaseUser::class.java)
        val reference = mock(DatabaseReference::class.java)
        `when`(auth.currentUser).thenReturn(user)
        `when`(user.uid).thenReturn("test-user")
        `when`(database.getReference("users/test-user/UserFavoriteList")).thenReturn(reference)
        val denied = DatabaseError.fromCode(DatabaseError.PERMISSION_DENIED)
        doAnswer { invocation ->
            val listener = invocation.arguments[0] as ValueEventListener
            `when`(auth.currentUser).thenReturn(null)
            listener.onCancelled(denied)
            listener
        }.`when`(reference).addValueEventListener(any(ValueEventListener::class.java))
        try {
            repo.getSavedPropertiesFlow().first()
            fail("Expected listener error")
        } catch (e: com.google.firebase.database.DatabaseException) {
            assertEquals(denied.toException().message, e.message)
        }
        verify(reference).removeEventListener(any(ValueEventListener::class.java))
    }

}
