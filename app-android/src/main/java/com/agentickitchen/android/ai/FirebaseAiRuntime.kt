package com.agentickitchen.android.ai

import android.app.Application
import com.agentickitchen.android.app.installFirebaseAppCheckProvider
import com.agentickitchen.shared.ai.KitchenAiProvider
import com.google.firebase.FirebaseApp

internal object FirebaseAiRuntime {
    fun create(application: Application): KitchenAiProvider? {
        val firebaseApp = runCatching { FirebaseApp.initializeApp(application) }.getOrNull() ?: return null
        return runCatching {
            installFirebaseAppCheckProvider()
            FirebaseAiProvider(firebaseApp)
        }.getOrNull()
    }
}
