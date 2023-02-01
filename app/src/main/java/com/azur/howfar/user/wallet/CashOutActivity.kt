package com.azur.howfar.user.wallet

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.azur.howfar.R
import com.azur.howfar.activity.BaseActivity
import com.azur.howfar.activity.ContactUsActivity
import com.azur.howfar.databinding.ActivityCashOutBinding
import com.azur.howfar.howfarchat.ChatLanding
import com.azur.howfar.livedata.ValueEventLiveData
import com.azur.howfar.models.EventListenerType.onDataChange
import com.azur.howfar.utils.HFCoinUtils
import com.azur.howfar.utils.Util
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class CashOutActivity : BaseActivity(), View.OnClickListener {
    private val binding by lazy { ActivityCashOutBinding.inflate(layoutInflater) }
    private var myAuth = FirebaseAuth.getInstance().currentUser!!.uid
    private val redeemHistoryAdapter = RedeemHistoryAdapter()
    private var dataset = arrayListOf<WithDrawRequestData>()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.cashOutBack.setOnClickListener(this)
        binding.btncountinue.setOnClickListener(this)
        redeemHistoryAdapter.dataset = dataset
        binding.rvHistory.adapter = redeemHistoryAdapter
        binding.rvHistory.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        val reqRef = FirebaseDatabase.getInstance().reference.child(MY_WITHDRAW_REQUEST).child(myAuth)
        ValueEventLiveData(reqRef).observe(this) {
            when (it.second) {
                onDataChange -> {
                    dataset.clear()
                    for (i in it.first.children) {
                        val withDrawRequestData = i.getValue(WithDrawRequestData::class.java)!!
                        if (withDrawRequestData !in dataset) dataset.add(withDrawRequestData)
                        redeemHistoryAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.cash_out_Back -> onBackPressed()
            R.id.btncountinue -> {
                val bank = binding.etBank.text.trim().toString()
                val account = binding.etAccount.text.trim().toString()
                val amount = binding.etAmount.text.trim().toString()
                val accountName = binding.etAccountName.text.trim().toString()
                if (bank == "" || account == "" || amount == "" || bank.length < 4 || accountName == "") return
                binding.etBank.text.clear()
                binding.etAccount.text.clear()
                binding.etAmount.text.clear()
                binding.etAccountName.text.clear()


                val alertDialog = AlertDialog.Builder(this)
                alertDialog.setTitle("Message")
                alertDialog.setMessage("You will be credited ₦ ${amount.toInt() * 0.9} in few minutes")
                alertDialog.setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    val historyRef = FirebaseDatabase.getInstance().reference.child(ChatLanding.TRANSFER_HISTORY).child(myAuth)
                    historyRef.get().addOnSuccessListener {
                        if (it.exists()) {
                            val available = HFCoinUtils.checkBalance(it)
                            val balance = available.toString()
                            if (amount.toFloat() > balance.toFloat()) {
                                showToast("Insufficient fund")
                            } else {
                                val amountCredited = (amount.toInt() * 0.9).toString()
                                //val amountDebited = amount.toFloat() - amountCredited.toFloat()
                                val withDrawRequest = WithDrawRequestData(
                                    amount = amountCredited, accountName = accountName,
                                    bank = bank, account = account.toInt(), uid = myAuth
                                )
                                sendLogic(withDrawRequest, amount.toFloat(), amount.toFloat())
                            }
                        }
                    }
                }
                alertDialog.create().show()
            }
        }
    }

    private fun sendLogic(withDrawRequest: WithDrawRequestData, amtCredit: Float, amtDebit: Float) {
        var requestData = withDrawRequest
        val timeRef = FirebaseDatabase.getInstance().reference.child("time").child(myAuth)
        timeRef.setValue(ServerValue.TIMESTAMP).addOnSuccessListener {
            timeRef.get().addOnSuccessListener { timeSnap ->
                val time = timeSnap.value.toString()
                requestData.time
                val creditChargesRef = FirebaseDatabase.getInstance(OVERTIME_CHANGE_URL).reference.child(CASH_OUT_ACCOUNT_UID)
                creditChargesRef.get().addOnSuccessListener { cashUidSnap ->
                    if (cashUidSnap.exists()) {
                        when (val uid = cashUidSnap.value.toString()) {
                            "" -> showToast("Unable to perform transaction now. Please retry\nError code $CASH_OUT_CODE_NO_UID")
                            else -> {
                                HFCoinUtils.send(amountCredit = amtCredit, amountDebit = amtDebit, receiverUid = uid, senderUid = myAuth)
                                val reqRef = FirebaseDatabase.getInstance().reference.child(MY_WITHDRAW_REQUEST).child(myAuth).child(time)
                                reqRef.setValue(withDrawRequest).addOnSuccessListener { showToast("Request sent") }
                            }
                        }
                    } else showToast("Unable to perform transaction now. Please retry later.\nError code $CASH_OUT_CODE_NO_UID")
                }.addOnFailureListener { showToast(it.message!!.toString()) }
            }
        }
    }

    private fun showToast(message: String = "Please retry") {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        const val MY_WITHDRAW_REQUEST = "MY_WITHDRAW_REQUEST"
        const val CASH_OUT_ACCOUNT_UID = "CASH_OUT_ACCOUNT_UID"

        const val OVERTIME_CHANGE_URL = "https://howfar-b24ef-overtime-change.firebaseio.com"
        const val CASH_OUT_CODE_NO_UID = 100
    }
}

data class WithDrawRequestData(
    var amount: String = "",
    var accountName: String = "",
    var bank: String = "",
    var account: Int = 0,
    var time: String = "",
    var reply: String = "",
    var uid: String = "",
    var status: Int = WithDrawRequestStatus.PENDING,
)

object WithDrawRequestStatus {
    const val PENDING = 0
    const val APPROVED = 1
    const val REJECTED = 2
}

class RedeemHistoryAdapter : RecyclerView.Adapter<RedeemHistoryAdapter.RedeemHistoryViewHolder>() {
    lateinit var context: Context
    var dataset = arrayListOf<WithDrawRequestData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RedeemHistoryViewHolder {
        context = parent.context
        return RedeemHistoryViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_reedem_history, parent, false))
    }

    override fun onBindViewHolder(holder: RedeemHistoryViewHolder, position: Int) = holder.setData(dataset[position])

    override fun getItemCount() = dataset.size

    inner class RedeemHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCoin: TextView = itemView.findViewById(R.id.tvCoin)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        fun setData(datum: WithDrawRequestData) {
            tvCoin.text = "${datum.amount} HFCoin"
            tvTime.text = Util.formatSmartDateTime(datum.time)
            tvStatus.text = when (datum.status) {
                WithDrawRequestStatus.PENDING -> "PENDING"
                WithDrawRequestStatus.REJECTED -> {
                    itemView.setOnClickListener {
                        val alert = AlertDialog.Builder(context)
                        alert.setTitle("Message")
                        alert.setMessage("${datum.reply}\n\nContact support if you have further questions.")
                        alert.setPositiveButton("Contact support") { dialog, _ ->
                            dialog.dismiss()
                            val intent = Intent(context, ContactUsActivity::class.java)
                            context.startActivity(intent)
                        }
                        alert.create().show()
                    }
                    "REJECTED"
                }
                else -> "APPROVED"
            }
        }
    }
}