package com.summitcodeworks.arcore

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.opencv.android.OpenCVLoader
import org.opencv.calib3d.Calib3d
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.MatOfPoint3f
import org.opencv.core.Point
import org.opencv.core.Point3
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.utils.Converters
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var ivMain: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "OpenCV Initialization Failed!", Toast.LENGTH_LONG).show()
            return
        }

        setContentView(R.layout.activity_main)

        if (!hasCameraPermission()) {
            requestCameraPermission()
        } else {
            startCamera()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        startActivity(Intent(this, ARActivity::class.java))
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<PreviewView>(R.id.viewFinder).surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processImageWithOpenCV(imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // Function to process an ImageProxy frame and overlay objects on detected markers
    @OptIn(ExperimentalGetImage::class)
    private fun processImageWithOpenCV(imageProxy: ImageProxy) {
        ivMain = findViewById(R.id.ivMain)

        val mediaImage = imageProxy.image ?: return

        // Convert ImageProxy to OpenCV Mat
        val mat = ImageProxyToMatConverter.convert(mediaImage, imageProxy.imageInfo.rotationDegrees)

        // Detect markers (dummy detection for illustration, replace with your logic)
        val detectedMarkers = MarkerDetector.detectMarkers(mat)

        // If markers are detected
        if (detectedMarkers.isNotEmpty()) {
            runOnUiThread {
                Toast.makeText(this, "Detected ${detectedMarkers.size} markers!", Toast.LENGTH_SHORT).show()
            }

            // For each marker, overlay an object
            for (marker in detectedMarkers) {
                overlayObjectOnMarker(mat, marker)
            }
        }

        // Display the modified frame in the UI (optional)
        runOnUiThread {
            // Assume you have an ImageView `imageView` in your layout
            val bitmap = MatToBitmapConverter.convert(mat)
            ivMain.setImageBitmap(bitmap)
        }

        imageProxy.close()
    }

//    private fun overlayObjectOnMarker(mat: Mat, markerRect: Rect) {
//        // Extract corner points from the detected marker rectangle
//        val topLeft = Point(markerRect.x.toDouble(), markerRect.y.toDouble())
//        val topRight = Point((markerRect.x + markerRect.width).toDouble(), markerRect.y.toDouble())
//        val bottomRight = Point((markerRect.x + markerRect.width).toDouble(), (markerRect.y + markerRect.height).toDouble())
//        val bottomLeft = Point(markerRect.x.toDouble(), (markerRect.y + markerRect.height).toDouble())
//
//        // Create 2D image points from marker rectangle corners
//        val imagePoints = MatOfPoint2f(topLeft, topRight, bottomRight, bottomLeft)
//
//        // Define 3D coordinates of the marker corners in the real-world space
//        val objectPoints = MatOfPoint3f(
//            Point3(0.0, 0.0, 0.0),
//            Point3(1.0, 0.0, 0.0),
//            Point3(1.0, 1.0, 0.0),
//            Point3(0.0, 1.0, 0.0)
//        )
//
//        // Define the camera matrix (intrinsics) and distortion coefficients
//        val cameraMatrix = getCameraMatrix() // Replace with your camera matrix function
//        val distCoeffs = MatOfDouble(0.0, 0.0, 0.0, 0.0) // No distortion for simplicity
//
//        // Solve for pose: Calculate rotation (rvec) and translation (tvec) vectors
//        val rvec = Mat()
//        val tvec = Mat()
//        Calib3d.solvePnP(objectPoints, imagePoints, cameraMatrix, distCoeffs, rvec, tvec)
//
//        // Define a 3D object (e.g., a cube) to render
//        val cubePoints = MatOfPoint3f(
//            Point3(0.0, 0.0, 0.0),
//            Point3(0.0, 1.0, 0.0),
//            Point3(1.0, 1.0, 0.0),
//            Point3(1.0, 0.0, 0.0),
//            Point3(0.0, 0.0, -1.0),
//            Point3(0.0, 1.0, -1.0),
//            Point3(1.0, 1.0, -1.0),
//            Point3(1.0, 0.0, -1.0)
//        )
//
//        // Project the 3D object points into the 2D image space
//        val projectedPoints = MatOfPoint2f()
//        Calib3d.projectPoints(cubePoints, rvec, tvec, cameraMatrix, distCoeffs, projectedPoints)
//
//        // Draw the projected 3D object
//        val points = projectedPoints.toArray()
//
//        // Draw the base square (marker's 2D surface)
//        Imgproc.line(mat, points[0], points[1], Scalar(255.0, 0.0, 0.0), 2)
//        Imgproc.line(mat, points[1], points[2], Scalar(255.0, 0.0, 0.0), 2)
//        Imgproc.line(mat, points[2], points[3], Scalar(255.0, 0.0, 0.0), 2)
//        Imgproc.line(mat, points[3], points[0], Scalar(255.0, 0.0, 0.0), 2)
//
//        // Draw the top square (cube top surface)
//        Imgproc.line(mat, points[4], points[5], Scalar(0.0, 255.0, 0.0), 2)
//        Imgproc.line(mat, points[5], points[6], Scalar(0.0, 255.0, 0.0), 2)
//        Imgproc.line(mat, points[6], points[7], Scalar(0.0, 255.0, 0.0), 2)
//        Imgproc.line(mat, points[7], points[4], Scalar(0.0, 255.0, 0.0), 2)
//
//        // Draw connecting lines between the base and top square
//        Imgproc.line(mat, points[0], points[4], Scalar(0.0, 0.0, 255.0), 2)
//        Imgproc.line(mat, points[1], points[5], Scalar(0.0, 0.0, 255.0), 2)
//        Imgproc.line(mat, points[2], points[6], Scalar(0.0, 0.0, 255.0), 2)
//        Imgproc.line(mat, points[3], points[7], Scalar(0.0, 0.0, 255.0), 2)
//    }

    private fun overlayObjectOnMarker(mat: Mat, markerRect: Rect) {
        // Extract corner points from the detected marker rectangle
        val topLeft = Point(markerRect.x.toDouble(), markerRect.y.toDouble())
        val topRight = Point((markerRect.x + markerRect.width).toDouble(), markerRect.y.toDouble())
        val bottomRight = Point((markerRect.x + markerRect.width).toDouble(), (markerRect.y + markerRect.height).toDouble())
        val bottomLeft = Point(markerRect.x.toDouble(), (markerRect.y + markerRect.height).toDouble())

        // Create 2D image points from marker rectangle corners
        val imagePoints = MatOfPoint2f(topLeft, topRight, bottomRight, bottomLeft)

        // Define 3D coordinates of the marker corners in real-world space
        val objectPoints = MatOfPoint3f(
            Point3(0.0, 0.0, 0.0),
            Point3(1.0, 0.0, 0.0),
            Point3(1.0, 1.0, 0.0),
            Point3(0.0, 1.0, 0.0)
        )

        // Define the camera matrix and distortion coefficients
        val cameraMatrix = getCameraMatrix()
        val distCoeffs = MatOfDouble(0.0, 0.0, 0.0, 0.0) // Assuming no distortion for simplicity

        // Solve for pose: Calculate rotation (rvec) and translation (tvec) vectors
        val rvec = Mat()
        val tvec = Mat()
        Calib3d.solvePnP(objectPoints, imagePoints, cameraMatrix, distCoeffs, rvec, tvec)

        // Define a 3D cube (or any other object) to render
        val cubePoints = MatOfPoint3f(
            Point3(0.0, 0.0, 0.0), // Base
            Point3(1.0, 0.0, 0.0),
            Point3(1.0, 1.0, 0.0),
            Point3(0.0, 1.0, 0.0),
            Point3(0.0, 0.0, -1.0), // Top
            Point3(1.0, 0.0, -1.0),
            Point3(1.0, 1.0, -1.0),
            Point3(0.0, 1.0, -1.0)
        )

        // Project the 3D object points into the 2D image space
        val projectedPoints = MatOfPoint2f()
        Calib3d.projectPoints(cubePoints, rvec, tvec, cameraMatrix, distCoeffs, projectedPoints)

        // Draw the projected 3D object
        val points = projectedPoints.toArray()

        // Draw the base square (marker surface)
        Imgproc.polylines(
            mat,
            listOf(
                MatOfPoint(*points.slice(0..3).toTypedArray())
            ),
            true,
            Scalar(0.0, 255.0, 0.0),
            2
        )

        // Draw the top square (cube top)
        Imgproc.polylines(
            mat,
            listOf(
                MatOfPoint(*points.slice(4..7).toTypedArray())
            ),
            true,
            Scalar(255.0, 0.0, 0.0),
            2
        )

        // Connect the corresponding points to form vertical edges
        for (i in 0..3) {
            Imgproc.line(mat, points[i], points[i + 4], Scalar(0.0, 0.0, 255.0), 2)
        }
    }



    private fun getCameraMatrix(): Mat {
        return Mat(3, 3, CvType.CV_64F).apply {
            put(0, 0, 800.0) // fx
            put(0, 2, 640.0) // cx
            put(1, 1, 800.0) // fy
            put(1, 2, 360.0) // cy
            put(2, 2, 1.0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        private const val TAG = "MainActivity"
    }
}
