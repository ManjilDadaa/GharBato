package com.example.gharbato.utils

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class ImageUtils(private val activity: Activity, private val registryOwner: ActivityResultRegistryOwner) {
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var onImageSelectedCallback: ((Uri?) -> Unit)? = null

    fun registerLaunchers(onImageSelected: (Uri?) -> Unit) {
        onImageSelectedCallback = onImageSelected

        // Register for selecting image from gallery
        galleryLauncher = registryOwner.activityResultRegistry.register(
            "galleryLauncher", ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val uri = result.data?.data
            if (result.resultCode == Activity.RESULT_OK && uri != null) {
                onImageSelectedCallback?.invoke(uri)
            } else {
                Log.e("ImageUtils", "Image selection cancelled or failed")
            }
        }

        // Register permission request
        permissionLauncher = registryOwner.activityResultRegistry.register(
            "permissionLauncher", ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                openGallery()
            } else {
                Log.e("ImageUtils", "Permission denied")
            }
        }
    }

    fun launchImagePicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(permission)
        } else {
            openGallery()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        galleryLauncher.launch(intent)
    }
}