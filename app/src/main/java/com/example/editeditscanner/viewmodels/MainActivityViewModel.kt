package com.example.editeditscanner.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.editeditscanner.dao.DocumentDao
import com.example.editeditscanner.dao.FrameDao
import com.example.editeditscanner.data.Document
import kotlinx.coroutines.launch

class MainActivityViewModel(
    private val documentDao: DocumentDao?,
    frameDao: FrameDao?
) : MainViewModel(frameDao) {
    var docId: Long? = null
    private var documents: LiveData<MutableList<Document>>? = null

    fun getAllDocuments(): LiveData<MutableList<Document>>? {
        if (documents == null) {
            viewModelScope.launch {
                documents = documentDao?.getAllDocuments()
            }
        }
        return documents
    }
}

class MainActivityViewModelFactory(
    private val documentDao: DocumentDao,
    private val frameDao: FrameDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MainActivityViewModel(documentDao, frameDao) as T
    }
}