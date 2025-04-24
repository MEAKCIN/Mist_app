package com.example.myapplication2

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects

class PhotographFragment : Fragment() {

    private lateinit var photoImageView: ImageView
    private lateinit var buttonTakePhoto: Button
    private lateinit var buttonUploadPhoto: Button
    private lateinit var buttonDetectEmotion: Button

    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var selectImageLauncher: ActivityResultLauncher<String>

    private var latestTmpUri: Uri? = null
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerActivityResultLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_photograph, container, false)
        photoImageView = view.findViewById(R.id.photoImageView)
        buttonTakePhoto = view.findViewById(R.id.buttonTakePhoto)
        buttonUploadPhoto = view.findViewById(R.id.buttonUploadPhoto)
        buttonDetectEmotion = view.findViewById(R.id.buttonDetectEmotion)

        buttonTakePhoto.setOnClickListener {
            checkCameraPermissionAndTakePhoto()
        }
        buttonUploadPhoto.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }
        buttonDetectEmotion.setOnClickListener {
            // Choose the image URI: prefer selected image; otherwise, use camera photo.
            val imageUriToProcess = selectedImageUri ?: latestTmpUri

            if (imageUriToProcess != null) {
                // Save the URI in MainActivity for further emotion detection use.
                (activity as MainActivity).photoUri = imageUriToProcess
                Toast.makeText(requireContext(), "Photo saved for emotion detection", Toast.LENGTH_SHORT).show()
                Log.d("EmotionDetection", "Photo URI saved: $imageUriToProcess")
                // Proceed with emotion detection here.
            } else {
                Toast.makeText(requireContext(), "No image available to detect emotion", Toast.LENGTH_SHORT).show()
                Log.w("EmotionDetection", "Detect button clicked but no image URI found")
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateDetectButtonState()
    }

    private fun registerActivityResultLaunchers() {
        requestCameraPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    Log.d("Permission", "Camera permission granted")
                    takePhoto()
                } else {
                    Log.d("Permission", "Camera permission denied")
                    Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
                }
            }

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                latestTmpUri?.let { uri ->
                    Log.d("TakePhoto", "Image captured successfully: $uri")
                    selectedImageUri = null
                    photoImageView.setImageURI(uri)
                    updateDetectButtonState()
                } ?: Log.e("TakePhoto", "latestTmpUri is null after taking picture")
            } else {
                Log.e("TakePhoto", "Image capture failed or was cancelled")
            }
        }

        selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                Log.d("SelectImage", "Image selected successfully: $it")
                selectedImageUri = it
                latestTmpUri = null
                photoImageView.setImageURI(it)
                updateDetectButtonState()
            } ?: Log.d("SelectImage", "No image selected")
        }
    }

    private fun checkCameraPermissionAndTakePhoto() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("Permission", "Camera permission already granted")
                takePhoto()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Log.d("Permission", "Showing camera permission rationale")
                Toast.makeText(requireContext(), "Camera access is needed to take photos", Toast.LENGTH_LONG).show()
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                Log.d("Permission", "Requesting camera permission")
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun takePhoto() {
        try {
            latestTmpUri = getTmpFileUri()
            Log.d("TakePhoto", "Launching camera with URI: $latestTmpUri")
            takePictureLauncher.launch(latestTmpUri)
        } catch (e: Exception) {
            Log.e("TakePhoto", "Error launching camera: ${e.message}", e)
            Toast.makeText(requireContext(), "Error starting camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getTmpFileUri(): Uri {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir: File? = requireContext().cacheDir

        if (storageDir == null) {
            Log.e("FileProvider", "Cache directory is null")
            throw RuntimeException("Cache directory is null, cannot create temp file")
        }

        val tmpFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        Log.d("FileProvider", "Temporary file created at: ${tmpFile.absolutePath}")
        val authority = "${requireContext().packageName}.fileprovider"
        return FileProvider.getUriForFile(Objects.requireNonNull(requireContext(), "Context cannot be null"), authority, tmpFile)
    }

    private fun updateDetectButtonState() {
        val isEnabled = latestTmpUri != null || selectedImageUri != null
        buttonDetectEmotion.isEnabled = isEnabled
        Log.d("UIState", "Detect button enabled: $isEnabled")
    }
}