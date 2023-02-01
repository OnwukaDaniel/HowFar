package com.azur.howfar.howfarwallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.RecyclerView
import com.azur.howfar.R
import com.azur.howfar.activity.BaseActivity
import com.azur.howfar.databinding.ActivityWalletBinding
import com.azur.howfar.livedata.ValueEventLiveData
import com.azur.howfar.models.*
import com.azur.howfar.utils.Keyboard.hideKeyboard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.SocketTimeoutException

class ActivityWallet : BaseActivity(), View.OnClickListener {
    private val binding by lazy { ActivityWalletBinding.inflate(layoutInflater) }
    private val scope = CoroutineScope(Dispatchers.IO)
    private val myAuth = FirebaseAuth.getInstance().currentUser!!.uid
    private var vfdData = VFDData()
    private var walletsSecurityRef = FirebaseDatabase.getInstance("https://howfar-b24ef-wallet.firebaseio.com").reference
    private var walletsRef = FirebaseDatabase.getInstance("https://howfar-b24ef-wallet.firebaseio.com").reference
    private var profileRef = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        showLoading()

        profileRef = profileRef.child("user_details").child(myAuth)
        walletsRef = walletsRef.child("wallets").child(myAuth)
        walletsSecurityRef = walletsSecurityRef.child("wallets_security").child(myAuth)
        walletsRef.keepSynced(false)
        walletsRef.get().addOnSuccessListener {
            if (it.exists()) {
                vfdData = it.getValue(VFDData::class.java)!!
                binding.accountNumber.text = vfdData.account_number
                binding.accountName.text = "${vfdData.firstname} ${vfdData.lastname}"
                binding.balance.text = vfdData.balance
                signInUser(vfdData)
            } else showRegister()
        }.addOnFailureListener { error ->
            finish()
            showMsg(error.message!!)
        }
        ValueEventLiveData(walletsRef).observe(this) {
            when (it.second) {
                EventListenerType.onDataChange -> {
                    vfdData = it.first.getValue(VFDData::class.java)!!
                    binding.accountNumber.text = vfdData.account_number
                    binding.accountName.text = "${vfdData.firstname} ${vfdData.lastname}"
                    binding.balance.text = vfdData.balance
                }
            }
        }
        binding.navChat.setOnClickListener(this)
        binding.navSend.setOnClickListener(this)
        binding.navSignOut.setOnClickListener(this)
        binding.sendMoney.setOnClickListener(this)
        binding.requestMoney.setOnClickListener(this)
        binding.myCards.setOnClickListener(this)
        binding.payBills.setOnClickListener(this)
        binding.accountCopy.setOnClickListener(this)
        binding.walletDrawerToggle.setOnClickListener(this)

