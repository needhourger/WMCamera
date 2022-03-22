package cc.yuukisama.wmcamera.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Tess4A(private val mContext:Context) {
    companion object {
        private const val TAG = "Tesseract"
        private const val debug = false
        private const val CHINESE = "chi_sim"
        private const val ENGLISH = "eng"
    }

    private var instance:TessBaseAPI = TessBaseAPI()

    init {
        if (!checkDataExists("tessdata")) {
            copyFile("tessdata")
        }
        instance.setDebug(debug)
        val languagePath = mContext.getExternalFilesDir(null)?.path + "/"
        instance.init(languagePath, CHINESE)
        instance.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
    }

    private fun checkDataExists(path:String):Boolean {
        val data = mContext.getExternalFilesDir(path)
        if (data != null) {
            return data.exists()
        }
        return false
    }

    private fun copyFile(path:String) {
        var path = path
        val newPath:String = mContext.getExternalFilesDir(null)?.path + "/"
        try {
            val files = mContext.assets.list(path)
            if (files != null && files.isNotEmpty()) {
                val file = File(newPath+path)
                file.mkdirs();
                for (file in files){
                    path = "$path/$file"
                    copyFile(path)
                    path = path.substring(0,path.lastIndexOf('/'))
                }
            } else {
                val inputStream = mContext.assets.open(path)
                val outStream = FileOutputStream(File(newPath+path))
                val buffer = ByteArray(1024)
                var count = 0
                while (true) {
                    count++
                    val len = inputStream.read(buffer)
                    if (len == -1) {
                        break
                    }
                    outStream.write(buffer,0,len)
                }
                Log.d(TAG, "copyFile: $newPath $path")
                inputStream.close()
                outStream.close()
            }
        } catch (e:IOException) {
            Log.e(TAG, "copyFile: ",e )
        }
    }

    fun doOCR(bitmap: Bitmap):String {
        instance.setImage(bitmap)
        val result = instance.utF8Text
        Log.d(TAG, "doOCR: $result")
        return result
    }

    fun doOCR(imageFile:File):String {
        instance.setImage(imageFile)
        val result = instance.utF8Text
        Log.d(TAG, "doOCR: $result")
        return result
    }
}