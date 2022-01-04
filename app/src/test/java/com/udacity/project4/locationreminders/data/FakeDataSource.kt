package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import java.lang.Exception

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(
    var reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

//    Create a fake data source to act as a double to the real data source

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        reminders?.let { return Result.Success(ArrayList(it)) }
        return Result.Error("Reminders not found")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        reminders?.let {
            for(i in 0..reminders?.size!!) {
                if(id == reminders!![i].title) {
                    return Result.Success(reminders!![i])
                }
            }
        }
        return Result.Error("Reminder not found")
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }


}