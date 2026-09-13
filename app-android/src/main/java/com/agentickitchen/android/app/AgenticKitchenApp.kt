package com.agentickitchen.android.app

import android.app.Application
import com.agentickitchen.android.AppLogger

class AgenticKitchenApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        container = AppContainer(this)
    }
}
