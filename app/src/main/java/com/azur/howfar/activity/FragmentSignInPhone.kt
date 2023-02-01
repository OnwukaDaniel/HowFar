package com.azur.howfar.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.azur.howfar.R
import com.azur.howfar.databinding.FragmentSignInPhoneBinding
import com.azur.howfar.howfarchat.ChatLanding
import com.azur.howfar.models.Countries
import com.azur.howfar.models.UserProfile
import com.azur.howfar.models.UserSignUp
import com.azur.howfar.utils.Keyboard.hideKeyboard
import com.azur.howfar.utils.Util
import com.azur.howfar.viewmodel.SignUpViewModel
import com.azur.howfar.workManger.HowFarAnalyticsTypes
import com.azur.howfar.workManger.OpenAppWorkManager
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseApiNotAvailableException
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

class FragmentSignInPhone : Fragment(), ClickHelper, View.OnClickListener {
    private lateinit var binding: FragmentSignInPhoneBinding
    private var json = ""
    private var phoneNumber = ""
    private var countryCode = ""
    private var verifiedPresentUserProfile = UserProfile()
    private var storedVerificationId = ""
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private val auth = FirebaseAuth.getInstance()
    private lateinit var pref: SharedPreferences
    private var ref = FirebaseDatabase.getInstance().reference
    private lateinit var dialog: Dialog
    private var countryCodesAdapter = CountryCodesAdapter()
    private var countriesSelected = Countries()
    private val signUpViewModel: SignUpViewModel by activityViewModels()
    private lateinit var workManager : WorkManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSignInPhoneBinding.inflate(inflater, container, false)
        pref = requireActivity().getSharedPreferences(getString(R.string.ALL_PREFERENCE), Context.MODE_PRIVATE)
        workManager = WorkManager.getInstance(requireContext())
        dialog = Dialog(requireContext())
        binding.phoneCode.setOnClickListener(this)
        binding.sendCode.setOnClickListener(this)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        countryCodesAdapter.clickHelper = this
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.country_codes_dialog, binding.root, false)
        dialog.setContentView(dialogView)
        val dialogRv: RecyclerView = dialogView.findViewById(R.id.dialog_rv)
        dialogRv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        dialogRv.adapter = countryCodesAdapter
        val tm = requireActivity().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        countryCode = (tm.networkCountryIso.lowercase())
        try {
            val stream = requireContext().assets.open("country_codes.json")
            json = stream.bufferedReader().use { it.readText() }
            val jj = ObjectMapper().readValue(json, object : TypeReference<List<Countries>?>() {})!!
            countryCodesAdapter.dataset = jj
            countryCodesAdapter.notifyDataSetChanged()
            for (i in jj) {
                if (i.code.lowercase() == countryCode) {
                    binding.phoneCode.text = i.dial_code
                    return
                }
            }
        } catch (e: Exception) {
            println("This is a stack *************************************** $e")
        }
    }

    private fun attemptLogin() {
        activeUserAnalytics()
        val json = Gson().toJson(verifiedPresentUserProfile)
        pref.edit().putString(getString(R.string.this_user), json).apply()
        val intent = Intent(requireContext(), ChatLanding::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        requireActivity().overridePendingTransition(
            R.anim.enter_right_to_left,
            R.anim.exit_right_to_left
        )
    }

    private fun activeUserAnalytics() {
        val workRequest = OneTimeWorkRequestBuilder<OpenAppWorkManager>().addTag("analytics")
            .setInputData(workDataOf("action" to HowFarAnalyticsTypes.LOG_IN))
            .build()
        workManager.enqueue(workRequest)
    }

    private fun auth(user: UserProfile) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                when (e) {
                    is FirebaseAuthInvalidCredentialsException -> errorToast("Invalid details. Retry")
                    is FirebaseTooManyRequestsException -> errorToast("Too many Requests, Try Again")
                    is FirebaseAuthException -> errorToast("App not registered, Try Again")
                    is FirebaseApiNotAvailableException -> errorToast("Google Play service not installed")
                }
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                errorToast("Code sent")
                storedVerificationId = verificationId
                resendToken = token
                val pro = UserSignUp(name = user.name, phone = phoneNumber, countryCode = countryCode, verificationCode = verificationId, gender = user.gender)
                signUpViewModel.setUserSignUp(pro)
                signUpViewModel.setStartCountdown(true)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun initFirebase() {
        val phone = binding.phoneNumber.text.trim().toString()
        val inputFormattedNumber = Util.formatNumber(phone)
        val ref = FirebaseDatabase.getInstance().reference.child(USER_DETAILS)
        ref.get().addOnSuccessListener {
            if (it.exists()) {
                for (i in it.children) {
                    val userProfile = i.getValue(UserProfile::class.java)!!
                    val singleFormattedPhone = Util.formatNumber(userProfile.phone)
                    if (singleFormattedPhone == inputFormattedNumber) {
                        verifiedPresentUserProfile = userProfile
                        hideKeyboard()
                        auth(userProfile)
                        return@addOnSuccessListener
                    }
                }
                errorToast("User not registered")
            }
        }.addOnFailureListener {
            hideKeyboard()
            //it.localizedMessage?.let { it1 -> errorToast(it1) }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    attemptLogin()
                } else {
                    errorToast("Error Occurred, Try Again")
                }
            }
    }

    private fun errorToast(input: String = "Wrong code") {
        if (activity != null && isAdded) {
            Snackbar.make(binding.root, input, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onClickedHelp(datum: Countries) {
        countriesSelected = datum
        binding.phoneCode.text = datum.dial_code
        dialog.dismiss()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.phone_code -> dialog.show()
            R.id.send_code -> {
                if (binding.phoneNumber.text.trim().toString() == "") {
                    errorToast("Input can't be empty")
                    return
                }
                val input = binding.phoneNumber.text.trim().toString().removePrefix("0")
                phoneNumber = binding.phoneCode.text.toString() + input
                initFirebase()
                val bundle = Bundle()
                bundle.putString("phoneNumber", phoneNumber)
                val fragment = FragmentConfirmationCodeSignIn()
                fragment.arguments = bundle
                childFragmentManager.beginTransaction()
                    .addToBackStack("verify code")
                    .replace(R.id.verify_root, fragment)
                    .commit()
            }
        }
    }

    companion object {
        const val CALL_REFERENCE = "call_reference"
        const val TRANSFER_HISTORY = "user_coins_transfer"
        const val USER_DETAILS = "user_details"
    }
}