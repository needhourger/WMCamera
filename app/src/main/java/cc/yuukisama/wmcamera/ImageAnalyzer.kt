package cc.yuukisama.wmcamera;

import android.graphics.*
import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import java.io.ByteArrayOutputStream

class ImageAnalyzer(private val listener: ImageAnalyzer?) :
    ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
//            val buffer = image.planes[0].buffer
//            val data = buffer.toByteArray()
//            val pixels = data.map { it.toInt() and 0xFF }
//            val luma = pixels.average()


        image.close()
    }

    fun toBitmap(image:ImageProxy):Bitmap{
        val yBuffer = image.planes[0].buffer
        val vuBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()

        val nv21 = ByteArray(ySize + vuSize)

        yBuffer.get(nv21,0,ySize)
        vuBuffer.get(nv21,ySize,vuSize)

        val yuvImage = YuvImage(nv21,ImageFormat.NV21,image.width,image.height,null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0,0,yuvImage.width,yuvImage.height),50,out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.size)
    }
}



