package com.ardy.stargazing

//import org.tensorflow.lite.task.core.BaseOptions
//import org.tensorflow.lite.task.vision.classifier.Classifications
//import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ardy.stargazing.utils.Constant
import kotlinx.android.synthetic.main.activity_gaze_detection.*
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class EyeGazeActivity : AppCompatActivity() {

    private val imageSize = 250

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gaze_detection)

        val savedUri = intent.getStringExtra(Constant.EXTRA_URI)
        val fileUri = Uri.parse(savedUri)

        var bitmap: Bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, fileUri)
        bitmap = Bitmap.createScaledBitmap(bitmap, 250, 125, false)
        runObjectDetection(bitmap)

        btn_take_picture.setOnClickListener {
            finish()
        }
    }

    private fun runObjectDetection(bitmap: Bitmap) {
        val image = TensorImage.fromBitmap(bitmap)
        // Initialization

        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(1)
            .build()
        val detector = ObjectDetector.createFromFileAndOptions(
            this, "final_50.tflite", options
        )

        val results = detector.detect(image)

        results.map {
            val category = it.categories.first()
            val confidence: Int = category.score.times(100).toInt()
            tv_result.setText(category.label)
            tv_score.setText("${confidence}%")
            drawBoundingBox(it.boundingBox, bitmap)
        }

        debugPrint(results)
    }

    private fun drawBoundingBox(box: RectF, bitmap: Bitmap) {
        val bmp = bitmap.copy(Bitmap.Config.RGB_565, true)
        val canvas = Canvas(bmp)
        val boundingBox  = RectF(box.left, box.top, box.right, box.bottom)

        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 1.0f
        }

        canvas.drawRect(boundingBox, paint)
        runOnUiThread {
            imageView.setImageBitmap(bmp)
        }
    }

    private fun debugPrint(results: List<Detection>) {
        for ((i, obj) in results.withIndex()) {
            val box = obj.boundingBox

            Log.d(TAG, "Detected object: ${i} ")
            Log.d(TAG, "  boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})")

            for ((j, category) in obj.categories.withIndex()) {
                Log.d(TAG, "    Label $j: ${category.label}")
                val confidence: Int = category.score.times(100).toInt()
                Log.d(TAG, "    Confidence: ${confidence}%")

                tv_result.setText(category.label)
                tv_score.setText("${confidence}%")
            }
        }
    }

//    private fun classifyImage(image: Bitmap) {
//        try {
//            val model = Model2.newInstance(applicationContext)
//
//            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 125, 250, 3), DataType.FLOAT32)
//
//            val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(4 * 250 * 125 * 3)
//            byteBuffer.order(ByteOrder.nativeOrder())
//
//            val intValues = IntArray(250 * 125)
//            image.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)
//            var pixel = 0
//
//            Log.d("intValue", intValues.size.toString())
//            for (i in 0 until 125) {
//                for (j in 0 until 250) {
//                    val `val` = intValues[pixel++]
//                    byteBuffer.putFloat((`val` shr 16 and 0xFF) * (1f / 1))
//                    byteBuffer.putFloat((`val` shr 8 and 0xFF) * (1f / 1))
//                    byteBuffer.putFloat((`val` and 0xFF) * (1f / 1))
//                }
//            }
//
//            inputFeature0.loadBuffer(byteBuffer)
//
//            val outputs = model.process(inputFeature0)
//            val outputFeature0 = outputs.outputFeature0AsTensorBuffer
//
//            val confidences = outputFeature0.floatArray
//            // find the index of the class with the biggest confidence.
//
//            Log.d("confi", confidences.size.toString())
//
//            var maxPos = 0
//            var maxConfidence = 0f
//            for (i in confidences.indices) {
//                Log.d("confidence", confidences[i].toString())
//                if (confidences[i] > maxConfidence) {
//                    maxConfidence = confidences[i]
//                    maxPos = i
//                }
//            }
//            Log.d("classes", maxPos.toString())
//            val classes = arrayOf("atas", "bawah", "kanan", "kiri", "tengah")
//            tv_result.setText(classes[maxPos])
//
//            model.close()
//        } catch (e: IOException) {
//            Log.e("classify Error", "Exception thrown while trying to close Face Detector: $e")
//        }
//    }

    companion object {
        val TAG = "object detection"
    }
}