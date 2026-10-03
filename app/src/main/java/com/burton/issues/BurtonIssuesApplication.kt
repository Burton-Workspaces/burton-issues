package com.burton.issues

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.Coil
import coil.ImageLoader
import com.burton.issues.data.refresh.BackgroundRefreshScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BurtonIssuesApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var backgroundRefreshScheduler: BackgroundRefreshScheduler

    override fun onCreate() {
        super.onCreate()
        Coil.setImageLoader(imageLoader)
        backgroundRefreshScheduler.start()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
