package com.example.wmcamera

import android.app.ProgressDialog
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.AsyncTask.execute
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.example.wmcamera.utils.ImageUtils
import com.example.wmcamera.utils.Tesseract
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

private const val TAG = "MainActivity"
class MainActivity : AppCompatActivity() {

    private lateinit var mButtonWaterMark:Button
    private lateinit var mButtonOCR:Button
    private lateinit var mImageView:ImageView
    private lateinit var mProgressBar:ProgressBar

    private var mPhotoSavePath:File? = null
    private var mCapturedImage:File? = null
    private var mCropedImage:Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mPhotoSavePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        initTesseractModel()
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
            startCropImage()
        }

        mProgressBar = findViewById(R.id.pbProgress)
        mProgressBar.visibility = View.GONE

        mImageView = findViewById(R.id.imageView)
        mImageView.setOnClickListener {
            if (mCropedImage != null) {
                if (!Tesseract.isInited) {
                    Toast.makeText(this,"OCR引擎尚未加载完成,请稍候尝试",Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(mCropedImage!!))
                mProgressBar.visibility = View.VISIBLE
                updateButtonEnable(false)
                Thread {
                    val result = Tesseract.doOCR(bitmap)
                    onOCRResult(result)
                }.start()
            }
        }
    }

    private fun updateButtonEnable(enabled: Boolean) {
        mButtonOCR.isEnabled = enabled
        mButtonWaterMark.isEnabled = enabled
    }

    private fun onOCRResult(result: String?) {
        runOnUiThread {
            mProgressBar.visibility = View.GONE
            updateButtonEnable(true)
            if (result != null) {
                showResultDialog(result)
            } else {
                Toast.makeText(this,"OCR未识别到结果",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showResultDialog(str:String) {
        val view = LayoutInflater.from(this).inflate(R.layout.ocr_result,null)
        val textview = view.findViewById<TextView>(R.id.tvResult)
        textview.text = str

        val dialog = AlertDialog.Builder(this)
            .setTitle("OCR 文字识别结果")
            .setView(view)
            .setIcon(R.mipmap.ic_launcher_round)
            .setPositiveButton("确定") { p0, p1 -> }
            .setNeutralButton("复制"){ p0,p1->
                val clipBoard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipdata:ClipData = ClipData.newPlainText("ocr",str)
                clipBoard.setPrimaryClip(clipdata)
            }.create()
        dialog.show()
    }

    private fun initTesseractModel() {
        Thread {
            Tesseract.extractData(baseContext)
            Tesseract.loadDataModel(baseContext)
        }.start()
    }

    private fun startCropImage() {
        CropImage.activity()
            .setGuidelines(CropImageView.Guidelines.ON)
            .start(this)
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
        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                mCropedImage = result.uri
                mImageView.setImageURI(mCropedImage)
                Toast.makeText(this,"点击图片进行OCR识别",Toast.LENGTH_SHORT).show()
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val e = result.error
                Log.e(TAG, "onActivityResult: ",e )
                e.printStackTrace()
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