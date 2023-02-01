package com.azur.howfar.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.biometrics.BiometricManager
import android.widget.Toast
import androidx.core.content.ContextCompat

/*
class Biometric(val context: Context) {
    private fun biometric() {
        val biometricManager: BiometricManager = BiometricManager.from(context)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS -> {
                val sharedPreferences: SharedPreferences = getSharedPreferences("Authentication", 0)
                val editor = sharedPreferences.edit()
                editor.putString(TEXT, "1")
                editor.putBoolean(SWITCH1, Swicth_authenticate.isChecked())
                editor.apply()
                val intent = Intent(this@AboutActivity, edit_profile::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                ContextCompat.startActivity(intent)
                finish()
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Swicth_authenticate.setChecked(false)
                Toast.makeText(this, "Error code 0x08080101 Authentication failed there's no Fingerprint Reader in your device.", Toast.LENGTH_SHORT).show()
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Swicth_authenticate.setChecked(false)
                Toast.makeText(this, "Error code 0x08080102 Authentication failed biometric system not found.", Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Swicth_authenticate.setChecked(false)
                Toast.makeText(this, "Error code 0x08080103 There's no password for this device.", Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {}
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {}
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {}
        }
    }
}*/
