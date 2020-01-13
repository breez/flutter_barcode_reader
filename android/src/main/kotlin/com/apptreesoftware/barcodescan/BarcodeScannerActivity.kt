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
import android.content.ClipboardManager
import com.yourcompany.barcodescan.R
import android.view.LayoutInflater
import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.Button
import android.view.View
import android.content.ClipData

class BarcodeScannerActivity : Activity(), ZXingScannerView.ResultHandler {

    lateinit var scannerView: me.dm7.barcodescanner.zxing.ZXingScannerView
    private var clipboard: ClipboardManager? = null
    private var pasteText: String? = null

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
        var pasteBtn = findViewById<Button>(R.id.PASTE)
        val bundle: Bundle? = intent.extras
        bundle?.let {
            bundle.apply {
                //Intent with data
                val pasteButtonText: String? = getString("pasteButtonText")
                if (pasteButtonText != null) {
                    pasteBtn.text = pasteButtonText
                }

            }
        }
        if (Build.VERSION.SDK_INT >= 29) {
            pasteText = "GET_CLIPBOARD_DATA"
        } else {
            clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
            val clip = clipboard?.primaryClip
            pasteText = clip?.getItemAt(0)?.text.toString()
        }
        val intent = Intent()
        pasteBtn.setOnClickListener {
            intent.putExtra("SCAN_RESULT", pasteText)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
        if (pasteText == null) {
            pasteBtn.setVisibility(View.INVISIBLE)
        }

    }

    override fun onResume() {
        super.onResume()
        scannerView.setResultHandler(this)
        // start camera immediately if permission is already given
        if (!requestCameraAccessIfNecessary()) {
            scannerView.startCamera()
        }
        if (Build.VERSION.SDK_INT < 29) {
            // show Paste Invoice button if there's a valid item in clipboard
            clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
            val clip = clipboard?.primaryClip
            pasteText = clip?.getItemAt(0)?.text.toString()
            if (pasteText != null) {
                var pasteBtn = findViewById<Button>(R.id.PASTE)
                pasteBtn.setVisibility(View.VISIBLE)
            }
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
