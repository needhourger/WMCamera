package com.example.wmcamera

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.example.wmcamera.utils.ImageUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "MainActivity"
class MainActivity : AppCompatActivity() {

    private lateinit var mButtonWaterMark:Button
    private lateinit var mButtonOCR:Button
    private lateinit var mImageView:ImageView

    private var mPhotoSavePath:File? = null
    private var mCapturedImage:File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mPhotoSavePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        initViews()
    }

    private fun initViews() {
        mButtonWaterMark = findViewById(R.id.button_water_mark)
        mButtonWaterMark.setOnClickListener {
            Log.d(TAG, "initViews: water mark button clicked")
            startCameraCapture()
        }

        mButtonOCR = findViewById(R.id.button_ocr)
        mButtonOCR.setOnClickListener{
            Log.d(TAG, "initViews: ocr button clicked")
        }

        mImageView = findViewById(R.id.imageView)
    }

    private fun createImageFile():File {
        val timeStamp:String = SimpleDateFormat("yyyMMdd_HHmmss").format(Date())
        if (mPhotoSavePath?.exists() != true) mPhotoSavePath?.mkdirs()
        Log.d(TAG, "createImageFile: saveDir=$mPhotoSavePath")
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            mPhotoSavePath /* directory */
        ).apply {
            mCapturedImage = this
        }
    }

    private val REQUEST_IMAGE_CAPTURE = 1
    private fun startCameraCapture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { cameraIntent ->
            cameraIntent.resolveActivity(packageManager)?.also {
                val imageFile: File? = try {
                    createImageFile()
                } catch (e:IOException) {
                    Log.e(TAG, "startCameraCapture: ",e )
                    null
                }

                imageFile?.also {
                    val photoURI:Uri = FileProvider
                        .getUriForFile(this,"com.example.wmcamera.fileprovider",it)
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoURI)
                    startActivityForResult(cameraIntent,REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            mCapturedImage?.let {
                updatePhotoGallery(mCapturedImage!!)
                val bitmap = ImageUtils.drawTextLeftBottom(this,
                    it,generateWaterMark(),30,5,R.color.watermark_white)
                if (bitmap != null) {
                    mImageView.setImageBitmap(bitmap)
                    saveBitmap(bitmap, mCapturedImage!!.name)
                }
            }
        }
    }

    private fun saveBitmap(bitmap:Bitmap,filename:String) {
        try {
            val photoFile = File(
                mPhotoSavePath,
                "wm_$filename"
            )
            Log.d(TAG, "saveBitmap to path: " + photoFile.absolutePath)
            val fileOutputStream = FileOutputStream(photoFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
            updatePhotoGallery(photoFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updatePhotoGallery(photoFile:File) {
        try {
            MediaStore.Images.Media.insertImage(baseContext.contentResolver,
                photoFile.absolutePath,photoFile.name,null)
            baseContext.sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(photoFile)
                )
            )
        } catch (e:FileNotFoundException) {
            Log.e(TAG, "updatePhotoGallery: ",e )
        }
    }

    private fun generateWaterMark():String {
        return "场地土壤污染成因与治理技术专项"
    }
}