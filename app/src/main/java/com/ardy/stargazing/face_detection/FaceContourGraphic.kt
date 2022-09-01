package com.ardy.stargazing.face_detection

import android.graphics.*
import androidx.annotation.ColorInt
import com.ardy.stargazing.MainActivity
import com.ardy.stargazing.camerax.GraphicOverlay
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour

class FaceContourGraphic(
    overlay: GraphicOverlay,
    private val face: Face,
    private val imageRect: Rect
) : GraphicOverlay.Graphic(overlay) {

    private val facePositionPaint: Paint
    private val idPaint: Paint
    private val boxPaint: Paint
    private lateinit var mainActivity: MainActivity

    init {
        val selectedColor = Color.WHITE

        facePositionPaint = Paint()
        facePositionPaint.color = selectedColor

        idPaint = Paint()
        idPaint.color = selectedColor

        boxPaint = Paint()
        boxPaint.color = selectedColor
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = BOX_STROKE_WIDTH

        mainActivity = MainActivity()
    }

    private fun Canvas.drawFace(facePosition: Int, @ColorInt selectedColor: Int) {
        val contour = face.getContour(facePosition)
        val path = Path()
        contour?.points?.forEachIndexed { index, pointF ->
            if (index == 0) {
                path.moveTo(
                    translateX(pointF.x),
                    translateY(pointF.y)
                )
            }
            path.lineTo(
                translateX(pointF.x),
                translateY(pointF.y)
            )
        }
        val paint = Paint().apply {
            color = selectedColor
            style = Paint.Style.STROKE
            strokeWidth = BOX_STROKE_WIDTH
        }

        drawPath(path, paint)
    }

    override fun draw(canvas: Canvas?) {
        val rect = calculateRect(
            imageRect.height().toFloat(),
            imageRect.width().toFloat(),
            face.boundingBox
        )
//        canvas?.drawRect(rect, boxPaint)

        val leftContour = face.getContour(FaceContour.LEFT_EYE)
        leftContour?.points?.forEach { point ->
            val px = translateX(point.x)
            val py = translateY(point.y)
            canvas?.drawCircle(px, py, FACE_POSITION_RADIUS, facePositionPaint)
        }

        val rightContour = face.getContour(FaceContour.RIGHT_EYE)
        rightContour?.points?.forEach { point ->
            val px = translateX(point.x)
            val py = translateY(point.y)
            canvas?.drawCircle(px, py, FACE_POSITION_RADIUS, facePositionPaint)
        }

        // left eye
        canvas?.drawFace(FaceContour.LEFT_EYE, Color.BLACK)

        // right eye
        canvas?.drawFace(FaceContour.RIGHT_EYE, Color.DKGRAY)
    }



    companion object {
        private const val BOX_STROKE_WIDTH = 5.0f
        private const val FACE_POSITION_RADIUS = 4.0f
    }

}