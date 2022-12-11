package com.example.editeditscanner.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.editeditscanner.dao.FrameDao
import kotlinx.coroutines.launch

open class MainViewModel(
    private val frameDao: FrameDao?
) : ViewModel() {

    fun getPageCount(docId: String): LiveData<Int> {
        lateinit var count: LiveData<Int>
        viewModelScope.launch {
            count = frameDao?.getFrameCount(docId) ?: MutableLiveData(0)
        }
        return count
    }

    fun getFirstFrameImagePath(docId: String): LiveData<String>? {
        return frameDao?.getFrameUri(docId)
    }
}