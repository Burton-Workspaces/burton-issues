package com.burton.issues.data.refresh

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.burton.issues.data.repository.LocalPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundRefreshScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: LocalPrefs,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            combine(prefs.backgroundRefresh, prefs.auth) { enabled, auth ->
                enabled && auth.accessToken.isNotBlank()
            }.distinctUntilChanged().collect { active ->
                if (active) enqueue() else cancel()
            }
        }
    }

    private fun enqueue() {
        val request = PeriodicWorkRequestBuilder<InboxRefreshWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
    }

    companion object {
        const val UNIQUE_NAME = "inbox_refresh"
    }
}
