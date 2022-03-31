package com.example.drawingappforkids

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private val openGallery: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val imageBackground = findViewById<ImageView>(R.id.backgroundImage)
                imageBackground.setImageURI(result.data?.data)
            }
        }
    private val storagePermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { isGranted ->
            for (permission in isGranted.keys) {
                if (isGranted[permission] == true) {
                    Toast.makeText(this, "Permission granted for: $permission", Toast.LENGTH_SHORT)
                        .show()
                    val pickIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGallery.launch(pickIntent)
                } else {
                    Toast.makeText(this, "Permission denied for: $permission", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    private lateinit var loadingDialog:Dialog
    private var drawingView: DrawingView? = null
    private var thickness = 20.0f
    private var selected: Int = 1
    private lateinit var layout: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_main)


// Setting default thickness

        drawingView = findViewById<DrawingView?>(R.id.drawingCanvas).also {
            it.setSize(thickness)
        }

//        Stroke Thickness

        findViewById<ImageButton>(R.id.brush_button).apply {
            setOnClickListener {
                showDialogues()
            }
        }


        findViewById<ImageButton>(R.id.palette_slct).apply {
            setOnClickListener {
                showColorDialog()
            }
        }
        findViewById<ImageButton>(R.id.backgroundImageSelect).apply {
            setOnClickListener {
                backgroundImageSelect()
            }
        }

        findViewById<ImageButton>(R.id.undo).apply {
            setOnClickListener {
                drawingView?.undo()
            }
        }
        findViewById<ImageButton>(R.id.redo).apply {
            setOnClickListener {
                drawingView?.redo()
            }
        }


    }

    // Stroke seeker dialog

    private fun showDialogues() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.brush_thickness)
        brushDialog.setTitle("Brush Size")
        val stroke: Slider = brushDialog.findViewById(R.id.brush)
        stroke.value = thickness
        stroke.addOnChangeListener { _, value, _ ->
            Log.i("message", "$value")
            drawingView?.setSize(value)
            thickness = value
        }
        brushDialog.show()

    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save -> {

                if(isWritePermissionAllowed())
                {
                    customDialog()
                    lifecycleScope.launch {
                        val flr:FrameLayout=findViewById(R.id.frameLayout)
                        saveBitmap(getBitmap(flr))
                    }

                }
//                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }

        }
    }

    //    Color chooser dialog

    private fun showColorDialog() {
        val colorDialog = Dialog(this)
        colorDialog.setContentView(R.layout.palette)
        layout = colorDialog.findViewById(R.id.color_picker)

        layout[selected].isSelected = true

        for (i in 1..layout.childCount) {
            layout[i - 1].setOnClickListener {
                colorPick(i - 1)
            }
        }
        colorDialog.window?.setGravity(Gravity.END)
        colorDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        colorDialog.show()
    }
// Picking only a single color at a time

    //    @SuppressLint("ResourceType")
    private fun colorPick(id: Int) {
        val a: Int?
        if (selected != id) {
            layout[selected].isSelected = false
            layout[id].isSelected = true
            selected = id
            a = layout[id].backgroundTintList?.defaultColor
            drawingView?.setColor(a!!)
        }

    }

    private fun backgroundImageSelect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                customAlert(
                    "Storage Access Denied",
                    "Storage Access is required to enable background Image"
                )
            }
            else {
                storagePermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            }
        }
    }

    private fun isWritePermissionAllowed():Boolean
    {
        val result=ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return result==PackageManager.PERMISSION_GRANTED
    }


    private fun getBitmap(view: View): Bitmap {
        val btmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(btmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return btmap
    }

    private suspend fun saveBitmap(mBitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO)
        {
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f = File(externalCacheDir?.absoluteFile.toString() + File.separator + "KidsDrawingApp" + System.currentTimeMillis() / 1000 + ".png")
                    runCatching {
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    }
                    result = f.absolutePath
                    runOnUiThread {
                        if (result.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File Saved successfully: $result",
                                Toast.LENGTH_LONG
                            ).show()
                            shareImage(result)
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Error while saving",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        loadingDialog.dismiss()
                    }
                } catch (error: Exception) {
                    result = ""
                    error.printStackTrace()
                }
            }
        }
        return result
    }

    private fun shareImage(result:String)
    {
        MediaScannerConnection.scanFile(this, arrayOf(result),null)
        {
            _,uri->
            val shareIntent=Intent()
            shareIntent.action=Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type="image/png"
            startActivity(Intent.createChooser(shareIntent,"share"))
        }
    }

    private fun customAlert(title: String, message: String) {
        AlertDialog.Builder(this).apply {
            setTitle(title)
            setMessage(message)
            setPositiveButton("Cancel") { dialogActivity, _ -> dialogActivity.dismiss() }
        }.create().show()
    }
    private fun customDialog()
    {
        loadingDialog= Dialog(this)
        loadingDialog.apply {
            setContentView(R.layout.loaders)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCancelable(false)
            show()
        }
    }



}

