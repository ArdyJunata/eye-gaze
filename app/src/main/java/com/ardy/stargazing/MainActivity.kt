package com.ardy.stargazing

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ardy.stargazing.camerax.CameraManager
import com.ardy.stargazing.utils.Constant
import com.ardy.stargazing.utils.cropImage
import com.ardy.stargazing.utils.rotateBitmap
import com.ardy.stargazing.utils.storeImage
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var viewFinderRect: Rect
    private lateinit var outputDirectory: File

    private lateinit var mContext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        outputDirectory = getOutputDirectory(this)

        setBrightness()
        createCameraManager()
        checkForPermission()
        setFinderRect()

        camera_capture_button.setOnClickListener {
            takePicture()
        }

        observeOpenEye()

        mContext = this
    }

    override fun onResume() {
        super.onResume()
        progressBar.visibility = View.GONE
        loadingBackground.visibility = View.GONE
        Settings.System.putInt(this.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 255);
    }

    override fun onPause() {
        super.onPause()
        Settings.System.putInt(this.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 100);
    }

    private fun setFinderRect() {
        view_finder.post {
            viewFinderRect = Rect(
                viewFinderWindow.left,
                viewFinderWindow.top,
                viewFinderWindow.right,
                viewFinderWindow.bottom
            )

            viewFinderBackground.setViewFinderRect(viewFinderRect)
        }
    }

    private fun setBrightness() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }

        // set max brighness
        Settings.System.putInt(this.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 255);
    }

    private fun checkForPermission() {
        if (allPermissionsGranted()) {
            cameraManager.startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                cameraManager.startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    private fun createCameraManager() {
        cameraManager = CameraManager(
            this,
            view_finder,
            this,
            graphicOverlay_finder
        )
    }

    private fun observeOpenEye() {
        cameraManager.isEyeOpen.observe(this, androidx.lifecycle.Observer {
            if (it) {
                viewFinderWindow.setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.shape_green
                    )
                )
            } else {
                viewFinderWindow.setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.shape_red
                    )
                )
            }
            Log.d(TAG, "open gak $it")
        })
    }

    private fun takePicture() {

        if (cameraManager.isEyeOpen.value != false) {
            progressBar.visibility = View.VISIBLE
            loadingBackground.visibility = View.VISIBLE
            Toast.makeText(this, "take a picture!", Toast.LENGTH_SHORT).show()
            cameraManager.imageCapture.let {
                // Create output file to hold the image
                val photoFile = createFile(outputDirectory, FILENAME, PHOTO_EXTENSION)

                // Create output options object which contains file + metadata
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                    .build()

                cameraManager.imageCapture.takePicture(
                    outputOptions, cameraManager.cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)

                            val bitmap : Bitmap = MediaStore.Images.Media.getBitmap(
                                this@MainActivity.contentResolver,
                                savedUri
                            )
                            val cropped = cropImage(
                                rotateBitmap(savedUri.path!!, bitmap),
                                Size(view_finder.width, view_finder.height),
                                viewFinderRect
                            )
                            val newUri = storeImage(cropped, photoFile)

                            Intent(this@MainActivity, EyeGazeActivity::class.java).apply {
                                putExtra(Constant.EXTRA_URI, newUri.toString())
                                startActivity(this)
                            }

                            Log.d(TAG, "Photo capture succeeded: $savedUri")
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                        }
                    }
                )
            }
        } else {
            Toast.makeText(this, "eye not found !", Toast.LENGTH_SHORT).show()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        const val TAG = "eye_gaze"

        private fun createFile(baseFolder: File, format: String, extension: String) =
            File(
                baseFolder, SimpleDateFormat(format, Locale.US)
                    .format(System.currentTimeMillis()) + extension
            )

        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
            }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }
    }

}