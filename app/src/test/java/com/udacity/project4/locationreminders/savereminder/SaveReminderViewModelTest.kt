package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsNull.nullValue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class SaveReminderViewModelTest {
//provide testing to the SaveReminderView and its live data objects

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: SaveReminderViewModel

    @Before
    fun setupViewModel() {
        stopKoin()
        viewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(), FakeDataSource())
    }

    @Test
    fun validateEnteredData_reminderDataTitleNull_returnsFalse() {
        val value: Boolean
        val data = ReminderDataItem(
            null,
            null,
            null,
            null,
            null
        )
        value = !data.title.isNullOrEmpty()

        assertThat(value, `is`(false))
    }

    @Test
    fun onClear_liveDataIsNull() {

        viewModel.apply {
            reminderTitle.value = "title"
            reminderDescription.value = "desc"
            reminderSelectedLocationStr.value = "Mountain View, Ca"
            selectedPOI.value = PointOfInterest(LatLng(0.0,0.0), "GooglePlex", "GooglePlex")
            latitude.value = 0.0
            longitude.value = 0.0
        }

        viewModel.onClear()
        val title = viewModel.reminderTitle.value
        val desc = viewModel.reminderDescription.value
        val selectedLocation = viewModel.reminderSelectedLocationStr.value
        val poi = viewModel.selectedPOI.value
        val lat = viewModel.latitude.value
        val lng = viewModel.longitude.value

        assertThat(title, `is`(nullValue()))
        assertThat(desc, `is`(nullValue()))
        assertThat(selectedLocation, `is`(nullValue()))
        assertThat(poi, `is`(nullValue()))
        assertThat(lat, `is`(nullValue()))
        assertThat(lng, `is`(nullValue()))
    }

    @Test
    fun validateAndSaveReminder_liveDataValues() {
        val reminder = ReminderDataItem(
            "title",
            "desc",
            "location",
            0.0,
            0.0
        )
        mainCoroutineRule.pauseDispatcher()
        viewModel.validateAndSaveReminder(reminder)

        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.resumeDispatcher()

        val loading = viewModel.showLoading.getOrAwaitValue()
        val toast = viewModel.showToast.getOrAwaitValue()

        assertThat(loading, `is`(false))
        assertThat(toast, `is`("Reminder Saved !"))
    }
}