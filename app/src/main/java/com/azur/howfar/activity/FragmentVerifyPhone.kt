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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.azur.howfar.R
import com.azur.howfar.databinding.FragmentVerifyPhoneBinding
import com.azur.howfar.dilog.ProgressFragment
import com.azur.howfar.howfarchat.ChatLanding
import com.azur.howfar.models.Countries
import com.azur.howfar.models.UserProfile
import com.azur.howfar.models.UserSignUp
import com.azur.howfar.user.EditProfileActivity
import com.azur.howfar.utils.Keyboard.hideKeyboard
import com.azur.howfar.utils.Util.formatNumber
import com.azur.howfar.viewmodel.SignUpViewModel
import com.bumptech.glide.Glide
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApiNotAvailableException
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import java.io.ByteArrayInputStream
import java.util.*
import java.util.concurrent.TimeUnit

class FragmentVerifyPhone : Fragment(), ClickHelper, View.OnClickListener {
    private lateinit var binding: FragmentVerifyPhoneBinding
    private var json = ""
    private var phoneNumber = ""
    private var countryCode = ""
    private var name = ""
    private var gender = ""
    private var imageStream: Pair<ByteArrayInputStream, ByteArray>? = null
    private var storedVerificationId = ""
    private lateinit var pref: SharedPreferences
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private val auth = FirebaseAuth.getInstance()
    private var ref = FirebaseDatabase.getInstance().reference
    private var countryCodesAdapter = CountryCodesAdapter()
    private var countriesSelected = Countries()
    private lateinit var dialog: Dialog
    private val signUpViewModel: SignUpViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentVerifyPhoneBinding.inflate(inflater, container, false)
        dialog = Dialog(requireContext())
        showSendText()
        pref = requireActivity().getSharedPreferences(getString(R.string.ALL_PREFERENCE), Context.MODE_PRIVATE)
        signUpViewModel.three.observe(viewLifecycleOwner) {
            name = it.name
            gender = it.gender
            imageStream = it.stream
            Glide.with(this).load(imageStream!!.second).error(R.drawable.ic_avatar).centerCrop().into(binding.verifyUserImage)
        }
        binding.phoneCode.setOnClickListener(this)
        binding.sendCode.setOnClickListener(this)
        binding.verifyCancel.setOnClickListener(this)
        return binding.root
    }

    private fun showSendText() {
        binding.sendCodeProgress.visibility = View.GONE
        binding.sendCodeText.visibility = View.VISIBLE
    }

    private fun showProgress() {
        binding.sendCodeProgress.visibility = View.VISIBLE
        binding.sendCodeText.visibility = View.GONE
    }

    private fun checkUser() {
        showProgress()
        val phone = binding.phoneNumber.text.trim().toString()
        val inputFormattedNumber = formatNumber(phone)
        val ref = ref.child(EditProfileActivity.USER_DETAILS)
        ref.get().addOnSuccessListener {
            if (it.exists()) {
                for (i in it.children) {
                    val userProfile = i.getValue(UserProfile::class.java)!!
                    val singleFormattedPhone = formatNumber(userProfile.phone)
                    if (singleFormattedPhone == inputFormattedNumber) {
                        errorToast("User already exist")
                        hideKeyboard()
                        showSendText()
                        return@addOnSuccessListener
                    }
                }
            }
            auth()
        }.addOnFailureListener {
            hideKeyboard()
            showSendText()
            it.localizedMessage?.let { it1 -> errorToast(it1) }
        }
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

            for (i in jj) if (i.code.lowercase() == countryCode) {
                binding.phoneCode.text = i.dial_code
                return
            }
        } catch (e: Exception) {
            println("This is a stack *************************************** ${e.printStackTrace()}")
        }
    }

    private fun auth() {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                hideKeyboard()
                showSendText()
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                hideKeyboard()
                showSendText()
                when (e) {
                    is FirebaseAuthInvalidCredentialsException -> errorToast("Invalid details. Retry")
                    is FirebaseTooManyRequestsException -> errorToast("Too many Requests, Try Again")
                    is FirebaseAuthException -> errorToast("App not registered, Try Again")
                    is FirebaseApiNotAvailableException -> errorToast("Google Play service not installed")
                    else -> errorToast("Retry.")
                }
                println("This is a stack *************************************** ${e.localizedMessage}")
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                hideKeyboard()
                showSendText()
                errorToast("Code sent to $phoneNumber")
                storedVerificationId = verificationId
                resendToken = token
                val pro = UserSignUp(name = name, phone = phoneNumber, countryCode = countryCode, verificationCode = verificationId, gender = gender)
                signUpViewModel.setUserSignUp(pro)
                signUpViewModel.setStartCountdown(true)
                childFragmentManager.beginTransaction().addToBackStack("verify code").replace(R.id.verify_root, FragmentConfirmationCode()).commit()
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

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    hideKeyboard()
                    showSendText()
                    upload(task)
                } else {
                    showSendText()
                    errorToast("Error Occurred, Try Again")
                }
            }
    }

    private fun errorToast(input: String = "Wrong code") {
        hideKeyboard()
        Toast.makeText(requireContext(), input, Toast.LENGTH_LONG).show()
    }

    private fun navigate(task: Task<AuthResult>, imageUri: String) {
        val user = task.result?.user
        val u = UserProfile(name = name, phone = phoneNumber, countryCode = countryCode, gender = gender, uid = user!!.uid, image = imageUri)

        ref = ref.child("user_details").child(user.uid)
        ref.setValue(u).addOnSuccessListener {
            val json = Gson().toJson(u)
            pref.edit().putString(getString(R.string.this_user), json).apply()
            val intent = Intent(requireContext(), ChatLanding::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            requireActivity().overridePendingTransition(R.anim.enter_right_to_left, R.anim.exit_right_to_left)

        }.addOnFailureListener { errorToast("Error Occurred, Try Again") }
    }

    private fun upload(authTask: Task<AuthResult>) {
        val fragmentDialog = ProgressFragment()
        requireActivity().supportFragmentManager.beginTransaction().addToBackStack("progress").replace(R.id.verify_root, fragmentDialog).commit()
        val timeSent = Calendar.getInstance().timeInMillis.toString()

        val imageRef =
            FirebaseStorage.getInstance().reference.child(EditProfileActivity.PROFILE_IMAGE).child(FirebaseAuth.getInstance().currentUser!!.uid).child(timeSent)
        val uploadTask = imageRef.putStream(imageStream!!.first)
        uploadTask.continueWith { task ->
            if (!task.isSuccessful) task.exception?.let { itId ->
                requireActivity().supportFragmentManager.beginTransaction().remove(fragmentDialog).commit()
                throw  itId
            }
            imageRef.downloadUrl.addOnSuccessListener {
                uploadTask.continueWith { task ->
                    if (!task.isSuccessful) task.exception?.let {
                        requireActivity().supportFragmentManager.beginTransaction().remove(fragmentDialog).commit()
                        throw  it
                    }
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        requireActivity().supportFragmentManager.beginTransaction().remove(fragmentDialog).commit()
                        navigate(authTask, uri.toString())
                    }.addOnFailureListener {
                        requireActivity().supportFragmentManager.beginTransaction().remove(fragmentDialog).commit()
                        Toast.makeText(context, "Upload failed!!! Retry", Toast.LENGTH_LONG).show()
                        return@addOnFailureListener
                    }
                }
            }.addOnFailureListener {
                requireActivity().supportFragmentManager.beginTransaction().remove(fragmentDialog).commit()
                Toast.makeText(context, "Upload failed!!! Retry", Toast.LENGTH_LONG).show()
                return@addOnFailureListener
            }
        }
    }

    companion object {
        const val TRANSFER_HISTORY = "user_coins_transfer"
        const val USER_DETAILS = "user_details"
    }

    override fun onClickedHelp(datum: Countries) {
        countriesSelected = datum
        binding.phoneCode.text = datum.dial_code
        dialog.dismiss()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.verify_cancel -> requireActivity().onBackPressed()
            R.id.phone_code -> dialog.show()
            R.id.send_code -> {
                hideKeyboard()
                if (binding.phoneNumber.text.trim().toString() == "") {
                    errorToast("Input can't be empty")
                    return
                }
                val input = binding.phoneNumber.text.trim().toString().removePrefix("0")
                phoneNumber = binding.phoneCode.text.toString() + input
                checkUser()
            }
        }
    }
}