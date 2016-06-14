package com.github.gfx.android.orma_kotlin_example

import android.app.Application

class App: Application() {
    override fun onCreate() {
        super.onCreate()

        OrmaHolder.initialize(this)
    }
}
