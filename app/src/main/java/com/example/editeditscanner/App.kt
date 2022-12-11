package com.example.editeditscanner

import android.app.Application
import com.example.editeditscanner.utils.MyDatabase

class App : Application() {
    val database by lazy { MyDatabase.getDatabase(this) }
}