package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ConversationEntity
import com.example.data.local.RickyDatabase
import kotlinx.coroutines.runBlocking
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
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RICKY AI", appName)
    }

    @Test
    fun `insert and retrieve conversation in Room database`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = RickyDatabase.getInstance(context)
        val conv = ConversationEntity(
            id = "test-conv-1",
            title = "Test Conversation",
            modelUsed = "gemini-3.8-flash"
        )
        db.chatDao().insertConversation(conv)
        val retrieved = db.chatDao().getConversationById("test-conv-1")
        assertNotNull(retrieved)
        assertEquals("Test Conversation", retrieved?.title)
    }
}
