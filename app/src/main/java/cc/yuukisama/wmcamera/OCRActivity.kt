package cc.yuukisama.wmcamera

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.camera_view.view.*

class OCRActivity : AppCompatActivity() {
    private val mContext = this
    private lateinit var mCameraView: CameraView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ocr)

        mCameraView = findViewById(R.id.camera_view)
        mCameraView.ocr_button.setImageDrawable(mContext.getDrawable(R.drawable.photo_gallary))
    }
}