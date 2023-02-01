package com.azur.howfar.howfarwallet

import android.os.Bundle
import com.azur.howfar.activity.BaseActivity
import com.azur.howfar.databinding.ActivityPayBinding

class ActivityPay : BaseActivity() {
    private val binding by lazy { ActivityPayBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}