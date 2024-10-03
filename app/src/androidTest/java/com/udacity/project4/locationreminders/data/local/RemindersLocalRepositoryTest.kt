package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemindersLocalRepositoryTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase
    private lateinit var repository: RemindersLocalRepository

    @Before
    fun setup() {
        // Using an in-memory database because the information stored here disappears when the process is killed
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()

        repository = RemindersLocalRepository(
            database.reminderDao(),
            Dispatchers.IO
        )
    }

    @After
    fun teardown() {
        database.close()  // Close the database once the test is done
    }

    @Test
    fun saveReminder_getReminderById() = runBlocking {
        // Given a reminder is saved
        val reminder = ReminderDTO(
            "Test title", "Test description", "Test location", 10.0, 10.0, "1"
        )
        repository.saveReminder(reminder)

        // When the reminder is retrieved by ID
        val result = repository.getReminder(reminder.id)

        // Then the reminder is retrieved successfully and the fields are correct
        assertThat(result is Result.Success, `is`(true))
        val resultReminder = (result as Result.Success).data
        assertThat(resultReminder.title, `is`(reminder.title))
        assertThat(resultReminder.description, `is`(reminder.description))
        assertThat(resultReminder.location, `is`(reminder.location))
        assertThat(resultReminder.latitude, `is`(reminder.latitude))
        assertThat(resultReminder.longitude, `is`(reminder.longitude))
    }

    @Test
    fun getReminderById_notFound() = runBlocking {
        // When trying to retrieve a reminder with a non-existent ID
        val result = repository.getReminder("non_existent_id")

        // Then the result should be an error
        assertThat(result is Result.Error, `is`(true))
        result as Result.Error
        assertThat(result.message, `is`("Reminder not found!"))
    }

    @Test
    fun deleteAllReminders_emptyListReturned() = runBlocking {
        // Given a reminder is saved
        val reminder = ReminderDTO(
            "Test title", "Test description", "Test location", 10.0, 10.0, "1"
        )
        repository.saveReminder(reminder)

        // When all reminders are deleted
        repository.deleteAllReminders()

        // Then the reminders list is empty
        val result = repository.getReminders()
        assertThat(result is Result.Success, `is`(true))
        result as Result.Success
        assertThat(result.data.isEmpty(), `is`(true))
    }

    @Test
    fun getReminders_emptyListReturnedWhenNoPreviousData() = runBlocking {
        // Given an empty database

        // When all reminders are retrieved
        repository.getReminders()

        // Then the reminders list is empty
        val result = repository.getReminders()
        assertThat(result is Result.Success, `is`(true))
        result as Result.Success
        assertThat(result.data.isEmpty(), `is`(true))
    }
}