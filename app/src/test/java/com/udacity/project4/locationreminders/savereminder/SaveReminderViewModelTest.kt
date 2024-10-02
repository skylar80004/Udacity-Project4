package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var saveReminderViewModel: SaveReminderViewModel
    private lateinit var fakeDataSource: FakeDataSource

    @Before
    fun setup() {
        fakeDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(
            app = ApplicationProvider.getApplicationContext(),
            dataSource = fakeDataSource
        )
    }

    @After
    fun teardown() {
        stopKoin()  // Stop Koin to clear the context after each test
    }

    @Test
    fun saveReminder_validReminder_showLoading() = runTest {
        // Given a valid reminder
        val reminder = ReminderDataItem(
            "Title",
            "Description",
            "Location",
            1.0,
            1.0
        )

        // Pause dispatcher to verify initial state
        mainCoroutineRule.testDispatcher.scheduler.advanceUntilIdle()

        // When saving the reminder
        saveReminderViewModel.validateAndSaveReminder(reminder)

        // Then loading is shown
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Resume the dispatcher to complete the save
        mainCoroutineRule.testDispatcher.scheduler.advanceUntilIdle()

        // Then loading is hidden and toast is shown
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(saveReminderViewModel.showToast.getOrAwaitValue(), `is`("Reminder Saved !"))
    }

    @Test
    fun saveReminder_emptyTitle_shouldReturnError() = runTest {
        // Given an invalid reminder (empty title)
        val reminder = ReminderDataItem(
            "",
            "Description",
            "Location",
            1.0,
            1.0
        )

        // When trying to save reminder
        saveReminderViewModel.validateAndSaveReminder(reminder)

        // Then an error message is shown for missing title
        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
    }

    @Test
    fun saveReminder_emptyLocation_shouldReturnError() = runTest {
        // Given an invalid reminder (empty location)
        val reminder = ReminderDataItem(
            "Title",
            "Description",
            "",
            1.0,
            1.0
        )

        // When trying to save reminder
        saveReminderViewModel.validateAndSaveReminder(reminder)

        // Then an error message is shown for missing location
        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_select_location))
    }

    @Test
    fun onSaveButtonClick_noLocationOrPOI_shouldShowError() = runTest {
        // Given no location or POI
        saveReminderViewModel.latitude.value = null
        saveReminderViewModel.longitude.value = null
        saveReminderViewModel.selectedPOI.value = null

        // When save button is clicked
        saveReminderViewModel.onSaveButtonClick()

        // Then show error
        assertThat(saveReminderViewModel.showError.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun onSaveButtonClick_withValidLocation_shouldNavigateBack() = runTest {
        // Given valid latitude and longitude
        saveReminderViewModel.latitude.value = 1.0
        saveReminderViewModel.longitude.value = 1.0

        // When save button is clicked
        saveReminderViewModel.onSaveButtonClick()

        // Then navigate back
        assertThat(saveReminderViewModel.navigateBack.getOrAwaitValue(), `is`(Unit))
    }
}
