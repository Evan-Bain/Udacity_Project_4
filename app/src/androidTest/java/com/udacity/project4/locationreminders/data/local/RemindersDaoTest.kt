package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

//    Add testing implementation to the RemindersDao.kt

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()


    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun saveReminder_retrievesReminder() = mainCoroutineRule.runBlockingTest {
        val newReminder = ReminderDTO("title", "desc",
        "location", 0.0, 0.0)
        database.reminderDao().saveReminder(newReminder)

        val result: ReminderDTO = database.reminderDao().getReminderById(newReminder.title!!)!!

        assertThat(result.title, `is`("title"))
        assertThat(result.description, `is`("desc"))
        assertThat(result.location, `is`("location"))
        assertThat(result.latitude, `is`(0.0))
        assertThat(result.longitude, `is`(0.0))
    }

    @Test
    fun deleteAllReminders() = mainCoroutineRule.runBlockingTest {
        val newReminder = ReminderDTO("title", "desc",
            "location", 0.0, 0.0)
        database.reminderDao().saveReminder(newReminder)

        database.reminderDao().deleteAllReminders()

        val result = database.reminderDao().getReminders()

        assertThat(result, `is`(emptyList()))
    }
}