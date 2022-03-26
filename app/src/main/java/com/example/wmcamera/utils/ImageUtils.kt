package com.example.wmcamera.utils

import android.content.Context
import android.graphics.*
import android.media.ExifInterface
import android.util.Log
import java.io.File
import java.lang.Exception

class ImageUtils {

    companion object {
        private const val TAG = "ImageUtils"
        fun createWaterMark(
            photo: File,
            waterMask: Bitmap,
            paddingLeft: Int,
            paddingTop: Int
        ): Bitmap? {
            val angle = readPictureDegree(photo)
            val tBitmap = BitmapFactory.decodeFile(photo.path)
            val src = rotaingBitmap(angle,tBitmap)

            val wmWidth =
                if (waterMask.width + paddingLeft > src.width) (src.width - paddingLeft) else waterMask.width
            val wmHeight =
                if (waterMask.height + paddingTop > src.height) (src.height - paddingTop) else waterMask.height
            if (wmHeight < 0 || wmHeight < 0) {
                return null
            }
            val newWm = resizeBitmap(waterMask, wmWidth, wmHeight)

            return drawWaterMaskToBitmap(src, newWm, paddingLeft, paddingTop)
        }

        fun drawTextLeftBottom(
            context: Context,
            photo: File,
            text: String,
            textsize: Int,
            lineHeight: Int,
            color: Int
        ): Bitmap? {
            val angle = readPictureDegree(photo)
            val tBitmap = BitmapFactory.decodeFile(photo.path)
            val bitmap = rotaingBitmap(angle, tBitmap)

            val texts = text.split("\n")
            val wmWidth = getMaxLineWidth(texts, dp2px(context, textsize))
            val wmHeight = texts.size * dp2px(context, textsize + lineHeight)

            val paddingLeft = dp2px(context, 5)
            val paddingTop = kotlin.math.abs(bitmap.height - wmHeight)

            if (wmWidth + paddingLeft > bitmap.width || wmHeight + paddingTop > bitmap.height) {
                Log.d(TAG, "drawText: too much text")
                return null
            }

            return drawText(
                context,
                bitmap,
                texts,
                dp2px(context, textsize),
                dp2px(context, lineHeight),
                paddingLeft,
                paddingTop,
                color
            )
        }


        fun drawText(
            context: Context,
            bitmap: Bitmap,
            texts: List<String>,
            textsize: Int,
            lineHeight: Int,
            paddingLeft: Int,
            paddingTop: Int,
            color: Int
        ): Bitmap? {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = context.resources.getColor(color)
            paint.textSize = textsize.toFloat()
            return drawTextToBitmap(
                bitmap,
                texts,
                paint.textSize.toInt(),
                lineHeight,
                paddingLeft,
                paddingTop,
                paint
            )
        }

        fun rotaingBitmap(angle: Int, bitmap: Bitmap): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(angle.toFloat())
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        fun readPictureDegree(file: File): Int {
            try {
                val exifInterface = ExifInterface(file.path)
                when (exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> return 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> return 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> return 270
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return 0
        }

        private fun drawTextToBitmap(
            bitmap: Bitmap,
            texts: List<String>,
            textsize: Int,
            lineHeight: Int,
            paddingLeft: Int,
            paddingTop: Int,
            paint: Paint
        ): Bitmap? {
            var bitmapConfig = bitmap.config

            paint.isDither = true
            paint.isFilterBitmap = true
            if (bitmapConfig == null) {
                bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888
            }
            val retBitmap = bitmap.copy(bitmapConfig, true)
            val canvas = Canvas(retBitmap)

            var y = paddingTop
            for (text: String in texts) {
                canvas.drawText(text, paddingLeft.toFloat(), y.toFloat(), paint)
                Log.d(TAG, "drawTextToBitmap: $text")
                y += textsize + lineHeight
            }
            return retBitmap
        }

        private fun getMaxLineWidth(texts: List<String>, textsize: Int): Float {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.textSize = textsize.toFloat()
            var maxlen: Float = 0F
            for (text: String in texts) {
                val t = paint.measureText(text)
                if (t > maxlen) maxlen = t
            }
            return maxlen
        }

        private fun drawWaterMaskToBitmap(
            src: Bitmap,
            waterMask: Bitmap,
            paddingLeft: Int,
            paddingTop: Int
        ): Bitmap {
            val retBitmap = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(retBitmap)

            canvas.drawBitmap(src, (0).toFloat(), (0).toFloat(), null)
            canvas.drawBitmap(waterMask, paddingLeft.toFloat(), paddingTop.toFloat(), null)
            canvas.save()
            canvas.restore()
            return retBitmap
        }

        private fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
            if (bitmap.width == width && bitmap.height == height) {
                return bitmap
            }
            val originWidth = bitmap.width
            val originHeight = bitmap.height

            val matrix = Matrix()
            val scaleWith: Float = (width.toFloat() / originWidth)
            val scaleHeight: Float = (height.toFloat() / originHeight)
            matrix.postScale(scaleWith, scaleHeight)
            return Bitmap.createBitmap(bitmap, 0, 0, originWidth, originHeight, matrix, true)
        }

        fun dp2px(context: Context, dp: Float): Int {
            val scale = context.resources.displayMetrics.density
            return (dp * scale + 0.5f).toInt()
        }

        fun dp2px(context: Context, dp: Int): Int {
            val scale = context.resources.displayMetrics.density
            return (dp * scale + 0.5f).toInt()
        }

        fun convertGray(imageFile:File):Bitmap {
            val angle = readPictureDegree(imageFile)
            val tBitmap = BitmapFactory.decodeFile(imageFile.path)
            val bitmap = rotaingBitmap(angle, tBitmap)

            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0F)
            val filter = ColorMatrixColorFilter(colorMatrix)

            val paint = Paint()
            paint.setColorFilter(filter)
            val result = Bitmap.createBitmap(bitmap.width,bitmap.height,Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)

            canvas.drawBitmap(bitmap,0f,0f,paint)
            return result
        }
    }

}