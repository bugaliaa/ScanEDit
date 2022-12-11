package com.example.editeditscanner.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.editeditscanner.data.Frame

class ExportPdf {

    companion object {

        private fun createPage(pdfDocument: PdfDocument, bitmap: Bitmap, width: Int, height: Int) {
            val pageInfo = PdfDocument.PageInfo.Builder(width, height, 0).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()
            paint.color = Color.WHITE
            canvas.drawPaint(paint)
            canvas.drawBitmap(
                bitmap,
                (width - bitmap.width) / 2f,
                (height - bitmap.height) / 2f,
                null
            )
            pdfDocument.finishPage(page)
        }

        fun exportPdf(frames: List<Frame>): PdfDocument {
            var bitmap: Bitmap?
            val pdf = PdfDocument()
            var width = 0
            var height = 0
            for (frame in frames) {
                bitmap = BitmapFactory.decodeFile(frame.editedUri)
                if (bitmap.width > width) width = bitmap.width
                if (bitmap.height > height) height = bitmap.height
                createPage(pdf, bitmap, width, height)
            }

            return pdf
        }
    }
}