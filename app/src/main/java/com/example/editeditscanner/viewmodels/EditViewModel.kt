package com.example.editeditscanner.viewmodels

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.editeditscanner.App
import com.example.editeditscanner.dao.FrameDao
import com.example.editeditscanner.data.Frame

class EditViewModel(
    application: App,
    private val frameDao: FrameDao
) : AndroidViewModel(application) {

    var frame: LiveData<Frame>? = null

    fun getFrame(frameId: Long) : LiveData<Frame>? {
        frame = frameDao.getFrame(frameId)
        return frame
    }
}

class EditViewModelFactory(
    private val application: App,
    private val frameDao: FrameDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return EditViewModel(application, frameDao) as T
    }
}