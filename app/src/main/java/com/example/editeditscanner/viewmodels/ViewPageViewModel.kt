package com.example.editeditscanner.viewmodels

import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.*
import com.example.editeditscanner.dao.DocumentDao
import com.example.editeditscanner.dao.FrameDao
import com.example.editeditscanner.data.Document
import com.example.editeditscanner.data.Frame
import com.googlecode.tesseract.android.TessBaseAPI
import com.googlecode.tesseract.android.TessBaseAPI.ProgressValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class ViewPageActivityViewModel(
    documentDao: DocumentDao,
    private val frameDao: FrameDao,
    docId: String
) : ViewModel() {

    val document: LiveData<Document> = documentDao.getDocument(docId)
    val frames: LiveData<MutableList<Frame>> = frameDao.getFrames(docId)
    var currentIndex = 0

    private lateinit var tessBaseAPI: TessBaseAPI
    private val processing = MutableLiveData(false)
    private val progress = MutableLiveData<String>()
    private val result = MutableLiveData<String>()

    private var tessInit: Boolean = false
    private var stopped: Boolean = false

    init {
        tessBaseAPI = TessBaseAPI { progressValues: ProgressValues ->
            progress.postValue(
                "Progress: " + progressValues.percent + " %"
            )
        }

        progress.value = String.format(
            Locale.ENGLISH,
            "Tesseract %s (%s)",
            tessBaseAPI.version,
            tessBaseAPI.libraryFlavor
        )
    }

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

    fun initTesseract(dataPath: String, language: String, engineMode: Int) {
        Log.i(
            "Tesseract Init", "Initializing Tesseract with: dataPath = [" + dataPath + "], " +
                    "language = [" + language + "], engineMode = [" + engineMode + "]"
        )
        try {
            tessInit = tessBaseAPI.init(dataPath, language, engineMode)
        } catch (e: IllegalStateException) {
            tessInit = false
            Log.e("Tesseract Init", "Cannot Initialize Tesseract: ", e)
        }
    }

    fun recognizeImage(imagePath: File) {
        if (!tessInit) {
            Log.e("TESSERACT", "recognizeImage: Tesserat is not initialized")
            return
        }
        if (isProcessing()) {
            Log.e("TESSERACT", "recognizeImage: Process is in progress")
            return
        }
        result.value = ""
        processing.value = true
        progress.value = "Processing..."
        stopped = false

        Thread {
            tessBaseAPI.setImage(imagePath)
            // Or set it as Bitmap, Pix,...
            // tessApi.setImage(imageBitmap);
            val startTime: Long = SystemClock.uptimeMillis()

            // Use getHOCRText(0) method to trigger recognition with progress notifications and
            // ability to cancel ongoing processing.
            tessBaseAPI.getHOCRText(0)

            // Then get just normal UTF8 text as result. Using only this method would also trigger
            // recognition, but would just block until it is completed.
            val text: String = tessBaseAPI.getUTF8Text()
            result.postValue(text)
            processing.postValue(false)
            if (stopped) {
                progress.postValue("Stopped.")
            } else {
                val duration: Long = SystemClock.uptimeMillis() - startTime
                progress.postValue(
                    java.lang.String.format(
                        Locale.ENGLISH,
                        "Completed in %.3fs.", duration / 1000f
                    )
                )
            }
        }.start()
    }

    fun stop() {
        if(!isProcessing()) {
            return
        }
        tessBaseAPI.stop()
        progress.value = "Stopping..."
        stopped = true
    }

    override fun onCleared() {
        if (isProcessing()) {
            tessBaseAPI.stop()
        }

        tessBaseAPI.recycle()
    }

    fun isProcessing(): Boolean {
        return processing.value == true
    }

    fun isInitialized(): Boolean {
        return tessInit
    }

    fun getProcessing(): LiveData<Boolean> {
        return processing
    }

    fun getProgress(): LiveData<String> {
        return progress
    }

    fun getResult(): LiveData<String> {
        return result
    }

    fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality:Int) {
        outputStream().use { out ->
            bitmap.compress(format, quality, out)
            out.flush()
        }
    }
}

class ViewPageActivityViewModelFactory(
    private val documentDao: DocumentDao,
    private val frameDao: FrameDao,
    private val docId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ViewPageActivityViewModel(documentDao, frameDao, docId) as T
    }
}