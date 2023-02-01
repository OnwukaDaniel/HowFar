package com.azur.howfar.user.wallet

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.azur.howfar.databinding.FragmentRechargeBinding
import com.azur.howfar.howfarchat.ChatLanding
import com.azur.howfar.models.CoinPlan
import com.azur.howfar.models.Currency
import com.azur.howfar.models.TransactionType
import com.azur.howfar.models.UserProfile
import com.azur.howfar.payment.FlutterWaveResponse
import com.azur.howfar.user.EditProfileActivity
import com.azur.howfar.viewmodel.FloatViewModel
import com.azur.howfar.viewmodel.UserProfileViewmodel
import com.flutterwave.raveandroid.RavePayActivity
import com.flutterwave.raveandroid.RaveUiManager
import com.flutterwave.raveandroid.rave_java_commons.RaveConstants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.gson.Gson
import org.jetbrains.annotations.Nullable

class RechargeFragment : Fragment(), CoinPurchaseAdapter.OnCoinPlanClickListener {
    private lateinit var binding: FragmentRechargeBinding
    private var coinPurchaseAdapter = CoinPurchaseAdapter()
    private val myAuth = FirebaseAuth.getInstance().currentUser!!.uid
    private val floatViewModel: FloatViewModel by activityViewModels()
    private val userProfileViewmodel: UserProfileViewmodel by activityViewModels()
    private var userProfile = UserProfile()

    private var selectedPlan: CoinPlan? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentRechargeBinding.inflate(inflater, container, false)
        initMain()
        return binding.root
    }

    private fun initMain() {
        userProfileViewmodel.userProfile.observe(viewLifecycleOwner) {
            userProfile = it
        }
        floatViewModel.float.observe(viewLifecycleOwner) { binding.tvHFCoin.text = it.toString() }
        val list = arrayListOf(
            CoinPlan(coin = 500, amount = 500, label = "# 500"),
            CoinPlan(coin = 1000, amount = 1000, label = "# 1000"),
            CoinPlan(coin = 2000, amount = 2000, label = "# 2000"),
            CoinPlan(coin = 3000, amount = 3000, label = "# 3000"),
            CoinPlan(coin = 5000, amount = 5000, label = "# 5000"),
        )
        coinPurchaseAdapter.onCoinPlanClickListener = this
        coinPurchaseAdapter.coinList = list
        binding.rvRecharge.adapter = coinPurchaseAdapter
    }

    override fun onPlanClick(coinPlan: CoinPlan) {
        val myAuth = FirebaseAuth.getInstance().currentUser!!.uid
        val myProfileRef = FirebaseDatabase.getInstance().reference.child(USER_DETAILS).child(myAuth)
        myProfileRef.get().addOnSuccessListener {
            if (it.exists()) {
                val myProfile = it.getValue(UserProfile::class.java)!!
                when (myProfile.email) {
                    "" -> {
                        val alert = AlertDialog.Builder(requireContext())
                        alert.setTitle("Email receipt")
                        alert.setMessage("You don't have your email set up.\nSet up your email to get payment receipt.")
                        alert.setPositiveButton("Set Up Email") { dialog, _ ->
                            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
                            dialog.dismiss()
                        }
                        alert.setNegativeButton("Continue without receipt") { dialog, _ ->
                            makePayment(coinPlan, myProfile)
                            dialog.dismiss()
                        }
                        alert.create().show()
                    }
                    else -> makePayment(coinPlan, myProfile)
                }
            }
        }
        selectedPlan = coinPlan
    }

    private fun makePayment(coinPlan: CoinPlan, userProfile: UserProfile) {
        RaveUiManager(this).setAmount(coinPlan.coin.toDouble())
            .setCurrency("NGN")
            .setEmail(userProfile.email)
            .setfName("HowFar user")
            .setlName(userProfile.name)
            .setNarration("Purchase HFCoin")
            .setPublicKey("FLWPUBK_TEST-21cabcc53547ba8072bc1dcd07379f15-X")
            .setEncryptionKey("FLWSECK_TESTa88c0ec28228")
            .setTxRef(System.currentTimeMillis().toString() + "Ref")
            .setPhoneNumber(userProfile.phone, true)
            .acceptAccountPayments(true)
            .acceptCardPayments(true)
            .acceptMpesaPayments(true)
            .acceptAchPayments(true)
            .acceptGHMobileMoneyPayments(true)
            .acceptUgMobileMoneyPayments(true)
            .acceptZmMobileMoneyPayments(true)
            .acceptRwfMobileMoneyPayments(true)
            .acceptSaBankPayments(true)
            .acceptUkPayments(true)
            .acceptBankTransferPayments(true)
            .acceptUssdPayments(true)
            .acceptBarterPayments(true)
            .allowSaveCardFeature(true)
            .onStagingEnv(true)
            .shouldDisplayFee(true)
            .showStagingLabel(true)
            .initialize()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        println("onActivityResult ****************************************************** $resultCode")
        when (resultCode) {
            RavePayActivity.RESULT_SUCCESS -> {
                if (requestCode == RaveConstants.RAVE_REQUEST_CODE && data != null) {
                    val message = data.getStringExtra("response")
                    val flutterWaveResponse = Gson().fromJson(message, FlutterWaveResponse::class.java)
                    val timeRef = FirebaseDatabase.getInstance().reference.child("time").child(myAuth)
                    if (resultCode == RavePayActivity.RESULT_SUCCESS) {
                        timeRef.setValue(ServerValue.TIMESTAMP).addOnSuccessListener {
                            timeRef.get().addOnSuccessListener { timeSnapshot ->
                                if (timeSnapshot.exists()) {
                                    val timeSent = timeSnapshot.value.toString()
                                    val historyRef = FirebaseDatabase.getInstance().reference.child(ChatLanding.TRANSFER_HISTORY).child(myAuth).child(timeSent)
                                    val currency = Currency(
                                        timeOfTransaction = timeSent,
                                        senderUid = myAuth,
                                        receiverUid = myAuth,
                                        transactionType = TransactionType.BOUGHT,
                                        hfcoin = flutterWaveResponse.data.amount
                                    )
                                    historyRef.setValue(currency).addOnSuccessListener {
                                        Toast.makeText(requireContext(), "${flutterWaveResponse.data.chargeResponseMessage} SUCCESS ", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    } else if (resultCode == RavePayActivity.RESULT_ERROR) {
                        println("Flutterwave message ****************************************************** $flutterWaveResponse")
                        Toast.makeText(requireContext(), "FAILED ${flutterWaveResponse.message}", Toast.LENGTH_LONG).show()
                    } else if (resultCode == RavePayActivity.RESULT_CANCELLED) {
                        println("Flutterwave message ****************************************************** $flutterWaveResponse")
                        Toast.makeText(requireContext(), "CANCELLED ${flutterWaveResponse.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            RavePayActivity.RESULT_CANCELLED -> {}
            RavePayActivity.RESULT_ERROR -> {
                Toast.makeText(requireContext(), "FAILED.", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val TRANSFER_HISTORY = "user_coins_transfer"
        const val USER_DETAILS = "user_details"
        const val CHAT_REFERENCE = "chat_reference"
    }
}