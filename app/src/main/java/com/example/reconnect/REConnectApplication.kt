package com.example.reconnect

import android.app.Application
import com.example.reconnect.data.local.REConnectDatabase
import com.example.reconnect.data.local.REConnectRepository
import kotlin.getValue

class REConnectApplication : Application() {
    val database by lazy { REConnectDatabase.getDatabase(this) }
    val repository by lazy { REConnectRepository(database) }
}