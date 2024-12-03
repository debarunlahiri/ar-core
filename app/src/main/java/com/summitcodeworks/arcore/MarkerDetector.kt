package com.summitcodeworks.arcore

import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc

object MarkerDetector {
    fun detectMarkers(mat: Mat): List<Rect> {
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY)

        // Detect edges
        val edges = Mat()
        Imgproc.Canny(grayMat, edges, 50.0, 150.0)

        // Find contours
        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(edges, contours, Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)

        // Filter rectangular contours (markers)
        val markers = contours.mapNotNull {
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(MatOfPoint2f(*it.toArray()), approx, 0.02 * Imgproc.arcLength(MatOfPoint2f(*it.toArray()), true), true)
            if (approx.total() == 4L) Imgproc.boundingRect(MatOfPoint(*approx.toArray())) else null
        }

        return markers
    }
}