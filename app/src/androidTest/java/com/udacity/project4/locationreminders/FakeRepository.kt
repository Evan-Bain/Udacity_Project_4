package com.udacity.project4.locationreminders

import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.RemindersDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest

@ExperimentalCoroutinesApi
class FakeRepository(
    private val reminderDao: RemindersDao) : ReminderDataSource {

    private var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if(shouldReturnError) {
            return Result.Error("Test Exception")
        }
        return try {
            Result.Success(reminderDao.getReminders())
        } catch (ex: Exception) {
            Result.Error(ex.localizedMessage)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminderDao.saveReminder(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if(shouldReturnError) {
            return Result.Error("Test Exception")
        }
        return try {
            val reminder = reminderDao.getReminderById(id)
            if (reminder != null) {
                Result.Success(reminder)
            } else {
                Result.Error("Reminder not found!")
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage)
        }
    }

    override suspend fun deleteAllReminders() {
        reminderDao.deleteAllReminders()
    }
}