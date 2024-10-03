package com.udacity.project4

import android.app.Activity
import android.app.Application
import android.os.IBinder
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.Root
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    KoinTest {// Extended Koin Test - embed autoclose @after method to close Koin after every test
    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) }
            single { LocalDB.createRemindersDao(appContext) }
            single<ReminderDataSource> { RemindersLocalRepository(get()) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoUtil.counting_id_resource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
        IdlingRegistry.getInstance().unregister(EspressoUtil.counting_id_resource)
    }

    @Test
    fun saveReminderButton_clickedWithNoLocation_errorToastMessageIsShown() = runBlocking {
        // Start RemindersActivity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // 1. Check if the RemindersActivity is displayed
        onView(withId(R.id.activity_reminders_cl)).check(matches(isDisplayed()))

        // 2. Navigate to the "Save Reminder" screen
        onView(withId(R.id.addReminderFAB)).perform(click())

        // 3. Enter reminder details
        onView(withId(R.id.reminderTitle)).perform(typeText("Test Reminder Title"))
        onView(withId(R.id.reminderDescription)).perform(typeText("Test Reminder Description"))
        onView(isRoot()).perform(closeSoftKeyboard())

        // 4. Select a location (mock the location selection)
        onView(withId(R.id.saveReminder)).perform(click())

        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_select_location)))

        activityScenario.close()
    }

    @Test
    fun saveReminderButton_clickedWithNoTitle_errorSnackbarMessageIsShown() = runBlocking {
        // Start RemindersActivity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // 1. Check if the RemindersActivity is displayed
        onView(withId(R.id.activity_reminders_cl)).check(matches(isDisplayed()))

        // 2. Navigate to the "Save Reminder" screen
        onView(withId(R.id.addReminderFAB)).perform(click())

        // 3. Enter reminder details
        onView(withId(R.id.reminderDescription)).perform(typeText("Test Reminder Description"))
        onView(isRoot()).perform(closeSoftKeyboard())

        // 4. Select a location (mock the location selection)
        onView(withId(R.id.saveReminder)).perform(click())

        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_enter_title)))

        activityScenario.close()
    }

    @Test
    fun floatingActionButton_clicked_saveReminderViewIsShown() = runBlocking {
        //Given the RemindersActivity is shown
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        //When the add reminder floating action button is clicked
        onView(withId(R.id.addReminderFAB)).perform(click())

        //Then the UI elements from SaveReminderFragment are shown
        onView(withId(R.id.selectLocation)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderDescription)).check(matches(isDisplayed()))
        activityScenario.close()
    }

    @Test
    fun saveReminderButton_clickedWithLocation_reminderSavedToastMessage() = runBlocking {
        // Start RemindersActivity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // 1. Check if the RemindersActivity is displayed
        onView(withId(R.id.activity_reminders_cl)).check(matches(isDisplayed()))

        // 2. Navigate to the "Save Reminder" screen
        onView(withId(R.id.addReminderFAB)).perform(click())

        // 3. Enter reminder details
        onView(withId(R.id.reminderTitle)).perform(typeText("Test Reminder Title"))
        onView(isRoot()).perform(closeSoftKeyboard())

        // 4. Go to map
        onView(withId(R.id.selectLocation)).perform(click())

        // 5. Click map
        onView(withId(R.id.map_center)).perform(click())

        // 6. Click on save location
        onView(withId(R.id.btn_save_location)).perform(click())

        // 7. Click on save reminder
        onView(withId(R.id.saveReminder)).perform(click())

        // 8 check toast
        onView(withText("Reminder Saved !"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))

        activityScenario.close()
    }

    private fun getActivityFromScenario(
        activityScenario: ActivityScenario<RemindersActivity>
    ): Activity? {
        var activity: Activity? = null
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }
}

class ToastMatcher : TypeSafeMatcher<Root?>() {
    override fun describeTo(description: Description?) {
        description?.appendText("is toast")
    }

    override fun matchesSafely(item: Root?): Boolean {
        val type: Int? = item?.windowLayoutParams?.get()?.type
        if (type == WindowManager.LayoutParams.TYPE_TOAST) {
            val windowToken: IBinder = item.decorView.windowToken
            val appToken: IBinder = item.decorView.getApplicationWindowToken()
            if (windowToken === appToken) { // means this window isn't contained by any other windows.
                return true
            }
        }
        return false
    }

}