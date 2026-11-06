package com.cerejaplay.app

import android.content.Context
import com.cerejaplay.app.data.api.NetworkModule
import com.cerejaplay.app.data.prefs.UserPreferences
import com.cerejaplay.app.data.repo.CerejaRepository

class AppContainer(context: Context) {

    val json = NetworkModule.json
    private val preferences = UserPreferences(context.applicationContext, json)
    private val service = NetworkModule.service()
    val repository = CerejaRepository(service, json, preferences)
}
