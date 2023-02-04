package com.azur.howfar.howfarwallet

import android.os.Bundle
import android.view.View
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.azur.howfar.R
import com.azur.howfar.activity.BaseActivity
import com.azur.howfar.databinding.ActivityWalletBinding
import com.azur.howfar.models.UserProfile
import com.azur.howfar.utils.Keyboard.hideKeyboard
import com.azur.howfar.utils.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.SocketTimeoutException
import java.util.*

class ActivityWallet : BaseActivity(), View.OnClickListener {
    private val binding by lazy { ActivityWalletBinding.inflate(layoutInflater) }
    private val scope = CoroutineScope(Dispatchers.IO)
    private val myAuth = FirebaseAuth.getInstance().currentUser!!.uid
    private var selectedTimeMillis = 0L
    private var user = UserProfile()
    private var walletsRef = FirebaseDatabase.getInstance().reference
    private var profileRef = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        selectedTimeMillis = Calendar.getInstance().timeInMillis
        showLoading()
        profileRef = profileRef.child("user_details").child(myAuth)
        walletsRef = walletsRef.child("wallets").child(myAuth)
        walletsRef.keepSynced(false)
        walletsRef.get().addOnSuccessListener {
            profileRef.get().addOnSuccessListener { profileSnap ->
                user = profileSnap.getValue(UserProfile::class.java)!!
            }
            if (it.exists()) showPayment() else showRegister()
        }.addOnFailureListener {
            showRegister()
        }

        binding.request.setOnClickListener(this)
        binding.send.setOnClickListener(this)
        binding.payClose.setOnClickListener(this)
        binding.createWallet.setOnClickListener(this)
        binding.dob.setOnClickListener(this)
        binding.walletClose.setOnClickListener(this)
        binding.createAccount.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        window.statusBarColor = resources.getColor(R.color.white)
    }

    private fun showPayment() {
        binding.payRegister.visibility = View.GONE
        binding.payLoading.visibility = View.GONE
        binding.payLayout.visibility = View.VISIBLE
        binding.walletInput.visibility = View.GONE
    }

    private fun showWalletInput() {
        binding.payRegister.visibility = View.GONE
        binding.payLoading.visibility = View.GONE
        binding.payLayout.visibility = View.GONE
        binding.walletInput.visibility = View.VISIBLE
    }

    private fun showLoading() {
        binding.payRegister.visibility = View.GONE
        binding.payLoading.visibility = View.VISIBLE
        binding.payLayout.visibility = View.GONE
        binding.walletInput.visibility = View.GONE
    }

    private fun showRegister() {
        binding.payRegister.visibility = View.VISIBLE
        binding.payLoading.visibility = View.GONE
        binding.payLayout.visibility = View.GONE
        binding.walletInput.visibility = View.GONE
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.send -> {
                hideKeyboard()
                supportFragmentManager.beginTransaction().addToBackStack("send").replace(R.id.wallet_root, FragmentBankList()).commit()
            }
            R.id.request -> {}
            R.id.create_account -> {
                hideKeyboard()
                val bvn = binding.bvn.text.toString().trim()
                when ("") {
                    bvn -> {
                        showMsg("BVN not set")
                        return
                    }
                }
                if (selectedTimeMillis == 0L) {
                    showMsg("Set date of birth")
                    return
                }
                val instance = Calendar.getInstance()
                instance.timeInMillis = selectedTimeMillis
                var day = instance.get(Calendar.DAY_OF_MONTH).toString()
                var month = instance.get(Calendar.MONTH).toString()
                val year = instance.get(Calendar.YEAR).toString()
                day = if (day.length == 1) "0$day" else day
                month = Util.getShortMonth(month.toInt())
                val dob = "$day-$month-$year"
                val vfdWallet = VFDContactModel(dob = dob, bvn = bvn)
                createAccount(vfdWallet)
            }
            R.id.pay_close -> onBackPressed()
            R.id.create_wallet -> showWalletInput()
            R.id.dob -> {
                hideKeyboard()
                val datePicker =
                    MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select date of birth")
                        .setSelection(selectedTimeMillis)
                        .build()
                datePicker.show(supportFragmentManager, "dob")
                datePicker.addOnPositiveButtonClickListener {
                    selectedTimeMillis = it
                    val instance = Calendar.getInstance()
                    instance.timeInMillis = it
                    val day = instance.get(Calendar.DAY_OF_MONTH)
                    var month = instance.get(Calendar.MONTH).toString()
                    val year = instance.get(Calendar.YEAR)
                    month = Util.getShortMonth(month.toInt())
                    val dob = "$day-$month-$year"
                    binding.dob.text = dob
                }

                datePicker.addOnCancelListener { showMsg() }
                //datePicker.addOnDismissListener { showMsg() }
            }
            R.id.wallet_close -> onBackPressed()
        }
    }

    private fun createAccount(vfdWallet: VFDContactModel) {
        showLoading()
        try {
            val header = "Authorization"
            val key = ": Bearer 72668e1e-9edf-311c-b36a-45056bda2185"
            val ref = FirebaseDatabase.getInstance().reference.child("howFar").child("vfd_credentials")

            ref.get().addOnSuccessListener {
                if (it.exists()){
                    scope.launch {
                        val credential = it.value.toString()
                        println("credential ********************************************** $credential")
                        if (credential == "") {
                            showMsg("Invalid / missing credentials")
                            return@launch
                        }
                        val body: RequestBody = "".toRequestBody("application/json".toMediaTypeOrNull())
                        val url = "https://devesb.vfdbank.systems:8263/vfd-wallet/1.1/wallet2/client/create?bvn=${vfdWallet.bvn}" +
                                "&wallet-credentials=$credential&dateOfBirth=${vfdWallet.dob}"
                        val client = OkHttpClient()
                        val request = Request.Builder().url(url).addHeader(header, key).post(body).build()
                        val response = client.newCall(request).execute()
                        val jsonResponse = response.body?.string()
                        println("Code ********************************************** ${response.code}")
                        println("JsonResponse ********************************************** $jsonResponse, *********** $vfdWallet")
                        if (response.code == 200) {
                            val responseData = Gson().fromJson(jsonResponse, VFDCreateWalletResponse::class.java)
                            walletsRef.setValue(responseData).addOnSuccessListener {
                                showMsg("Account created successfully")
                                showPayment()
                            }
                        } else if (response.code in 400..499) {
                            showMsg("Incorrect details")
                            runOnUiThread { showWalletInput() }
                        } else {
                            showMsg(response.message)
                            runOnUiThread { showWalletInput() }
                        }
                    }
                } else {
                    showMsg("Does not exist $it")
                    runOnUiThread { showWalletInput() }
                }

            }.addOnFailureListener {
                showMsg("Network exception")
                runOnUiThread { showWalletInput() }
            }
        } catch (e: SocketTimeoutException) {
            showMsg("Time out")
            runOnUiThread { showWalletInput() }
        }
    }

    private fun showMsg(msg: String = "Date not set") {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
    }
}

data class VFDContactModel(
    var dob: String = "",
    var bvn: String = "",
)

data class VFDCreateWalletResponse(
    var status: String = "",
    var message: String = "",
    var data: VFDData = VFDData(),
)

data class VFDData(
    var firstname: String = "",
    var middlename: String = "",
    var lastname: String = "",
    var bvn: String = "",
    var phone: String = "",
    var accountNo: String = "",
    var dob: String = "",
)