package com.azur.howfar.activity

import android.content.Intent
import android.os.Bundle
import com.azur.howfar.R
import com.azur.howfar.databinding.ActivitySettingBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SettingActivity : BaseActivity() {
    private val binding by lazy { ActivitySettingBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.logOut.setOnClickListener {
            Firebase.auth.signOut()
            val intent = Intent(this, LoginActivityActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.enter_left_to_right, R.anim.exit_left_to_right)
        }
        binding.contactUs.setOnClickListener {
            val intent = Intent(this, ContactUsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.enter_left_to_right, R.anim.exit_left_to_right)
        }
    }
}