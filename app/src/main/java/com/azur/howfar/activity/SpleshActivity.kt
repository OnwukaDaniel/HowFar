package com.azur.howfar.activity

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.azur.howfar.R
import com.azur.howfar.howfarchat.ChatLanding

class SpleshActivity : AppCompatActivity() {
    private val myAuth = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splesh)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        try {
            if (myAuth != null) {
                val intent1 = Intent(this@SpleshActivity, ChatLanding::class.java)
                intent1.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent1)
            } else {
                val intent2 = Intent(this@SpleshActivity, LoginActivityActivity::class.java)
                intent2.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent2)
            }
            overridePendingTransition(0,0)
        } catch (e: Exception) {
        }
    }

    override fun onResume() {
        window.statusBarColor = resources.getColor(R.color.colorPrimary)
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    }
}