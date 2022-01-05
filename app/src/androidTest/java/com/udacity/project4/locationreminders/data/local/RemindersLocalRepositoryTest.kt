package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.locationreminders.FakeRepository
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Error

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

//    Add testing implementation to the RemindersLocalRepository.kt

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()


    private lateinit var database: RemindersDatabase
    private lateinit var repository: FakeRepository

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        val reminderDao = database.reminderDao()
        repository =
            FakeRepository(reminderDao)
    }

    @After
    fun closeDb() = database.close()


    @Test
    fun getReminders_Success() = mainCoroutineRule.runBlockingTest {
        val newReminder = ReminderDTO("title", "desc",
            "location", 0.0, 0.0)
        repository.saveReminder(newReminder)

        val result = repository.getReminders()

        assertThat(result, `is`(Result.Success(listOf(newReminder))))
    }

    @Test
    fun getReminders_Failure() = mainCoroutineRule.runBlockingTest {
        repository.setShouldReturnError(true)

        val value = repository.getReminders()

        assertThat(value, `is`(Result.Error("Test Exception")))
    }

    @Test
    fun getReminder_Success() = mainCoroutineRule.runBlockingTest {
        val newReminder = ReminderDTO("title", "desc",
            "location", 0.0, 0.0)
        repository.saveReminder(newReminder)
        val value = repository.getReminder("title")

        assertThat(value, `is`(Result.Success(newReminder)))
    }

    @Test
    fun getReminder_Failure() = mainCoroutineRule.runBlockingTest {
        val newReminder = ReminderDTO("title", "desc",
            "location", 0.0, 0.0)
        repository.saveReminder(newReminder)
        repository.setShouldReturnError(true)

        val value = repository.getReminder("title")

        assertThat(value, `is`(Result.Error("Test Exception")))
    }

    @Test
    fun getReminder_errorReminderNotFound() = mainCoroutineRule.runBlockingTest {
        val newReminder = ReminderDTO("title", "desc",
            "location", 0.0, 0.0, "id")
        repository.saveReminder(newReminder)

        val value = repository.getReminder("id")

        assertThat(value, `is`(Result.Error("Reminder not found!")))
    }

    @Test
    fun deleteAllReminders() = mainCoroutineRule.runBlockingTest {
        val newReminder = ReminderDTO("title", "desc",
            "location", 0.0, 0.0, "id")
        repository.saveReminder(newReminder)
        repository.deleteAllReminders()

        val value = repository.getReminders()

        assertThat(value, `is`(Result.Success(emptyList())))
    }
}