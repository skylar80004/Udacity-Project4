package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.junit.After
import org.junit.Test
import java.io.IOException

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest {
    private lateinit var database: RemindersDatabase
    private lateinit var remindersDao: RemindersDao

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        remindersDao = database.reminderDao()
    }

    @After
    @Throws(IOException::class)
    fun teardown() {
        database.close()
    }

    @Test
    fun insertReminderAndRetrieveById() = runTest {
        // Given a reminder
        val reminder = ReminderDTO(
            title = "Test Title",
            description = "Test Description",
            location = "Test Location",
            latitude = 10.0,
            longitude = 10.0,
            id = "1"
        )

        // When inserting the reminder
        remindersDao.saveReminder(reminder)

        // Then the inserted reminder can be retrieved by ID
        val loaded = remindersDao.getReminderById(reminder.id)

        assertThat(loaded, notNullValue())
        assertThat(loaded?.id, `is`(equalTo(reminder.id)))
        assertThat(loaded?.title, `is`(equalTo(reminder.title)))
        assertThat(loaded?.description, `is`(equalTo(reminder.description)))
        assertThat(loaded?.location, `is`(equalTo(reminder.location)))
        assertThat(loaded?.latitude, `is`(equalTo(reminder.latitude)))
        assertThat(loaded?.longitude, `is`(equalTo(reminder.longitude)))
    }

    @Test
    fun getReminders_emptyDatabase() = runTest {
        // Given an empty database

        // When getting reminders
        val reminders = remindersDao.getReminders()

        // Then the result is an empty list
        assertThat(reminders, `is`(empty()))
    }

    @Test
    fun deleteAllReminders_emptyDatabase() = runTest {
        // Given a database with a reminder
        val reminder = ReminderDTO(
            title = "Test Title",
            description = "Test Description",
            location = "Test Location",
            latitude = 10.0,
            longitude = 10.0,
            id = "1"
        )
        remindersDao.saveReminder(reminder)

        // When deleting all reminders
        remindersDao.deleteAllReminders()

        // Then the database is empty
        val reminders = remindersDao.getReminders()
        assertThat(reminders, `is`(empty()))
    }

    @Test
    fun getReminderById_nonExistentReminder_returnsNull() = runTest {
        // Given a non-existent reminder ID

        // When getting reminder by ID
        val reminder = remindersDao.getReminderById("non-existent-id")

        // Then the result is null
        assertThat(reminder, `is`(nullValue()))
    }
}