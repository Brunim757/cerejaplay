package com.cerejaplay.app

import android.app.Application

class CerejaPlayApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
