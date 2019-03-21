/*
 * Copyright 2018-2019 Sandip Vaghela
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.afterroot.expenses.firebase

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.afterroot.expenses.R
import com.afterroot.expenses.ui.HomeActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FireMessagingService : FirebaseMessagingService() {

    private val _tag = "FireMessagingService"

    override fun onNewToken(token: String?) {
        super.onNewToken(token)
        Log.d(_tag, "NEW_TOKEN $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        Log.d(_tag, "From: " + remoteMessage!!.from)

        if (remoteMessage.data.isNotEmpty()) {
            Log.d(_tag, "Message data payload: " + remoteMessage.data)
        }

        if (remoteMessage.notification != null) {
            Log.d(_tag, "Message Notification Body: " + remoteMessage.notification!!.body!!)
        }

        //TODO Handle Notification
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, "Expenses")
                .setSmallIcon(R.drawable.launch_icon)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(remoteMessage.notification!!.body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notificationBuilder.build())
    }
}
