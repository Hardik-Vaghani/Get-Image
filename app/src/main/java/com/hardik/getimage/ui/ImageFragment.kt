package com.hardik.getimage.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.hardik.getimage.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageFragment : Fragment() {

    companion object { fun newInstance() = ImageFragment() }
    private val TAG = ImageFragment::class.java.simpleName

    private lateinit var imageView: ImageView
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    private lateinit var photoFile: File
    private lateinit var photoUri: Uri

    private lateinit var viewModel: ImageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the ActivityResultLauncher here
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var allGranted = true
            permissions.entries.forEach { entry ->
                if (!entry.value) {
                    allGranted = false
                }
            }

            if (allGranted) {
                // All permissions are granted, proceed with your functionality
                Toast.makeText(requireContext(), "All permissions granted ✔️", Toast.LENGTH_SHORT).show()
                showImageSourceDialog()
            } else {
                // Handle the case where permissions are not granted
                Toast.makeText(requireContext(), "Permissions denied ✖️", Toast.LENGTH_SHORT).show()
            }
        }

        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    Glide.with(this)
                        .load(uri)
                        .into(imageView)
                    Log.e(TAG, "Selected image URI: $uri")
                }
            }
        }

        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Load the captured image into the ImageView
                Glide.with(this)
                    .load(photoUri) // Use the URI we stored
                    .into(imageView)
                Log.e(TAG, "Captured image URI: $photoUri")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ImageViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imageView = view.findViewById(R.id.image_view)
        val imgBtn = view.findViewById<AppCompatImageButton>(R.id.img_btn)
        imgBtn.setOnClickListener {
            checkPermissions() // Just check permissions here
        }
    }

    private fun checkPermissions() {
        val permissionsNeeded = arrayOf(
            Manifest.permission.CAMERA,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES // For Android 13 and higher
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            }
        )

        val permissionsToRequest = permissionsNeeded.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest)
        } else {
            // All permissions are granted, proceed with your functionality
           showImageSourceDialog()
        }
    }

    private fun showImageSourceDialog() {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Select Image Source")
            .setItems(arrayOf("Camera", "Gallery")) { dialog, which ->
                when (which) {
                    0 -> onCameraSelected()
                    1 -> onGallerySelected()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun onGallerySelected() {
        openImagePicker()
    }

    private fun onCameraSelected() {
        dispatchTakePictureIntent()
    }

    private fun dispatchTakePictureIntent() {

        // Create an image file
        photoFile = createImageFile()

        // Get the image URI
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )

        // Launch camera intent
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri) // Set the URI for the image
        }
        cameraLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)// storage/sdcard0/Pictures/

        // Create a permanent file (not temporary)
        return File(storageDir, "IMG_$timeStamp.jpg").apply {
            Log.d(TAG, "Image file created at: $absolutePath")
        }
//        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)// storage/sdcard0/Android/data/packageName/file
        // Create a Temporary file (not permanent)
//        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
//            Log.d(TAG, "Image file created at: $absolutePath")
//        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        imagePickerLauncher.launch(intent)
    }

}
