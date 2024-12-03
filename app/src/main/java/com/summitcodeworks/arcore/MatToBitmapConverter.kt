package com.summitcodeworks.arcore

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat

object MatToBitmapConverter {
    fun convert(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }
}