package com.example.editeditscanner.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.editeditscanner.dao.DocumentDao
import com.example.editeditscanner.dao.FrameDao
import com.example.editeditscanner.data.Document
import com.example.editeditscanner.data.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ViewPageActivityViewModel(
    documentDao: DocumentDao,
    private val frameDao: FrameDao,
    docId: String
) : ViewModel() {

    val document: LiveData<Document> = documentDao.getDocument(docId)
    val frames: LiveData<MutableList<Frame>> = frameDao.getFrames(docId)
    var currentIndex = 0

    fun updateFrame(frame: Frame) {
        viewModelScope.launch(Dispatchers.IO) {
            frameDao.update(frame)
        }
    }

    fun deleteFrame(frame: Frame) {
        viewModelScope.launch(Dispatchers.IO) {
            frameDao.delete(frame)
        }
    }
}

class ViewPageActivityViewModelFactory(
    private val documentDao: DocumentDao,
    private val frameDao: FrameDao,
    private val docId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ViewPageActivityViewModel(documentDao, frameDao, docId) as T
    }
}