package com.ardy.stargazing.predict

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ardy.stargazing.R
import kotlinx.android.synthetic.main.activity_predict.*
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector


class PredictActivity : AppCompatActivity() {

    private val imageSize = 250
    private val REQUEST_CODE = 111
    private var predict: MutableList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_predict)

        btnChoose.setOnClickListener {
            Log.d("predicts", predict.count().toString())
            predict.forEach {
                Log.d("predicts", it)
            }
        }

        for(i in 1..160) {
            val uri = "@drawable/tengah$i"
            val imageResource = resources.getIdentifier(
                uri, null, packageName)
            val res = resources.getDrawable(imageResource)
            imageView2.setImageDrawable(res)
            val icon = BitmapFactory.decodeResource(
                resources,
                imageResource
            )
            val bitmap = Bitmap.createScaledBitmap(icon, 250, 125, false)
            var result = runObjectDetection(bitmap)
            Log.d("kiri$i", result)
        }
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE){
//            imageView2.setImageURI(data?.data) // handle chosen image
//            var bitmap: Bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, data?.data)
//            bitmap = Bitmap.createScaledBitmap(bitmap, 250, 125, false)

            val uri = "@drawable/atas1" // where myresource (without the extension) is the file
            val imageResource = resources.getIdentifier(uri, null, packageName)
            val res = resources.getDrawable(imageResource)
            imageView2.setImageDrawable(res)
            val icon = BitmapFactory.decodeResource(
                resources,
                imageResource
            )
            val bitmap = Bitmap.createScaledBitmap(icon, 250, 125, false)
            runObjectDetection(bitmap)
        }
    }

    private fun runObjectDetection(bitmap: Bitmap) : String {
        val image = TensorImage.fromBitmap(bitmap)
        // Initialization

        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(1)
            .build()
        val detector = ObjectDetector.createFromFileAndOptions(
            this, "final_50.tflite", options
        )

        val results = detector.detect(image)
        var hasil: String = ""

        results.map {
            val category = it.categories.first()
            val confidence: Int = category.score.times(100).toInt()
            textView.setText(category.label)
            predict.add(category.label)
            hasil = category.label
        }
        return hasil
    }

}