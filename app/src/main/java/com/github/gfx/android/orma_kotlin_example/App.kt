package com.github.gfx.android.orma_kotlin_example

import android.app.Application
import io.realm.Realm

class App: Application() {
    override fun onCreate() {
        super.onCreate()

        OrmaHolder.initialize(this)
        Realm.init(this)
    }
}
