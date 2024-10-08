package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.checkerframework.checker.units.qual.A
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()


    private lateinit var remindersListViewModel: RemindersListViewModel
    private lateinit var fakeDataSource: FakeDataSource


    @Before
    fun setup() {
        fakeDataSource = FakeDataSource()
        remindersListViewModel = RemindersListViewModel(
            app = ApplicationProvider.getApplicationContext(),
            dataSource = fakeDataSource
        )
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun loadReminders_loading() = runTest {
        // Given a viewModel
        mainCoroutineRule.testDispatcher.scheduler.advanceUntilIdle()

        // When loadReminders is called
        remindersListViewModel.loadReminders()

        // Then the loading indicator is shown
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Given the loadReminders task finishes
        mainCoroutineRule.testDispatcher.scheduler.advanceUntilIdle()

        // Then the loading indicator is hidden
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }


    @Test
    fun loadReminders_emptyList_showNoData() = runTest {
        // Given an empty list of reminders from data source
        fakeDataSource.deleteAllReminders()

        val remindersListViewModel = RemindersListViewModel(
            app = ApplicationProvider.getApplicationContext(),
            dataSource = fakeDataSource
        )

        // When loadReminders is called and an empty list is returned by data source
        mainCoroutineRule.testDispatcher.scheduler.advanceUntilIdle()
        remindersListViewModel.loadReminders()
        mainCoroutineRule.testDispatcher.scheduler.advanceUntilIdle()

        // Then show no data indicator
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun loadReminders_error_showSnackBar() = runTest {
        val title = "Title"
        val description = "Description"
        val location = "Location"
        val latitude = 1.0
        val longitude = 2.0
        val id = "id1"

        val reminder = ReminderDTO(title, description, location, latitude, longitude, id)
        fakeDataSource.deleteAllReminders()
        fakeDataSource.setReturnError(true)
        fakeDataSource.saveReminder(reminder)

        // Given a data source that returns an error
        val remindersListViewModel = RemindersListViewModel(
            app = ApplicationProvider.getApplicationContext(),
            dataSource = fakeDataSource
        )

        // When loadReminders is called
        mainCoroutineRule.testDispatcher.scheduler.advanceUntilIdle()
        remindersListViewModel.loadReminders()
        mainCoroutineRule.testDispatcher.scheduler.advanceUntilIdle()

        // Then show no data indicator is shown and snack bar error is shown with the exception message
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(true))
        assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(), `is`("Test exception"))
    }

    @Test
    fun loadReminders_listWithValues_showData() = runTest {
        val title = "Title"
        val description = "Description"
        val location = "Location"
        val latitude = 1.0
        val longitude = 2.0
        val id = "id1"

        val reminder = ReminderDTO(title, description, location, latitude, longitude, id)
        fakeDataSource.deleteAllReminders()
        fakeDataSource.saveReminder(reminder)

        val remindersListViewModel = RemindersListViewModel(
            app = ApplicationProvider.getApplicationContext(),
            dataSource = fakeDataSource
        )

        // Given a data source 
        mainCoroutineRule.testDispatcher.scheduler.advanceUntilIdle()
        remindersListViewModel.loadReminders()
        mainCoroutineRule.testDispatcher.scheduler.advanceUntilIdle()

        // Then the correct value is retrived from dataSource and No Data indicator is hidden
        val remindersList = remindersListViewModel.remindersList.getOrAwaitValue()
        val reminderResult = remindersList.first()

        assertThat(remindersList.isEmpty(), `is`(false))
        assertThat(reminderResult.title, `is`(title))
        assertThat(reminderResult.description, `is`(description))
        assertThat(reminderResult.location, `is`(location))
        assertThat(reminderResult.latitude, `is`(latitude))
        assertThat(reminderResult.longitude, `is`(longitude))
        assertThat(reminderResult.id, `is`(id))
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(false))
    }
}