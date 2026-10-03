package com.burton.issues.data.refresh

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.burton.issues.data.repository.IssuesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class InboxRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: IssuesRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            repository.refreshInboxInBackground()
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }
}
