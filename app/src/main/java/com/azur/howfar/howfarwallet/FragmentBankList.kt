package com.azur.howfar.howfarwallet

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.azur.howfar.R
import com.azur.howfar.databinding.FragmentBankListBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.SocketTimeoutException

class FragmentBankList : Fragment() {
    private lateinit var binding: FragmentBankListBinding
    private val scope = CoroutineScope(Dispatchers.IO)
    private val adapter = BanksAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentBankListBinding.inflate(inflater, container, false)
        fetchBanks()
        return binding.root
    }

    private fun hideLoading() {
        binding.banksLoading.visibility = View.GONE
    }

    private fun fetchBanks() {
        binding.banksLoading.visibility = View.VISIBLE
        try {
            val header = "Authorization"
            val key = ": Bearer 72668e1e-9edf-311c-b36a-45056bda2185"
            val ref = FirebaseDatabase.getInstance().reference.child("howFar").child("vfd_credentials")

            ref.get().addOnSuccessListener {
                scope.launch {
                    val url = "https://devesb.vfdbank.systems:8263/vfd-wallet/1.1/wallet2/bank"
                    val client = OkHttpClient()
                    val request = Request.Builder().url(url).addHeader(header, key).build()
                    val response = client.newCall(request).execute()
                    val jsonResponse = response.body?.string()
                    println("Code ********************************************** ${response.code}")
                    println("JsonResponse ********************************************** $jsonResponse")
                    if (response.code == 200) {
                        if (activity != null && isAdded) requireActivity().runOnUiThread{
                            hideLoading()
                            val banks = Gson().fromJson(jsonResponse, VFDBanks::class.java)
                            adapter.dataset = banks.data.bank
                            binding.banksRv.adapter = adapter
                            binding.banksRv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                        }
                    } else {
                        if (activity != null && isAdded) requireActivity().runOnUiThread { hideLoading() }
                    }
                }
            }.addOnFailureListener {
                showMsg("Network exception")
                if (activity != null && isAdded) requireActivity().runOnUiThread { hideLoading() }
            }
        } catch (e: SocketTimeoutException) {
            showMsg("Time out")
            if (activity != null && isAdded) requireActivity().runOnUiThread { hideLoading() }
        }
    }

    private fun showMsg(msg: String = "Date not set") {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
    }
}

class BanksAdapter : RecyclerView.Adapter<BanksAdapter.ViewHolder>() {
    var dataset: ArrayList<VFDAccreditedBanks> = arrayListOf()
    lateinit var context: Context
    lateinit var activity: Activity
    private val alpha = arrayListOf("A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z")

    init {
        setHasStableIds(true)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val alph: TextView = itemView.findViewById(R.id.bank_alpha)
        val name: TextView = itemView.findViewById(R.id.bank_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.row_bank_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val datum = dataset[position]
        holder.name.text = datum.name
        if (holder.absoluteAdapterPosition == 0) holder.alph.text = alpha[0]
        if (holder.absoluteAdapterPosition - 1 >= 0 && holder.absoluteAdapterPosition + 1 < dataset.size) {
            //previousAndNext(holder)
        }

        holder.itemView.setOnClickListener {

            //(activity as AppCompatActivity).supportFragmentManager.beginTransaction().addToBackStack("send").replace(R.id.wallet_root, FragmentSendMoney())
            //    .commit()
        }
    }

    private fun previousAndNext(holder: ViewHolder) {
        val position = holder.absoluteAdapterPosition
        val nextPosition = holder.absoluteAdapterPosition + 1
        val previousPosition = holder.absoluteAdapterPosition - 1
        if (dataset[position].code != dataset[previousPosition].code) {
            holder.alph.visibility = View.VISIBLE
            holder.alph.text = alpha[position]
        }
        if (dataset[position].code != dataset[nextPosition].code) {
            holder.alph.visibility = View.VISIBLE
            holder.alph.text = dataset[position].code
        }
    }

    override fun getItemCount() = dataset.size

    override fun getItemId(position: Int) = position.toLong()
}

data class VFDBanks(
    var status: String = "",
    var message: String = "",
    var data: VFDBanksList = VFDBanksList(),
)

data class VFDBanksList(
    var bank: ArrayList<VFDAccreditedBanks> = arrayListOf()
)

data class VFDAccreditedBanks(
    var name: String = "",
    var code: String = ""
)