package com.zbycorp.wx

import android.app.Application
import android.content.Context

class App : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        // MultiDex.install(this)
    }
}