package com.azur.howfar.activity

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.azur.howfar.R
import com.azur.howfar.databinding.ActivityContactUsBinding
import com.azur.howfar.models.SupportChatData
import com.azur.howfar.utils.ImageCompressor
import com.azur.howfar.utils.Util
import com.azur.howfar.workManger.SupportWorkManager
import com.bumptech.glide.Glide
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import java.io.ByteArrayInputStream

class ContactUsActivity : AppCompatActivity(), SupportDeleteHelper {
    private val binding by lazy { ActivityContactUsBinding.inflate(layoutInflater) }
    private var user = FirebaseAuth.getInstance().currentUser
    private var permissionsStorage = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private var dataset = arrayListOf<ByteArray>()
    private lateinit var pref: SharedPreferences
    private var datasetString = arrayListOf<String>()
    private var contactImagesAdapter = ContactImagesAdapter()
    private val workManager = WorkManager.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        pref = getSharedPreferences(getString(R.string.ALL_PREFERENCE), Context.MODE_PRIVATE)
        contactImagesAdapter.dataset = dataset
        contactImagesAdapter.supportDeleteHelper = this
        binding.rvImages.adapter = contactImagesAdapter
        binding.addImagesCard.setOnClickListener { openImagePick() }
        binding.send.setOnClickListener {
            val subject = binding.subject.text.trim().toString()
            val content = binding.message.text.trim().toString()
            if (subject == "") {
                Snackbar.make(binding.root, "Empty subject", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (content == "") {
                Snackbar.make(binding.root, "Empty Message", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            binding.subject.text.clear()
            binding.message.text.clear()
            Toast.makeText(this, "Sent", Toast.LENGTH_LONG).show()
            workManagerUpload(SupportChatData(senderUid = user!!.uid, subject = subject, content = content, images = datasetString, time = ""))
            finish()
        }
    }

    private fun workManagerUpload(data: SupportChatData) {
        val json = Gson().toJson(data)
        pref.edit().putString(getString(R.string.support_data), json).apply()
        val workRequest = OneTimeWorkRequestBuilder<SupportWorkManager>().addTag("moment upload").build()
        workManager.enqueue(workRequest)
    }

    private fun openImagePick() {
        if (Util.permissionsAvailable(permissionsStorage, this)) {
            cropImage.launch(
                options {
                    this.setActivityTitle("Moment")
                    this.setAllowFlipping(true)
                    this.setAllowRotation(true)
                    this.setAspectRatio(400, 550)
                    this.setFixAspectRatio(true)
                    this.setAutoZoomEnabled(true)
                    this.setBackgroundColor(Color.BLACK)
                    this.setImageSource(includeGallery = true, includeCamera = true)
                    setGuidelines(CropImageView.Guidelines.ON)
                }
            )
        } else {
            ActivityCompat.requestPermissions(this, permissionsStorage, 36)
        }
    }

    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val selectedImage = result.uriContent!!
            val imageUri = result.getUriFilePath(this)!! // optional usage
            val pair: Pair<ByteArrayInputStream, ByteArray> = ImageCompressor.compressImage(selectedImage, this, null)
            dataset.add(pair.second)
            datasetString.add(selectedImage.toString())
            contactImagesAdapter.notifyItemInserted(dataset.size)
        } else {
            val exception = result.error
            exception!!.printStackTrace()
        }
    }

    override fun delete(position: Int) {
        dataset.removeAt(position)
        datasetString.removeAt(position)
    }
}

interface SupportDeleteHelper {
    fun delete(position: Int)
}

class ContactImagesAdapter : RecyclerView.Adapter<ContactImagesAdapter.ViewHolder>() {
    var dataset = arrayListOf<ByteArray>()
    lateinit var supportDeleteHelper: SupportDeleteHelper
    lateinit var context: Context

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.contact_image)
        val delete: ImageView = itemView.findViewById(R.id.delete_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.contact_support_images, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val datum = dataset[position]
        Glide.with(context).load(dataset[position]).centerCrop().into(holder.image)

        holder.delete.setOnClickListener {
            supportDeleteHelper.delete(dataset.indexOf(datum))
            dataset.remove(datum)
            notifyItemRemoved(dataset.indexOf(datum))
        }
    }

    override fun getItemCount() = dataset.size
}