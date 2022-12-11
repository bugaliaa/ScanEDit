package com.example.editeditscanner.viewmodels

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.*
import com.example.editeditscanner.activity.ListFramesActivity
import com.example.editeditscanner.dao.DocumentDao
import com.example.editeditscanner.dao.FrameDao
import com.example.editeditscanner.data.Document
import com.example.editeditscanner.data.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScanViewModel(
    private val documentDao: DocumentDao?,
    private val frameDao: FrameDao?
) : ViewModel() {
    private var newDocument = true
    var docId: String? = null
    var count: LiveData<Int> = MutableLiveData(0)

    private val paths: MutableList<Pair<String, String>> = ArrayList()

    fun addPath(sourceUri: String, croppedUri: String) {
        paths.add(Pair(sourceUri, croppedUri))
    }

    fun pathsCount(): Int {
        return paths.size
    }

    fun getPageCount(docId: String): LiveData<Int> {
        viewModelScope.launch {
            count = frameDao?.getFrameCount(docId) ?: MutableLiveData(0)
        }
        newDocument = false
        this.docId = docId
        return count
    }

    fun capture(name: String, angle: Int, count: Int, activity: Activity) {
        viewModelScope.launch(Dispatchers.Default) {
            if (newDocument) {
                val doc = Document()
                docId = doc.id
                doc.name = name
                doc.dateTime = System.currentTimeMillis()
                documentDao?.insert(doc)
            }
            for (i in paths.indices) {
                val path = paths[i]
                val frame = Frame(
                    timeInMillis = System.currentTimeMillis(),
                    index = count + i,
                    angle = angle,
                    docId = docId!!,
                    uri = path.first,
                    croppedUri = path.second
                )
                frameDao?.insert(frame)
            }
            if(!activity.isDestroyed) {
                val intent = Intent(activity, ListFramesActivity::class.java)
                intent.putExtra("document_id", docId)
                activity.startActivity(intent)
                activity.finish()
            }
        }
    }
}

class ScanActivityViewModelFactory(
    private val documentDao: DocumentDao?,
    private val frameDao: FrameDao?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ScanViewModel(documentDao, frameDao) as T
    }
}