        binding.payClose.setOnClickListener(this)
        binding.createWallet.setOnClickListener(this)
        binding.walletClose.setOnClickListener(this)
        binding.createAccount.setOnClickListener(this)
        profileRef.get().addOnSuccessListener {
            if (it.exists()) {
                val userProfile = it.getValue(UserProfile::class.java)!!
                binding.email.setText(userProfile.email)
                binding.username.setText(userProfile.name)
                binding.phone.text = userProfile.phone
            } else {
                showMsg("Account doesn't exist (Disable/deleted)")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        getHistory()
    }

    private fun signInUser(data: VFDData) {
        createAccount(data, signUp = false)
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

    private fun checkEmpty(firstname: String, email: String, lastname: String, username: String, password: String, phone: String): Boolean {
        if (firstname == "") {
            showMsg("Firstname not set")
            return true
        }
        if (lastname == "") {
            showMsg("LastName not set")
            return true
        }
        if (email == "") {
            showMsg("Email not set")
            return true
        }
        if (phone == "") {
            showMsg("Phone not set")
            return true
        }
        if (username == "") {
            showMsg("Username not set")
            return true
        }
        if (password == "") {
            showMsg("Username not set")
            return true
        }
        return false
    }

    private fun getHistory() {
        if (vfdData.token == "") return
        try {
            val header = "Authorization"
            val key = "Bearer ${vfdData.token}"
            val body: RequestBody = "".toRequestBody("application/json".toMediaTypeOrNull())
            val url = "https://howfarserver.online/v1/transaction/fetch"
            val client = OkHttpClient()
            val request = Request.Builder().url(url).addHeader(header, key).post(body).build()
            val response = client.newCall(request).execute()
            val jsonResponse = response.body?.string()
            val responseData = Gson().fromJson(jsonResponse, VfdTransferHistory::class.java)
            if (response.code == 200) {
                showMsg(responseData.message)
                responseData.message
            } else if (response.code in 400..499) {
                showMsg(responseData.message)
            } else {
                showMsg(response.message)
            }
        } catch (e: SocketTimeoutException) {
            showMsg("${e.message}")
        }
    }

    private fun createAccount(data: VFDData, signUp: Boolean = true) {
        showLoading()
        scope.launch {
            try {
                val header = "Authorization"
                val key = "Bearer 72668e1e-9edf-311c-b36a-45056bda2185"
                val body: RequestBody = if (signUp) Gson().toJson(data).toRequestBody("application/json".toMediaTypeOrNull())
                else Gson().toJson(EmailPassword(email = data.email, password = data.password)).toRequestBody("application/json".toMediaTypeOrNull())
                val url = "https://howfarserver.online/v1/${if (signUp) "signup" else "signin"}"
                val client = OkHttpClient()
                val request = Request.Builder().url(url).addHeader(header, key).post(body).build()
                val response = client.newCall(request).execute()
                val jsonResponse = response.body?.string()
                val responseData = Gson().fromJson(jsonResponse, SignUpResponse::class.java)
                println("JsonResponse ******************** $data ************************** $responseData")
                if (response.code == 200) {
                    data.token = responseData.data.token
                    data.account_number = responseData.data.account_number
                    data.balance = responseData.data.balance
                    data.bank_name = responseData.data.bank_name
                    walletsRef.setValue(data).addOnSuccessListener {
                        if (signUp) showMsg("Account created successfully")
                        runOnUiThread { showPayment() }
                    }
                } else if (response.code in 400..499) {
                    showMsg(responseData.message)
                    runOnUiThread { showWalletInput() }
                } else {
                    showMsg(responseData.message)
                    runOnUiThread { showWalletInput() }
                }
            } catch (e: SocketTimeoutException) {
                runOnUiThread { showPayment() }
                showMsg("${e.message}")
                runOnUiThread { showWalletInput() }
            }
        }
    }

    private fun showMsg(msg: String = "Date not set") {
        runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun drawer() {
        if (binding.root.isDrawerOpen(GravityCompat.START)) {
            binding.root.closeDrawer(GravityCompat.START)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.wallet_drawer_toggle -> drawer()
            R.id.nav_chat -> drawer()
            R.id.nav_send -> {
                val fragment = SendMoneyFragment()
                val bundle = Bundle()
                bundle.putString("data", Gson().toJson(vfdData))
                fragment.arguments = bundle
                supportFragmentManager.beginTransaction().addToBackStack("send")
                    .replace(R.id.wallet_root, fragment).commit()
                drawer()
            }
            R.id.nav_sign_out -> {
                drawer()
                onBackPressed()
            }
            R.id.account_copy -> {
                var text = binding.accountNumber.text.trim().toString()
                if (text == "") {
                    Toast.makeText(this, "No account number", Toast.LENGTH_LONG).show()
                    return
                }
                val clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Copied", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied", Toast.LENGTH_LONG).show()
            }
            R.id.banner_cancel -> {}
            R.id.send_money -> {
                val fragment = SendMoneyFragment()
                val bundle = Bundle()
                bundle.putString("data", Gson().toJson(vfdData))
                fragment.arguments = bundle
                supportFragmentManager.beginTransaction().addToBackStack("send")
                    .replace(R.id.wallet_root, fragment).commit()
            }
            R.id.request_money -> {}
            R.id.my_cards -> {}
            R.id.pay_bills -> {}
            R.id.send -> {
                hideKeyboard()
                val fragment = FragmentBankList()
                val bundle = Bundle()
                bundle.putString("token", vfdData.token)
                fragment.arguments = bundle
                supportFragmentManager.beginTransaction().addToBackStack("send").replace(R.id.wallet_root, fragment).commit()
            }
            R.id.create_account -> {
                hideKeyboard()
                val firstname = binding.firstName.text.toString().trim()
                val lastname = binding.lastName.text.toString().trim()
                val email = binding.email.text.toString().trim()
                val phone = binding.phone.text.toString().trim()
                val username = binding.username.text.toString().trim()
                val password = binding.password.text.toString().trim()
                val empty = checkEmpty(firstname = firstname, email = email, lastname = lastname, username = username, password = password, phone = phone)
                if (empty) return

                val data = VFDData(firstname = firstname, email = email, lastname = lastname, username = username, password = password, phone = phone)
                createAccount(data)
            }
            R.id.pay_close -> onBackPressed()
            R.id.create_wallet -> showWalletInput()
            R.id.wallet_close -> onBackPressed()
        }
    }
}

data class VfdBvnDob(
    var dateOfBirth: String = "",
    var bvn: String = "",
)

data class VFDCreateWalletResponse(
    var status: String = "",
    var message: String = "",
    var data: VFDData = VFDData(),
)

data class VFDData(
    var firstname: String = "",
    var email: String = "",
    var lastname: String = "",
    var username: String = "",
    var password: String = "",
    var phone: String = "",
    var account_number: String = "",
    var account_name: String = "",
    var bank_name: String = "",
    var balance: String = "",
    var token: String = "",
    var dob: String = "",
)

data class HowFarPayData(
    var secretKey: String = "",
    var signedOut: Boolean = false,
    var phone: String = "",
    var accountNumber: String = "",
    var bvn: String = "",
    var dob: String = "",
)

/*
class HistoryAdapter: RecyclerView.Adapter<HistoryAdapter.ViewHolder>(){
    var dataset = arrayListOf<VFDHistory>()

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //var view =
        return ViewHolder()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var datum = dataset[position]
    }

    override fun getItemCount() = dataset.size
}*/
