package com.agentickitchen.android.app

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

internal fun installFirebaseAppCheckProvider() {
    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
        PlayIntegrityAppCheckProviderFactory.getInstance()
    )
}
