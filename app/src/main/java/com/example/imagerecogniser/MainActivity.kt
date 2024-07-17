package com.example.imagerecogniser

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.JsonObject
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private lateinit var inputImageBtn: Button
    private lateinit var recogniseBtn: Button
    private lateinit var imageIv: ImageView

    private lateinit var imageTV: TextView


    private companion object {
        private const val CAMERA_REQUEST_CODE = 100
        private const val TITLE = MediaStore.Images.Media.TITLE
        private const val DESCRIPTION = MediaStore.Images.Media.DESCRIPTION
    }

    private var imageUri: Uri? = null
    private lateinit var cameraPermissions: Array<String>

    private lateinit var textRecognizer: TextRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        inputImageBtn = findViewById(R.id.takeimage)
        recogniseBtn = findViewById(R.id.recoText)
        imageIv = findViewById(R.id.ImageView)

        imageTV = findViewById(R.id.textView)

        cameraPermissions = arrayOf(android.Manifest.permission.CAMERA)

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        inputImageBtn.setOnClickListener {
            showInputImageDialog()
        }
        recogniseBtn.setOnClickListener {
            if (imageUri == null) {
                showToast("Pick Image First")
            } else {
                recogniseTextFromImage()
            }
        }
    }

    private fun recogniseTextFromImage() {
        try {
            val inputImage = InputImage.fromFilePath(this, imageUri!!)
            textRecognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    val recognisedText = text.text
                    val retrofit = RetrofitClient.getInstance()

                    val apiInterface = retrofit.create(Genai::class.java)

                    val message = recognisedText.toString()

                    val call: Call<JsonObject> = apiInterface.getResponse(message)

                    call.enqueue(object : Callback<JsonObject> {
                        override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                            if (response.isSuccessful) {
                                val responseBody = response.body()
                                Log.d("MainActivity", "Response: $responseBody")

                                imageTV.text = responseBody.toString().split(".")[0]
                            } else {
                                Log.d("MainActivity", "Response not successful")
                            }
                        }

                        override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                            try {
                                t.printStackTrace()
                                imageTV.text = t.toString()
                                Log.d("MainActivity", "Error: ${t.message}")
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.d("MainActivity", "Exception in error handling: ${e.message}")
                            }

                        }
                    })
                    imageTV.setText(message)
                }
                .addOnFailureListener { e ->
                    showToast("Failed to recognize text: ${e.message}")
                }
        } catch (e: Exception) {
            showToast("Error: ${e.message}")
        }
    }

    private fun retroCall(recognisedText : String) {





    }

    private fun showInputImageDialog() {
        val options = arrayOf("Camera", "Gallery")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pick Image From")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> {
                    if (checkCameraPermission()) {
                        pickImageCamera()
                    } else {
                        requestCameraPermission()
                    }
                }
                1 -> {
                    pickImageGallery()
                }
            }
        }
        builder.show()
    }

    private fun pickImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                imageUri = data?.data
                imageIv.setImageURI(imageUri)
            } else {
                showToast("Cancelled")
            }
        }

    private fun pickImageCamera() {
        val values = ContentValues().apply {
            put(TITLE, "Sample Title")
            put(DESCRIPTION, "Sample Description")
        }
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                imageIv.setImageURI(imageUri)
            } else {
                showToast("Cancelled")
            }
        }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (cameraAccepted) {
                        pickImageCamera()
                    } else {
                        showToast("Camera permission is required")
                    }
                }
            }
        }
    }
}
