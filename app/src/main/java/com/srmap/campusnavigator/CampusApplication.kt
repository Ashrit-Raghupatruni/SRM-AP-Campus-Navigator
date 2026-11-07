package com.srmap.campusnavigator

import android.app.Application
import com.srmap.campusnavigator.data.local.database.AppDatabase

class CampusApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
    }
}
