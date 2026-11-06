package com.furkanbarissonmezisik.tesbihatpro

import android.app.Application
import com.furkanbarissonmezisik.tesbihatpro.di.AppContainer

class TesbihApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}
