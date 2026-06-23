package com.counter.app

import android.app.Application
import com.counter.app.data.CounterDatabase

class CounterApp : Application() {
    val database by lazy { CounterDatabase.getInstance(this) }
}
