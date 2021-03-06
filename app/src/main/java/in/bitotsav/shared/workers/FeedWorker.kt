package `in`.bitotsav.shared.workers

import `in`.bitotsav.feed.data.FeedRepository
import `in`.bitotsav.shared.exceptions.NonRetryableException
import `in`.bitotsav.shared.utils.isBitotsavOver
import `in`.bitotsav.shared.workers.FeedWorkType.valueOf
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import org.koin.core.KoinComponent
import org.koin.core.get

private const val TAG = "FeedWorker"

enum class FeedWorkType {
    FETCH_FEEDS
}

class FeedWorker(context: Context, params: WorkerParameters) : Worker(context, params), KoinComponent {

    override fun doWork(): Result {
        try {
            if (isBitotsavOver())
                return Result.success()
            @Suppress("UNUSED_VARIABLE")
            val type = inputData.getString("type")?.let { valueOf(it) }
                ?: throw NonRetryableException("Invalid work type")
            runBlocking {
                val latestFeedTimestamp = get<FeedRepository>().getLatestTimestamp()
                get<FeedRepository>().fetchFeedsAfterAsync(latestFeedTimestamp).await()
            }
            return Result.success()
        } catch (e: Exception) {
            Log.d(TAG, e.message ?: "Unknown Error")
            return Result.retry()
        }
    }
}