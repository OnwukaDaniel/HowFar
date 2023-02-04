package com.azur.howfar.workManger

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.azur.howfar.R
import com.azur.howfar.models.SupportChatData
import com.azur.howfar.utils.ImageCompressor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import java.io.ByteArrayInputStream

class SupportWorkManager(private val context: Context, params: WorkerParameters) : Worker(context, params) {
    private lateinit var pref: SharedPreferences
    private var imageStream: ByteArrayInputStream? = null
    private var user = FirebaseAuth.getInstance().currentUser
    private var timeRef = FirebaseDatabase.getInstance().reference
    private var data = SupportChatData()

    override fun doWork(): Result {
        pref = context.getSharedPreferences(context.getString(R.string.ALL_PREFERENCE), Context.MODE_PRIVATE)
        val json = pref.getString(context.getString(R.string.support_data), "")
        data = Gson().fromJson(json, SupportChatData::class.java)
        when {
            data.images.isEmpty() -> upload()
            else -> storeImage()
        }
        return Result.success()
    }

    private fun storeImage() {
        timeRef = FirebaseDatabase.getInstance().reference.child("time").child(user!!.uid)
        timeRef.setValue(ServerValue.TIMESTAMP).addOnSuccessListener {
            timeRef.get().addOnSuccessListener { timeSnapshot ->
                if (timeSnapshot.exists()) {
                    var timeSent = timeSnapshot.value.toString()
                    var successCount = 0
                    for ((index, i) in data.images.withIndex()) {
                        val pair: Pair<ByteArrayInputStream, ByteArray> = ImageCompressor.compressImage(Uri.parse(i), context, null)
                        imageStream = pair.first
                        val imageRef = FirebaseStorage.getInstance().reference.child(SUPPORT_IMAGES).child(user!!.uid).child(timeSent)
                        val imageUploadTask = imageRef.putStream(imageStream!!)
                        timeSent += 1000
                        imageUploadTask.continueWith { task ->
                            if (!task.isSuccessful) task.exception?.let { it ->
                                throw  it
                            }
                            imageRef.metadata.addOnSuccessListener {
                                imageRef.downloadUrl.addOnSuccessListener { uri ->
                                    data.images[index] = uri.toString()
                                    successCount++
                                    if (successCount == data.images.size) upload()
                                }.addOnFailureListener {
                                    Toast.makeText(context, "Moment upload failed!!! Retry", Toast.LENGTH_LONG).show()
                                    return@addOnFailureListener
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun upload() {
        val timeRef = FirebaseDatabase.getInstance().reference.child("time").child(user!!.uid)
        timeRef.setValue(ServerValue.TIMESTAMP).addOnSuccessListener {
            timeRef.get().addOnSuccessListener { timeSnapshot ->
                if (timeSnapshot.exists()) {
                    val timeSent = timeSnapshot.value.toString()
                    data.time = timeSent
                    val con = "$timeSent-${user!!.uid}"
                    val ref = FirebaseDatabase.getInstance("https://howfar-b24ef.firebaseio.com").reference.child(CONTACT_SUPPORT).child(con)
                    val mySupportRef = FirebaseDatabase.getInstance().reference.child(CONTACT_SUPPORT).child(user!!.uid).child(timeSent)
                    ref.setValue(data).addOnSuccessListener {
                        Toast.makeText(context, "Support message delivered.", Toast.LENGTH_LONG).show()
                    }
                    mySupportRef.setValue(data)
                }
            }
        }
    }

    companion object {
        const val CONTACT_SUPPORT = "CONTACT_SUPPORT"
        const val MOMENT_DATA = "MOMENT_DATA"
        const val PERSONAL_POST_RECORD = "PERSONAL_POST_RECORD"
        const val SUPPORT_IMAGES = "SUPPORT_IMAGES"
        const val USER_DETAILS = "user_details"
    }
}