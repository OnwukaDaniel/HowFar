package com.azur.howfar.howfarchat.chat

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.azur.howfar.R
import com.azur.howfar.activity.BaseActivity
import com.azur.howfar.databinding.ActivityCustomizeChatBinding

class ActivityCustomizeChatWallpaper : BaseActivity(), WallpaperClickHelper {
    private val binding by lazy { ActivityCustomizeChatBinding.inflate(layoutInflater) }
    private lateinit var pref: SharedPreferences
    private val verticalWallpaperDisplayAdapter = VerticalWallpaperDisplayAdapter()
    private val myAuth = FirebaseAuth.getInstance().currentUser!!.uid
    private var receiverUid = ""
    private val defaultWall = R.drawable.chat_wallpaper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        pref = getSharedPreferences(getString(R.string.ALL_PREFERENCE), Context.MODE_PRIVATE)
        receiverUid = intent.getStringExtra("uid")!!
        binding.bubbleEdit.visibility = View.GONE
        binding.displayBottomSheet.visibility = View.VISIBLE

        val current = pref.getInt(getString(R.string.chatWallpaper) + receiverUid, defaultWall)
        Glide.with(this).load(current).centerCrop().into(binding.displayChatBackground)

        binding.displayBack.setOnClickListener { onBackPressed() }
    }

    override fun onResume() {
        super.onResume()
        verticalWallpaperDisplayAdapter.wallpaperClickHelper = this
        verticalWallpaperDisplayAdapter.pref = pref
        binding.displayRv.adapter = verticalWallpaperDisplayAdapter
        binding.displayRv.layoutManager = GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false)

        val unwrappedDrawableRight = AppCompatResources.getDrawable(this, R.drawable.chat_bubble_purple_right)
        val unwrappedDrawableLeft = AppCompatResources.getDrawable(this, R.drawable.chat_bubble_purple_left)
        val wrappedDrawableRight = DrawableCompat.wrap(unwrappedDrawableRight!!)
        val wrappedDrawableLeft = DrawableCompat.wrap(unwrappedDrawableLeft!!)
        val chatColor = pref.getString(getString(R.string.chatBubbleColor) + receiverUid, "#660099")
        DrawableCompat.setTint(wrappedDrawableRight, Color.parseColor(chatColor))
        DrawableCompat.setTint(wrappedDrawableLeft, Color.parseColor(chatColor))
        binding.displayToolbar.setBackgroundColor(Color.parseColor(chatColor))
        window.statusBarColor = Color.parseColor(chatColor)
        binding.displayReceived.chatLayout.background = wrappedDrawableLeft
        binding.displaySent.chatLayout.background = wrappedDrawableRight
        binding.displaySent2.chatLayout.background = wrappedDrawableRight
    }

    override fun onWallClicked(wall: Int) {
        Glide.with(this).load(wall).centerCrop().into(binding.displayChatBackground)
        pref.edit().putInt(getString(R.string.chatWallpaper) + receiverUid, wall).apply()
        val toast = Toast.makeText(this, "Wallpaper set", Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }
}

interface WallpaperClickHelper {
    fun onWallClicked(wall: Int)
}

class VerticalWallpaperDisplayAdapter : RecyclerView.Adapter<VerticalWallpaperDisplayAdapter.ViewHolder>() {
    lateinit var wallpaperClickHelper: WallpaperClickHelper
    private lateinit var context: Context
    lateinit var pref: SharedPreferences
    private var dataset: ArrayList<Int> = arrayListOf(
        R.drawable.wall2,
        R.drawable.wall3, R.drawable.wall4,
        R.drawable.wall5, R.drawable.wall6,
        R.drawable.wall7, R.drawable.wall8,
        R.drawable.wall9, R.drawable.wall10,
        R.drawable.wall11, R.drawable.wall12,
        R.drawable.wall13, R.drawable.wall14,
        R.drawable.wall15, R.drawable.wall16,
        R.drawable.wall17, R.drawable.wall18,
        R.drawable.wall19, R.drawable.wall20,
        R.drawable.wall21, R.drawable.wall22,
        R.drawable.wall23, R.drawable.wall24,
        R.drawable.wall25, R.drawable.wall26,
    )

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.display_image_root)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.row_display_wallpaper_images, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val datum = dataset[position]
        Glide.with(context).load(datum).centerCrop().into(holder.image)
        holder.image.setOnClickListener { wallpaperClickHelper.onWallClicked(datum) }
    }

    override fun getItemCount() = dataset.size
}