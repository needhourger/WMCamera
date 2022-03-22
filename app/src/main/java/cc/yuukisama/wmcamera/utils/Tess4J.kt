package cc.yuukisama.wmcamera.utils

import android.util.Log
import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.TesseractException
import java.io.File

class Tess4J {
    companion object {
        private const val TAG = "Tesseract"
        private val instance = Tesseract()

        @JvmStatic
        fun doOCR(imageFile:File):String? {
            try {
                val res = instance.doOCR(imageFile);
                Log.d(TAG, "doOCR: $res")
                return res
            } catch (e:TesseractException){
                Log.e(TAG, "doOCR: ",e )
            }
            return null
        }
    }
}