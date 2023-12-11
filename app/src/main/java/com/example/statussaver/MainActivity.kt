package com.example.statussaver

import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var rvstatus: RecyclerView
    private lateinit var statusList: ArrayList<StatusModel>
    private lateinit var statusAdapter: StatusAdapter
    private lateinit var permissionLauncher: ActivityResultLauncher<Intent>

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvstatus = findViewById(R.id.rvstatus)
        statusList = ArrayList()

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { permissionResult ->
                if (permissionResult.resultCode == Activity.RESULT_OK) {
                    val data = permissionResult.data
                    if (data != null) {
                        val treeUri = data.data

                        val shpref = getSharedPreferences("DATA_PATH", MODE_PRIVATE)
                        val myEdit = shpref.edit()
                        myEdit.putString("PATH", treeUri.toString())
                        myEdit.apply()

                        if (treeUri != null) {
                            contentResolver.takePersistableUriPermission(
                                treeUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            val fileDoc = DocumentFile.fromTreeUri(applicationContext, treeUri)

                            for (file: DocumentFile in fileDoc!!.listFiles()) {
                                if (!file.name!!.endsWith(".nomedia")) {
                                    val statusModel = StatusModel(file.name!!, file.uri.toString())
                                    statusList.add(statusModel)
                                }
                            }
                            setUpRecyclerView(statusList)
                        }
                    }
                }
            }

        val result = readDataFromPrefs()

        if (result) {
            val spref = getSharedPreferences("DATA_PATH", MODE_PRIVATE)
            val uriPath = spref.getString("PATH", "")
            contentResolver.takePersistableUriPermission(
                Uri.parse(uriPath),
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            if (uriPath != null) {
                val fileDoc = DocumentFile.fromTreeUri(applicationContext, Uri.parse(uriPath))
                for (file: DocumentFile in fileDoc!!.listFiles()) {
                    if (!file.name!!.endsWith(".nomedia")) {
                        val ststusModel = StatusModel(file.name!!, file.uri.toString())
                        statusList.add(ststusModel)
                    }
                }
                setUpRecyclerView(statusList)
            }
        } else {
            getFolderPermission()
        }
    }

    private fun setUpRecyclerView(statusList: ArrayList<StatusModel>) {
        statusAdapter = applicationContext?.let {
            StatusAdapter(
                it, statusList
            ) { selectedStatusItem: StatusModel -> listItemClicked(selectedStatusItem) }
        }!!

        rvstatus.apply {
            setHasFixedSize(true)
            layoutManager = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
            adapter = statusAdapter
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getFolderPermission() {
        val storageManager = application.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val intent = storageManager.primaryStorageVolume.createOpenDocumentTreeIntent()
        val targetDirectory = "Android%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2FStatuses"

        if (intent.hasExtra("android.provider.extra.INITIAL_URI")) {
            val uri = intent.data

            if (uri != null) {
                val scheme = uri.toString().replace("/root/", "/tree/") + "3A$targetDirectory"
                val modifiedUri = Uri.parse(scheme)
                intent.putExtra("android.provider.extra.INITIAL_URI", modifiedUri)
                intent.putExtra("android.content.extra.SHOW_ADVANCED", true)

                permissionLauncher.launch(intent)
            }
        }
    }

    private fun listItemClicked(status: StatusModel) {
        val dialog = Dialog(this@MainActivity)
        dialog.setContentView(R.layout.dialog_sheet)
        dialog.show()
        val downoadbtn = dialog.findViewById<Button>(R.id.downloadbtn)

        downoadbtn.setOnClickListener {
            dialog.dismiss()
            saveFile(status)
        }
    }

    private fun saveFile(status: StatusModel) {
        if (status.fileUri.endsWith(".mp4")) {
            val inputStream = contentResolver.openInputStream(Uri.parse(status.fileUri))
            val fileName = "${System.currentTimeMillis()}.mp4"
            try {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_MOVIES + "/Videos/"
                    )
                }
                val uri = contentResolver.insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    values
                )
                val outputStream: OutputStream = uri?.let { contentResolver.openOutputStream(it) }!!
                if (inputStream != null) {
                    outputStream.write(inputStream.readBytes())
                }
                outputStream.close()
                Toast.makeText(applicationContext, "Video Saved", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(applicationContext, e.toString(), Toast.LENGTH_SHORT).show()
            } finally {
                inputStream?.close()
            }
        } else {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, Uri.parse(status.fileUri))
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, Uri.parse(status.fileUri))
            }

            val fileName = "${System.currentTimeMillis()}.jpg"
            var fos: OutputStream?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.also { resolver ->
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }
                    val imageUri: Uri? =
                        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    fos = imageUri?.let { resolver.openOutputStream(it) }
                }
            } else {
                val imageDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val image = File(imageDir, fileName)
                fos = FileOutputStream(image)
            }
            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                Toast.makeText(applicationContext, "Image Saved", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun readDataFromPrefs(): Boolean {
        val pref = getSharedPreferences("DATA_PATH", MODE_PRIVATE)
        val uriPath = pref.getString("PATH", "")

        if (uriPath != null) {
            if (uriPath.isEmpty()) {
                return false
            }
        }
        return true
    }
}