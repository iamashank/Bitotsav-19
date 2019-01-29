package `in`.bitotsav.notification.fcm

import `in`.bitotsav.HomeActivity
import `in`.bitotsav.events.data.EventRepository
import `in`.bitotsav.feed.data.Feed
import `in`.bitotsav.feed.data.FeedRepository
import `in`.bitotsav.feed.data.FeedType
import `in`.bitotsav.notification.utils.Channel
import `in`.bitotsav.notification.utils.displayNotification
import `in`.bitotsav.profile.User
import `in`.bitotsav.shared.network.getWork
import `in`.bitotsav.shared.network.scheduleWork
import `in`.bitotsav.shared.workers.*
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.get
import org.koin.core.KoinComponent

private enum class UpdateType {
    EVENT,
    RESULT,
    ANNOUNCEMENT,
    PM,
    ALL_EVENTS,
    ALL_TEAMS
}

class DefaultFirebaseMessagingService : FirebaseMessagingService(), KoinComponent {

    companion object {
        private const val TAG = "FirebaseMsgService"
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        Log.d(TAG, "From: ${remoteMessage?.from}")

        // Check if message contains a data payload.
        remoteMessage?.data?.isNotEmpty()?.let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)

            val updateType: UpdateType
            try {
                updateType = UpdateType.valueOf(remoteMessage.data["type"] ?: return)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, e.message)
                return
            }

            if (UpdateType.ALL_EVENTS == updateType) {
                scheduleWork<EventWorker>(workDataOf("type" to EventWorkType.FETCH_ALL_EVENTS.name))
                return
            }

            if (UpdateType.ALL_TEAMS == updateType) {
                scheduleWork<TeamWorker>(workDataOf("type" to TeamWorkType.FETCH_ALL_TEAMS.name))
                return
            }

            val title = remoteMessage.data["title"] ?: return
            val content = remoteMessage.data["content"] ?: return
            val timestamp = remoteMessage.data["timestamp"]?.toLong() ?: System.currentTimeMillis()
            val feedId = remoteMessage.data["feedId"]?.toLong() ?: return
            val feedType = FeedType.valueOf(updateType.name)
            var channel = Channel.valueOf(updateType.name)

            when (updateType) {
                UpdateType.ANNOUNCEMENT, UpdateType.PM -> {
                    val feed = Feed(
                        feedId,
                        title,
                        content,
                        feedType.name,
                        timestamp,
                        false,
                        null,
                        null
                    )
                    CoroutineScope(Dispatchers.IO).async {
                        get<FeedRepository>().insert(feed)
                    }
//                    TODO("Pass appropriate intent")
                    val intent = Intent(this, HomeActivity::class.java)
                    displayNotification(
                        title,
                        content,
                        timestamp,
                        channel,
                        intent,
                        this
                    )
                }
                else -> {
//                    UpdateType.EVENT or UpdateType.RESULT
                    val eventId = remoteMessage.data["eventId"]?.toInt() ?: return
                    val deferredIsStarred = CoroutineScope(Dispatchers.IO).async {
                        get<EventRepository>().isStarred(eventId)
                    }
                    val deferredEventName = CoroutineScope(Dispatchers.IO).async {
                        get<EventRepository>().getEventName(eventId)
                    }
//                    TODO("Refresh data from server here")

                    if (updateType == UpdateType.EVENT) {
                        scheduleWork<EventWorker>(
                            workDataOf("type" to EventWorkType.FETCH_EVENT.name, "eventId" to eventId)
                        )
                    } else {
                        val eventWork = getWork<EventWorker>(
                            workDataOf("type" to EventWorkType.FETCH_EVENT.name, "eventId" to eventId)
                        )
                        val resultWork = getWork<ResultWorker>(
                            workDataOf("type" to ResultWorkType.RESULT.name, "eventId" to eventId)
                        )
                        WorkManager.getInstance().beginUniqueWork(
                            eventId.toString(),
                            ExistingWorkPolicy.REPLACE,
                            eventWork
                        ).then(resultWork).enqueue()
                    }

                    val isStarred = runBlocking { deferredIsStarred.await() } ?: return
                    val eventName = runBlocking { deferredEventName.await() } ?: return
                    val feed = Feed(
                        feedId,
                        title,
                        content,
                        feedType.name,
                        timestamp,
                        isStarred,
                        eventId,
                        eventName
                    )
                    CoroutineScope(Dispatchers.IO).async {
                        get<FeedRepository>().insert(feed)
                    }
                    if (isStarred) channel = Channel.STARRED
//                    TODO("Pass appropriate intent")
                    val intent = Intent(this, HomeActivity::class.java)
                    displayNotification(
                        title,
                        content,
                        timestamp,
                        channel,
                        intent,
                        applicationContext
                    )
                }
            }
        }
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String?) {
        Log.d(TAG, "Refreshed token: $token")
        token?.let {
            User.fcmToken = token
            if (User.isLoggedIn)
                sendTokenToServer()
            return
        }
        Log.wtf(TAG, "Empty token generated!")
    }

    /**
     * Schedule a job using FirebaseJobDispatcher.
     */
//    private fun scheduleJob(bundle: Bundle, tag: String) {
//        val dispatcher = Singleton.dispatcher.getInstance(applicationContext)
//        Log.d(TAG, "Scheduling new job")
//        val random = Random()
//        val timeDelay = random.nextInt(5)
//        val myJob = dispatcher.newJobBuilder()
//            .setService(NetworkJobService::class.java)
//            .setTag(tag)
//            .setRecurring(false)
//            .setLifetime(Lifetime.FOREVER)
//            .setTrigger(Trigger.executionWindow(timeDelay, 10))
//            .setReplaceCurrent(false)
//            .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
//            .setConstraints(
//                Constraint.ON_ANY_NETWORK
//            )
//            .setExtras(bundle)
//            .build()
//
//        dispatcher.mustSchedule(myJob)
//    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private fun sendTokenToServer() {
        scheduleWork<FcmTokenWorker>(workDataOf("type" to FcmTokenWorkType.SEND_TOKEN.name))
    }
}