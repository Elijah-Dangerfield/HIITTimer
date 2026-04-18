package com.dangerfield.hiittimer

import android.app.Application

class HIITTimerApplication : Application() {
    
    lateinit var appComponent: AndroidAppComponent
        private set
    
    override fun onCreate() {
        super.onCreate()
        appComponent = AndroidAppComponent::class.create(this)
        appComponent.telemetry.initialize()
        appComponent.appEventDispatcher
    }
}
