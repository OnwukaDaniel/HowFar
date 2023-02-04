package com.azur.howfar.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.azur.howfar.R
import android.os.Looper
import android.content.Intent
import android.os.Handler
import android.view.WindowManager
import android.view.animation.AnimationUtils

class SpleshActivityLike : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splesh_like)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        val anim = AnimationUtils.loadAnimation(this, R.anim.splesh)
        anim.fillAfter = true
        //findViewById<View>(R.id.tv1).visibility = View.VISIBLE
        //findViewById<View>(R.id.tv1).startAnimation(anim)
        Handler(Looper.myLooper()!!).postDelayed({ startActivity(Intent(this@SpleshActivityLike, MainActivity::class.java)) }, 500)
    }

    override fun onResume() {
        window.statusBarColor = resources.getColor(R.color.colorPrimary)
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = resources.getColor(R.color.colorPrimary)
    }
}