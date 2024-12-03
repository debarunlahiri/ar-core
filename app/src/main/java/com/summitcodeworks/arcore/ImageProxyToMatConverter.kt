package com.summitcodeworks.arcore

import android.media.Image
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

object ImageProxyToMatConverter {
    fun convert(image: Image, rotationDegrees: Int): Mat {
        // Convert Image to byte array
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)

        // Create a Mat from the byte array
        val mat = Mat(image.height + image.height / 2, image.width, CvType.CV_8UC1)
        mat.put(0, 0, bytes)

        // Convert NV21 format to RGB
        val rgbMat = Mat()
        Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_YUV2RGB_NV21)

        // Rotate image if necessary
        if (rotationDegrees != 0) {
            val rotatedMat = Mat()
            Core.rotate(rgbMat, rotatedMat, when (rotationDegrees) {
                90 -> Core.ROTATE_90_CLOCKWISE
                180 -> Core.ROTATE_180
                270 -> Core.ROTATE_90_COUNTERCLOCKWISE
                else -> Core.ROTATE_180
            })
            return rotatedMat
        }

        return rgbMat
    }
}