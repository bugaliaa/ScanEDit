package com.example.editeditscanner.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.editeditscanner.dao.DocumentDao
import com.example.editeditscanner.dao.FrameDao
import com.example.editeditscanner.data.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchViewModel(
    private val documentDao: DocumentDao,
    frameDao: FrameDao
) : MainViewModel(frameDao) {

    var documents: MutableLiveData<MutableList<Document>> = MutableLiveData()

    fun search(query: String) {
        viewModelScope.launch(Dispatchers.Main) {
            documents.postValue(documentDao.search("%$query%"))
        }
    }
}

class SearchViewModelFactory(
    private val documentDao: DocumentDao,
    private val frameDao: FrameDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SearchViewModel(documentDao, frameDao) as T
    }
}