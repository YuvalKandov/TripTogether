package com.example.triptogether.utilities

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.lang.ref.WeakReference

class StorageManager private constructor(context: Context) {
    private val contextRef = WeakReference(context)

    companion object {
        @Volatile
        private var instance: StorageManager? = null

        fun init(context: Context): StorageManager {
            return instance ?: synchronized(this) {
                instance ?: StorageManager(context).also { instance = it }
            }
        }

        fun getInstance(): StorageManager {
            return instance ?: throw IllegalStateException(
                "StorageManager must be initialized by calling init(context) before use."
            )
        }
    }

    fun uploadMemoryPhoto(
        tripId: String,
        activityId: String,
        uri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val storageRef = FirebaseStorage.getInstance().reference
        val photoRef = storageRef.child("memories/$tripId/$activityId/${System.currentTimeMillis()}_${uri.lastPathSegment}")

        val uploadTask = photoRef.putFile(uri)

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            photoRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                onSuccess(downloadUri.toString())
            } else {
                task.exception?.let { onFailure(it) }
            }
        }
    }

    fun uploadTripCover(
        tripId: String,
        uri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val storageRef = FirebaseStorage.getInstance().reference
        val coverRef = storageRef.child("trip_covers/$tripId/${System.currentTimeMillis()}_${uri.lastPathSegment}")

        val uploadTask = coverRef.putFile(uri)

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            coverRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                onSuccess(downloadUri.toString())
            } else {
                task.exception?.let { onFailure(it) }
            }
        }
    }
}
