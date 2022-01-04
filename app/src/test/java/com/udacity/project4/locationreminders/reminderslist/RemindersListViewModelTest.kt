package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    //provide testing to the RemindersListViewModel and its live data objects

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: RemindersListViewModel

    @Before
    fun setupViewModel() {
        viewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(), FakeDataSource()
        )
    }

    @Test
    fun invalidateShowNoData() {
        var remindersList: List<ReminderDataItem>? = emptyList()
        var showNoData = remindersList?.isEmpty() == true || remindersList == null
        assertThat(showNoData, `is`(true))

        remindersList = null
        showNoData = remindersList?.isEmpty() == true || remindersList == null
        assertThat(showNoData, `is`(true))

        remindersList = listOf(
            ReminderDataItem("", "", "", 0.0,
            0.0)
        )
        showNoData = remindersList.isEmpty() == true || remindersList == null
        assertThat(showNoData, `is`(false))
    }

    @Test
    fun loadReminders() = mainCoroutineRule.runBlockingTest {
        viewModel.dataSource.saveReminder(ReminderDTO(
            "title",
            "desc",
            "location",
            0.0,
            0.0,
            "Random"
        ))

        when (val result = viewModel.dataSource.getReminders()) {
            is Result.Success<*> -> {
                val dataList = ArrayList<ReminderDataItem>()
                dataList.addAll((result.data as List<ReminderDTO>).map { reminder ->
                    ReminderDataItem(
                        reminder.title,
                        reminder.description,
                        reminder.location,
                        reminder.latitude,
                        reminder.longitude,
                        reminder.id
                    )
                })
                viewModel.remindersList.value = dataList
            }
            is Result.Error ->
                viewModel.remindersList.value = null
        }

        val value = viewModel.remindersList.value?.get(0)?.title

        assertThat(value, `is`("title"))
    }
}