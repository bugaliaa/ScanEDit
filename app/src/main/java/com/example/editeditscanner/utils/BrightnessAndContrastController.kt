package com.example.editeditscanner.utils

import org.opencv.core.Mat

class BrightnessAndContrastController(var brightness: Double, var contrast: Double) {
    var mat: Mat? = null

    fun setBrightness(sourceMat: Mat, value: Double) : Mat {
        brightness = value
        process(sourceMat).let {
            mat = it
            return it
        }
    }

    fun setContrast(sourceMat: Mat, value: Double): Mat {
        contrast = value
        process(sourceMat).let {
            mat = it
            return it
        }
    }

    private fun process(sourceMat: Mat) : Mat {
        val mat = Mat()
        sourceMat.copyTo(mat)
        mat.convertTo(mat, -1, contrast, brightness)
        return mat
    }
}