package com.example.editeditscanner.activity

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.editeditscanner.App
import com.example.editeditscanner.databinding.ActivityScanBinding
import com.example.editeditscanner.utils.DetectBox
import com.example.editeditscanner.utils.Utils
import com.example.editeditscanner.utils.YuvToRgbConverter
import com.example.editeditscanner.viewmodels.ScanActivityViewModelFactory
import com.example.editeditscanner.viewmodels.ScanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

class ScanActivity : BaseActivity() {

    private val requestCodePermissions = 1001
    private val requiredPermissions =
        arrayOf("android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE")
    private var count = 0
    private var angle = 0

    private lateinit var executor: Executor
    private lateinit var binding: ActivityScanBinding
    private lateinit var viewModel: ScanViewModel
    private lateinit var converter: YuvToRgbConverter

    private fun initialiseViewModel() {
        (application as App).database?.let { db ->
            viewModel = ViewModelProvider(
                this,
                ScanActivityViewModelFactory(db.documentDao(), db.frameDao())
            ).get(ScanViewModel::class.java)
        }
    }

    private fun confirm() {
        val simpleDateFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm:ss", Locale.getDefault())
        val name = "EditeditScanner" + " " + simpleDateFormat.format(Date())
        viewModel.capture(name, angle, count, this)
    }

    @ExperimentalGetImage
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        executor = ContextCompat.getMainExecutor(this)
        converter = YuvToRgbConverter(this)
        initialiseViewModel()
        intent.getStringExtra("document_id")?.let { docId ->
            viewModel.getPageCount(docId).observe(this) { count -> this.count = count }
        }
        val width = Utils.getDeviceWidth()
        val height = (width * (4 / 3f)).toInt()
        binding.cameraFrame.layoutParams = LinearLayout.LayoutParams(width, height)
        binding.ivRecentCapture.setOnClickListener {
            confirm()
        }
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, requiredPermissions, requestCodePermissions)
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    @ExperimentalGetImage
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodePermissions) {
            if(allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    @ExperimentalGetImage
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    @ExperimentalGetImage
    fun processImage(imageProxy: ImageProxy) {
        imageProxy.image?.let { image ->
            if (image.format == ImageFormat.YUV_420_888) {
                lifecycleScope.launch(Dispatchers.Default) {
                    Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
                        .let { bitmap ->
                            converter.yuvToRgb(image, bitmap)
                            DetectBox.findCorners(bitmap, angle).let { box ->
                                imageProxy.close()
                                lifecycleScope.launch(Dispatchers.Main) {
                                    binding.scanView.setBoundingRect(box)
                                }
                            }
                        }
                }
            } else {
                imageProxy.close()
            }
        }
    }

    @ExperimentalGetImage
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        val builder = ImageCapture.Builder()
        val imageCapture = builder.build()
        val imageAnalysis = ImageAnalysis.Builder().build()

        preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis)
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), { imageProxy: ImageProxy ->
            angle = imageProxy.imageInfo.rotationDegrees
            processImage(imageProxy)
        })

        binding.btnCapture.setOnClickListener {
            binding.pbScan.visibility = View.VISIBLE
            val file = Utils.createPhotoFile(this)
            imageCapture.takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(),
                executor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Intent(this@ScanActivity, CropActivity::class.java).let {
                            it.putExtra("source_path", file.absolutePath)
                            it.putExtra("angle", angle)
                            resultLauncher.launch(it)
                        }
                        binding.pbScan.visibility = View.GONE
                    }

                    override fun onError(error: ImageCaptureException) {
                        Log.e(TAG, Log.getStackTraceString(error))
                    }
                })
        }
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { intent ->
                    val croppedPath = intent.getStringExtra("cropped_path")
                    val sourcePath = intent.getStringExtra("source_path")
                    viewModel.addPath(sourcePath!!, croppedPath!!)
                    binding.let {
                        it.ivRecentCapture.setImageBitmap(BitmapFactory.decodeFile(croppedPath))
                        if (it.pageCount.visibility != View.VISIBLE) it.pageCount.visibility =
                            View.VISIBLE
                        it.pageCount.text = viewModel.pathsCount().toString()
                    }
                }
            }
        }

    companion object {
        val TAG: String = ScanActivity::class.java.simpleName
    }
}