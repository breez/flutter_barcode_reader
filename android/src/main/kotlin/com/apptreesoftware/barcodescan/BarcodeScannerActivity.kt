package com.apptreesoftware.barcodescan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView
import com.yourcompany.barcodescan.R
import android.view.LayoutInflater
import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.Button
import android.view.View

import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.collections.ArrayList
import android.R.attr.data
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import android.graphics.Bitmap
import android.database.Cursor
import android.provider.MediaStore
import android.net.Uri
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import android.R.attr.data

class BarcodeScannerActivity : Activity(), ZXingScannerView.ResultHandler {

    lateinit var scannerView: me.dm7.barcodescanner.zxing.ZXingScannerView
    private val SELECT_PHOTO = 12345

    companion object {
        val REQUEST_TAKE_PHOTO_CAMERA_PERMISSION = 100

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.BarcodeScannerTheme)
        super.onCreate(savedInstanceState)
        title = ""
        scannerView = ZXingScannerView(this)
        scannerView.setAutoFocus(true)
        scannerView.setAspectTolerance(0.5f)
        setContentView(scannerView)
        val inflator = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val v = inflator.inflate(com.yourcompany.barcodescan.R.layout.layout, null)
        actionBar.customView = v
        actionBar.setDisplayShowCustomEnabled(true)
        var flashBtn = findViewById<Button>(R.id.TOGGLE_FLASH)
        flashBtn.setOnClickListener {
            scannerView.flash = !scannerView.flash
            if (!scannerView.flash) {
                flashBtn.text = "FLASH ON"
                val drawable = ContextCompat.getDrawable(this, R.drawable.ic_flash_on)
                flashBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawable, null)
            } else {
                flashBtn.text = "FLASH OFF"
                val drawable = ContextCompat.getDrawable(this, R.drawable.ic_flash_off)
                flashBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawable, null)
            }
            this.invalidateOptionsMenu()
        }
        var selectImageBtn = findViewById<Button>(R.id.SELECT_IMAGE)
        val intent = Intent()
        selectImageBtn.setOnClickListener {
            val necessaryPermissions = arrayOf<String>(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (arePermissionsGranted(necessaryPermissions)) {
                val photoPickerIntent = Intent(Intent.ACTION_PICK)
                photoPickerIntent.setType("image/*")
                startActivityForResult(photoPickerIntent, SELECT_PHOTO)
            } else {
                requestPermissionsCompat(necessaryPermissions, SELECT_PHOTO)
            }
        }
    }

    private fun arePermissionsGranted(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) !== PackageManager.PERMISSION_GRANTED) return false
        }
        return true
    }

    private fun requestPermissionsCompat(permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(this, permissions, requestCode)
    }

    override protected fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_PHOTO && resultCode == RESULT_OK && data != null) {
            // Let's read picked image data - its URI
            val pickedImage: Uri = data.getData()
            // Let's read picked image path using content resolver
            val filePath = arrayOf<String>(MediaStore.Images.Media.DATA)
            val cursor: Cursor = getContentResolver().query(pickedImage, filePath, null, null, null)
            cursor.moveToFirst()
            val imagePath: String = cursor.getString(cursor.getColumnIndex(filePath[0]))

            val options = Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            val bitmap: Bitmap = BitmapFactory.decodeFile(imagePath, options)

            val w = bitmap.width
            val h = bitmap.height
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            val hints = Hashtable<DecodeHintType, Any>()
            val decodeFormats = ArrayList<BarcodeFormat>()
            decodeFormats.add(BarcodeFormat.QR_CODE)
            hints[DecodeHintType.POSSIBLE_FORMATS] = decodeFormats
            hints[DecodeHintType.CHARACTER_SET] = "utf-8"
            hints[DecodeHintType.TRY_HARDER] = true

            try {
                val intent = Intent()
                val decodeResult = MultiFormatReader().decode(binaryBitmap, hints)
                intent.putExtra("SCAN_RESULT", decodeResult.text)
                setResult(Activity.RESULT_OK, intent)
                finish()
            } catch (e: NotFoundException) {
                val intent = Intent()
                intent.putExtra("SCAN_RESULT", "")
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        scannerView.setResultHandler(this)
        // start camera immediately if permission is already given
        if (!requestCameraAccessIfNecessary()) {
            scannerView.startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        scannerView.stopCamera()
    }

    override fun handleResult(result: Result?) {
        val intent = Intent()
        intent.putExtra("SCAN_RESULT", result.toString())
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    fun finishWithError(errorCode: String) {
        val intent = Intent()
        intent.putExtra("ERROR_CODE", errorCode)
        setResult(Activity.RESULT_CANCELED, intent)
        finish()
    }

    private fun requestCameraAccessIfNecessary(): Boolean {
        val array = arrayOf(Manifest.permission.CAMERA)
        if (ContextCompat
                        .checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, array,
                    REQUEST_TAKE_PHOTO_CAMERA_PERMISSION)
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_TAKE_PHOTO_CAMERA_PERMISSION -> {
                if (PermissionUtil.verifyPermissions(grantResults)) {
                    scannerView.startCamera()
                } else {
                    finishWithError("PERMISSION_NOT_GRANTED")
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }
}

object PermissionUtil {

    /**
     * Check that all given permissions have been granted by verifying that each entry in the
     * given array is of the value [PackageManager.PERMISSION_GRANTED].

     * @see Activity.onRequestPermissionsResult
     */
    fun verifyPermissions(grantResults: IntArray): Boolean {
        // At least one result must be checked.
        if (grantResults.size < 1) {
            return false
        }

        // Verify that each required permission has been granted, otherwise return false.
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
}
