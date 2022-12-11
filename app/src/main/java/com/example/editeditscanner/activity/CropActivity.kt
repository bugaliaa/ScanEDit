package com.example.editeditscanner.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import com.example.editeditscanner.data.BoundingRect
import com.example.editeditscanner.databinding.ActivityCropBinding
import com.example.editeditscanner.utils.DetectBox
import com.example.editeditscanner.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class CropActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityCropBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var croppedUri = intent.getStringExtra("cropped_path")
        var editedUri = intent.getStringExtra("edited_path")
        if (editedUri != null) {
            binding.tvRetake.text = "Cancel"
        }

        val uri = intent.getStringExtra("source_path")
        val angle = intent.getIntExtra("angle", 0)
        val framePos = intent.getIntExtra("frame_position", 0)
        val bitmap = BitmapFactory.decodeFile(uri)
        var width = Utils.getDeviceWidth()
        var height = (bitmap.height * (width / bitmap.width.toFloat())).toInt()

        if (height > width) {
            val ratio: Double = (width.toDouble() / height) * 1.5
            height = (ratio * height).toInt()
            width = (ratio * width).toInt()
        }

        val viewHeight = bitmap.height * (width / bitmap.width.toFloat())
        val scaleFactor = height / viewHeight * 0.9f
        val params = LinearLayout.LayoutParams(width, height).apply { gravity = Gravity.CENTER }
        val ratio = width / bitmap.width.toDouble()

        binding.cvCrop.apply {
            layoutParams = params
            setImageBitmap(bitmap)
            animate().rotation(angle.toFloat())
                .scaleX(scaleFactor)
                .scaleY(scaleFactor)
                .setDuration(500)
                .start()
        }

        lifecycleScope.launch(Dispatchers.Default) {
            binding.cvCrop.setBoundingRect(
                DetectBox.findCorners(bitmap, 0) ?: BoundingRect().apply {
                    val w = bitmap.width
                    val h = bitmap.height
                    val padding = w * 0.1
                    topLeft = Point(padding * ratio, padding * ratio)
                    topRight = Point((w - padding) * ratio, padding * ratio)
                    bottomLeft = Point(padding * ratio, (h - padding) * ratio)
                    bottomRight = Point((w - padding) * ratio, (h - padding) * ratio)
                })
        }

        binding.tvRetake.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        binding.tvConfirm.setOnClickListener {
            binding.progressFrame.visibility = View.VISIBLE
            lifecycleScope.launch(Dispatchers.Default) {
                editedUri?.let { File(it).delete() }
                croppedUri = croppedUri ?: Utils.createPhotoFile(this@CropActivity).absolutePath
                getPerspectiveTransform(
                    bitmap,
                    binding.cvCrop.getBoundingRect(),
                    ratio
                ).run {
                    Utils.rotateMat(this, angle)
                    Utils.saveMat(this, croppedUri)
                }
                bitmap.recycle()

                setResult(RESULT_OK, Intent().apply {
                    putExtra("source_path", uri)
                    putExtra("cropped_path", croppedUri)
                    putExtra("frame_position", framePos)
                    putExtra("angle", angle)
                })
                finish()
            }
        }
    }

    companion object {
        fun getPerspectiveTransform(
            bitmap: Bitmap,
            boundingRect: BoundingRect,
            ratio: Double
        ): Mat {
            val tl = boundingRect.topLeft
            val tr = boundingRect.topRight
            val bl = boundingRect.bottomLeft
            val br = boundingRect.bottomRight
            val srcMat = Mat(4, 1, CvType.CV_32FC2).apply {
                put(
                    0, 0,
                    tl.x / ratio, tl.y / ratio,
                    bl.x / ratio, bl.y / ratio,
                    br.x / ratio, br.y / ratio,
                    tr.x / ratio, tr.y / ratio
                )
            }
            val dstMat = Mat(4, 1, CvType.CV_32FC2)
            val hwRatio = getHWRatio(tl, tr, bl, br, bitmap.width, bitmap.height, ratio)
            val height: Double
            val width: Double
            if (hwRatio != Double.POSITIVE_INFINITY) {
                val widthA = sqrt((br.x - bl.x).pow(2.0) + (br.y - bl.y).pow(2.0))
                val widthB = sqrt((tr.x - tl.x).pow(2.0) + (tr.y - tl.y).pow(2.0))
                width = max(widthA, widthB)
                height = width / hwRatio
            } else {
                height = bl.y - tl.y
                width = tr.x - tl.x
            }
            dstMat.put(
                0, 0,
                0.0, 0.0,
                0.0, height,
                width, height,
                width, 0.0
            )

            val mat = Mat()
            org.opencv.android.Utils.bitmapToMat(bitmap, mat)
            return mat.clone().apply {
                Imgproc.warpPerspective(
                    mat,
                    this,
                    Imgproc.getPerspectiveTransform(srcMat, dstMat),
                    Size(width, height)
                )
            }
        }

        fun getPerspectiveTransform(
            sourceMat: Mat,
            boundingRect: BoundingRect,
            ratio: Double
        ): Mat {
            val mat = sourceMat.clone()
            val srcMat = Mat(4, 1, CvType.CV_32FC2)
            val dstMat = Mat(4, 1, CvType.CV_32FC2)
            val tl = boundingRect.topLeft
            val tr = boundingRect.topRight
            val bl = boundingRect.bottomLeft
            val br = boundingRect.bottomRight
            srcMat.put(
                0, 0,
                tl.x / ratio, tl.y / ratio,
                bl.x / ratio, bl.y / ratio,
                br.x / ratio, br.y / ratio,
                tr.x / ratio, tr.y / ratio
            )
            val hwRatio = getHWRatio(tl, tr, bl, br, mat.width(), mat.height(), ratio)
            val widthA = sqrt((br.x - bl.x).pow(2.0) + (br.y - bl.y).pow(2.0))
            val widthB = sqrt((tr.x - tl.x).pow(2.0) + (tr.y - tl.y).pow(2.0))
            val width = max(widthA, widthB)
            val height = width / hwRatio
            dstMat.put(
                0, 0,
                0.0, 0.0,
                0.0, height,
                width, height,
                widthA, 0.0
            )

            return mat.clone().apply {
                Imgproc.warpPerspective(
                    mat,
                    this,
                    Imgproc.getPerspectiveTransform(srcMat, dstMat),
                    Size(width, height)
                )
            }
        }

        private fun getHWRatio(
            TL: Point,
            TR: Point,
            BL: Point,
            BR: Point,
            width: Int,
            height: Int,
            ratio: Double
        ): Double {
            var m1x = TL.x / ratio
            var m1y = TL.y / ratio
            var m2x = TR.x / ratio
            var m2y = TR.y / ratio
            var m3x = BL.x / ratio
            var m3y = BL.y / ratio
            var m4x = BR.x / ratio
            var m4y = BR.y / ratio

            val u0 = (width / 2f).toDouble()
            val v0 = (height / 2f).toDouble()

            m1x -= u0
            m2x -= u0
            m3x -= u0
            m4x -= u0

            m1y -= v0
            m2y -= v0
            m3y -= v0
            m4y -= v0


            val k2 = ((m1y - m4y) * m3x - (m1x - m4x) * m3y + m1x * m4y - m1y * m4x) /
                    ((m2y - m4y) * m3x - (m2x - m4x) * m3y + m2x * m4y - m2y * m4x)
            val k3 = ((m1y - m4y) * m2x - (m1x - m4x) * m2y + m1x * m4y - m1y * m4x) /
                    ((m3y - m4y) * m2x - (m3x - m4x) * m2y + m3x * m4y - m3y * m4x)

            val fSquared =
                -((k3 * m3y - m1y) * (k2 * m2y - m1y) + (k3 * m3x - m1x) * (k2 * m2x - m1x)) /
                        ((k3 - 1) * (k2 - 1))

            var hwRatio = sqrt(
                ((k2 - 1).pow(2) +
                        (k2 * m2y - m1y).pow(2) / fSquared +
                        (k2 * m2x - m1x).pow(2) / fSquared) /
                        ((k3 - 1).pow(2) +
                                (k3 * m3y - m1y).pow(2) / fSquared +
                                (k3 * m3x - m1x).pow(2) / fSquared)
            )

            if (k2 == 1.0 && k3 == 1.0)
                hwRatio = sqrt(
                    ((m2y - m1y).pow(2) + (m2x - m1x)).pow(2) /
                            ((m3y - m1y).pow(2) + (m3x - m1x).pow(2))
                )

            return hwRatio
        }
    }
}