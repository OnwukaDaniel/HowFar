package com.azur.howfar.howfarwallet

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.azur.howfar.R
import com.azur.howfar.databinding.FragmentConfirmTransferBinding
import com.azur.howfar.models.*
import com.azur.howfar.viewmodel.BooleanViewModel
import com.azur.howfar.viewmodel.TimeStringViewModel
import com.azur.howfar.viewmodel.VFDTransferInitViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
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

class FragmentConfirmBankTransfer : Fragment(), View.OnClickListener {
    private lateinit var binding: FragmentConfirmTransferBinding
    private var vfdAccountDetailsResponse = VfdAccountDetailsResponse()
    private var token = ""
    private val myAuth = FirebaseAuth.getInstance().currentUser!!.uid
    private var initData = VFDTransferInitData()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var historyRef = FirebaseDatabase.getInstance("https://howfar-b24ef-wallet.firebaseio.com").reference.child("history")
    private var timeRef = FirebaseDatabase.getInstance().reference
    private val booleanViewModel by activityViewModels<BooleanViewModel>()
    private val timeStringViewModel by activityViewModels<TimeStringViewModel>()
    private val vFDTransferInitViewModel by activityViewModels<VFDTransferInitViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentConfirmTransferBinding.inflate(inflater, container, false)
        hideProgress()
        timeRef = FirebaseDatabase.getInstance().reference.child("time").child(myAuth)
        historyRef = historyRef.child(myAuth)
        timeStringViewModel.time.observe(viewLifecycleOwner) {
            token = it
            println("vFDTransferInitViewModel ************************************** $token")
        }
        vFDTransferInitViewModel.vFDTransferInitData.observe(viewLifecycleOwner) {
            initData = it
        }
        val json = requireArguments().getString("data")
        vfdAccountDetailsResponse = Gson().fromJson(json, VfdAccountDetailsResponse::class.java)
        binding.accountName.text = vfdAccountDetailsResponse.data.accountName
        binding.accountNumber.text = vfdAccountDetailsResponse.data.accountNumber
        binding.banksName.text = vfdAccountDetailsResponse.data.bankName
        binding.transferProceed.setOnClickListener(this)
        return binding.root
    }

    private fun showProgress() {
        if (activity != null && isAdded) {
            requireActivity().runOnUiThread {
                binding.transferProceedProgress.visibility = View.VISIBLE
                binding.transferProceed.visibility = View.GONE
            }
        }
    }

    private fun hideProgress() {
        if (activity != null && isAdded) {
            requireActivity().runOnUiThread {
                binding.transferProceedProgress.visibility = View.GONE
                binding.transferProceed.visibility = View.VISIBLE
            }
        }
    }

    private fun initiatePayment() {
        showProgress()
        val data = initData
        scope.launch {
            try {
                val header = "Authorization"
                val key = "Bearer $token"
                val body: RequestBody = Gson().toJson(data).toRequestBody("application/json".toMediaTypeOrNull())
                val url = "https://howfarserver.online/v1/transfer/bank/init"
                val client = OkHttpClient()
                val request = Request.Builder().url(url).addHeader(header, key).post(body).build()
                val response = client.newCall(request).execute()
                val jsonResponse = response.body?.string()
                val responseData = Gson().fromJson(jsonResponse, VfdTransferInitResponse::class.java)
                if (response.code == 200) {
                    showMsg(responseData.message)
                    val d = VfdReference(responseData.data.reference)
                    completePayment(d)
                } else if (response.code in 400..499) {
                    showMsg(responseData.message)
                    hideProgress()
                    failure(responseData.message)
                } else {
                    showMsg(response.message)
                    hideProgress()
                    failure(responseData.message)
                }
            } catch (e: SocketTimeoutException) {
                showMsg("${e.message}")
                hideProgress()
                failure(e.message!!)
            }
        }
    }

    private fun completePayment(data: VfdReference) {
        try {
            val header = "Authorization"
            val key = "Bearer $token"
            val body: RequestBody = Gson().toJson(data).toRequestBody("application/json".toMediaTypeOrNull())
            val url = "https://howfarserver.online/v1/transfer/complete"
            val client = OkHttpClient()
            val request = Request.Builder().url(url).addHeader(header, key).post(body).build()
            val response = client.newCall(request).execute()
            val jsonResponse = response.body?.string()
            val responseData = Gson().fromJson(jsonResponse, VfdTransferInitResponse::class.java)
            if (response.code == 200) {
                showMsg(responseData.message)
                if (activity != null && isAdded) {
                    requireActivity().runOnUiThread {
                        responseData.message
                        booleanViewModel.setSwitch(true)
                        val historyData = WalletHistoryData(
                            myUid = myAuth,
                            amount = initData.amount,
                            reference = responseData.data.reference,
                            bankWallet = TranBank.BANK,
                            description = "Debit Bank transaction to ${initData.accountNumber}",
                            direction = TranDirection.SENT,
                            otherUid = "",
                        )
                        timeRef.setValue(ServerValue.TIMESTAMP).addOnSuccessListener {
                            timeRef.get().addOnSuccessListener { snapshot ->
                                if (snapshot.exists()) {
                                    val rawTime = snapshot.value.toString()
                                    historyRef.child(rawTime).setValue(historyData)
                                    success()
                                }
                            }
                        }
                    }
                }
                hideProgress()
            } else if (response.code in 400..499) {
                showMsg(responseData.message)
                hideProgress()
                failure(responseData.message)
            } else {
                showMsg(response.message)
                hideProgress()
                failure(responseData.message)
            }
        } catch (e: SocketTimeoutException) {
            showMsg("${e.message}")
            hideProgress()
            failure(e.message!!)
        }
    }

    private fun showMsg(msg: String) {
        if (activity != null && isAdded) {
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun success() {
        if (activity != null && isAdded) {
            requireActivity().runOnUiThread {
                requireActivity().finish()
                startActivity(
                    Intent(requireContext(), SuccessFailure::class.java)
                        .putExtra("amount", initData.amount)
                        .putExtra("name", initData.accountNumber)
                        .putExtra("isSuccess", true)
                )
                requireActivity().overridePendingTransition(R.anim.enter_right_to_left, R.anim.exit_right_to_left)
            }
        }
    }

    private fun failure(message: String) {
        if (activity != null && isAdded) {
            requireActivity().runOnUiThread {
                requireActivity().finish()
                startActivity(
                    Intent(requireContext(), SuccessFailure::class.java)
                        .putExtra("amount", initData.amount)
                        .putExtra("message", message)
                        .putExtra("isSuccess", false)
                )
                requireActivity().overridePendingTransition(R.anim.enter_right_to_left, R.anim.exit_right_to_left)
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.transfer_proceed -> initiatePayment()
        }
    }
}