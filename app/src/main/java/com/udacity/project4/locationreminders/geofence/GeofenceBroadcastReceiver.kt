package com.udacity.project4.locationreminders.geofence

import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings.Global.getString
import android.util.Log
import android.util.Printer
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.R
import com.udacity.project4.locationreminders.ReminderDescriptionActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.ACTION_GEOFENCE_EVENT
import com.udacity.project4.utils.GEOFENCE_CHANNEL_ID
import com.udacity.project4.utils.NOTIFICATION_ID
import com.udacity.project4.utils.REMINDER_ID
import com.udacity.project4.utils.REMINDER_NOTIFICATION_PENDING_INTENT_REQUEST_CODE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * Triggered by the Geofence.  Since we can have many Geofences at once, we pull the request
 * ID from the first Geofence, and locate it within the cached data in our Room DB
 *
 * Or users can add the reminders and then close the app, So our app has to run in the background
 * and handle the geofencing in the background.
 * To do that you can use https://developer.android.com/reference/android/support/v4/app/JobIntentService to do that.
 *
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderDataSource: ReminderDataSource = GlobalContext.get().get()

        if (intent.action == ACTION_GEOFENCE_EVENT) {
            println("prueba, geo fence triggered")
            val geofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent?.hasError() == true) {
                val errorMessage =
                    GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
                println("prueba, geofence has error $errorMessage")
                Log.e("GeofenceReceiver", "Error: $errorMessage")
                return
            }

            // Get the geofence transition type.
            val geofenceTransition = geofencingEvent?.geofenceTransition


            println("prueba, geofence transition $geofenceTransition")
            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                println("prueba, geo fence transition enter")

                println("prueba, triggeringGeofences list size: ${geofencingEvent.triggeringGeofences?.size}")

                geofencingEvent.triggeringGeofences?.forEach { geofence ->
                    val id = geofence.requestId

                    println("prueba, geofence request id: $id")



                    CoroutineScope(Dispatchers.IO).launch {
                        val result: com.udacity.project4.locationreminders.data.dto.Result<ReminderDTO> =
                            reminderDataSource.getReminder(id)

                        println("prueba, getting reminder from database" )

                        if (result is com.udacity.project4.locationreminders.data.dto.Result.Success) {
                            println("prueba, success: reminder : ${result.data}" )
                            sendNotification(context = context, reminderDTO = result.data)
                        }
                    }
                }
            }
        }
    }

    private fun sendNotification(context: Context, reminderDTO: ReminderDTO) {
        val reminderDataItem = ReminderDataItem(
            title = reminderDTO.title,
            description = reminderDTO.description,
            location = reminderDTO.location,
            latitude = reminderDTO.latitude,
            longitude = reminderDTO.longitude,
            id = reminderDTO.id
        )

        val intent = ReminderDescriptionActivity.newIntent(context, reminderDataItem)

        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                REMINDER_NOTIFICATION_PENDING_INTENT_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

        val notification = NotificationCompat.Builder(context, GEOFENCE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_location)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(
                context.getString(
                    R.string.notification_message,
                    reminderDTO.description
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = ContextCompat.getSystemService(
            context,
            NotificationManager::class.java
        ) as NotificationManager

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

}


