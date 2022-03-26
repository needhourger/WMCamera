package com.example.wmcamera.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception

class Tesseract {

    companion object {
        private const val TAG = "Tesseract"
        private val instance = TessBaseAPI()
        private const val CHINESE = "chi_sim"
        private const val ENGLISH = "eng"

        private var inited = false
        val isInited get() = this.inited


        @JvmStatic
        fun extractData(context: Context) {
            val targetPath = context.getExternalFilesDir("tessdata")
//            if (targetPath != null && targetPath.exists()) {
//                Log.d(TAG, "extractData: directory exists")
//                return
//            }
            copyFile("tessdata",context)
        }

        private fun copyFile(path:String,mContext:Context) {
            var path = path
            val newPath:String = mContext.getExternalFilesDir(null)?.path + "/"
            Log.d(TAG, "copyFile: $path")
            try {
                val files = mContext.assets.list(path)
                if (files != null && files.isNotEmpty()) {
                    val file = File(newPath+path)
                    file.mkdirs();
                    for (f in files){
                        path = "$path/$f"
                        copyFile(path,mContext)
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
            } catch (e: IOException) {
                Log.e(TAG, "copyFile: ",e )
            }
        }

        @JvmStatic
        fun loadDataModel(context: Context) {
            val dataPath = context.getExternalFilesDir(null)?.path
            instance.init(dataPath,"$CHINESE+$ENGLISH");
            inited = true
        }

        @JvmStatic
        fun doOCR(bitmap:Bitmap):String? {
            try {
                instance.setImage(bitmap)
                return instance.utF8Text
            } catch (e:Exception) {
                Log.e(TAG, "doOCR: ",e )
                e.printStackTrace()
            }
            return null
        }

        @JvmStatic
        fun release() {
            instance.recycle()
        }
    }

}