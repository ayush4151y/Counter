package com.counter.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.counter.app.data.CounterDatabase
import com.counter.app.data.ReelCount
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = CounterDatabase.getInstance(application)

    val totalCount = MutableLiveData(0)
    val instagramCount = MutableLiveData(0)
    val youtubeCount = MutableLiveData(0)
    val facebookCount = MutableLiveData(0)

    private var currentDate = getTodayDate()

    init {
        observeCounts()

        BroadcastObserver.register(
            application,
            ReelCountingService.ACTION_COUNT_UPDATED
        ) { intent ->
            val pkg = intent.getStringExtra(ReelCountingService.EXTRA_PACKAGE) ?: return@register
            val count = intent.getIntExtra(ReelCountingService.EXTRA_COUNT, 0)
            updateFromService(pkg, count)
        }
    }

    private fun observeCounts() {
        viewModelScope.launch {
            db.reelCountDao().observeTotal(currentDate).collectLatest { total ->
                totalCount.postValue(total ?: 0)
            }
        }
        for ((pkg, liveData) in mapOf(
            "com.instagram.android" to instagramCount,
            "com.google.android.youtube" to youtubeCount,
            "com.facebook.katana" to facebookCount
        )) {
            viewModelScope.launch {
                db.reelCountDao().observeCount(currentDate, pkg).collectLatest { count ->
                    liveData.postValue(count?.count ?: 0)
                }
            }
        }
    }

    private fun updateFromService(pkg: String, count: Int) {
        when (pkg) {
            "com.instagram.android" -> instagramCount.postValue(count)
            "com.google.android.youtube" -> youtubeCount.postValue(count)
            "com.facebook.katana" -> youtubeCount.postValue(count)
            "com.facebook.orca" -> facebookCount.postValue(count)
        }
        viewModelScope.launch {
            totalCount.postValue(db.reelCountDao().getTotalForDate(currentDate) ?: 0)
        }
    }

    private fun getTodayDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
