package com.azur.howfar.howfarwallet

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.azur.howfar.R
import com.azur.howfar.databinding.FragmentBankListBinding
import com.azur.howfar.viewmodel.VFDAccreditedBanksVieModel
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
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
    private var token = ""
    private val vFDTransferToVieModel by activityViewModels<VFDAccreditedBanksVieModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentBankListBinding.inflate(inflater, container, false)
        token = requireArguments().getString("token")!!
        fetchBanks()
        adapter.activity = requireActivity()
        adapter.vFDTransferToVieModel = vFDTransferToVieModel
        return binding.root
    }

    private fun hideLoading() {
        binding.banksLoading.visibility = View.GONE
    }

    private fun fetchBanks() {
        binding.banksLoading.visibility = View.VISIBLE
        try {
            val header = "Authorization"
            val key = "Bearer: $token"
            scope.launch {
                val url = "https://howfarserver.online/v1/bank/list"
                val client = OkHttpClient()
                val request = Request.Builder().url(url).addHeader(header, key).build()
                val response = client.newCall(request).execute()
                val jsonResponse = response.body?.string()
                if (response.code == 200) {
                    if (activity != null && isAdded) requireActivity().runOnUiThread {
                        hideLoading()
                        val banks = Gson().fromJson(jsonResponse, VFDBanks::class.java)
                        adapter.dataset = banks.data
                        binding.banksRv.adapter = adapter
                        binding.banksRv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                    }
                } else {
                    if (activity != null && isAdded) requireActivity().runOnUiThread { hideLoading() }
                }
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
    var dataset: ArrayList<VFDBanksList> = arrayListOf()
    lateinit var context: Context
    lateinit var activity: Activity
    lateinit var vFDTransferToVieModel: VFDAccreditedBanksVieModel

    init {
        setHasStableIds(true)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.bank_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.row_bank_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val datum = dataset[position]
        holder.name.text = datum.bankName
        if (holder.absoluteAdapterPosition - 1 >= 0 && holder.absoluteAdapterPosition + 1 < dataset.size) {
            //previousAndNext(holder)
        }

        holder.itemView.setOnClickListener {
            vFDTransferToVieModel.setBank(datum)
            (activity as AppCompatActivity).onBackPressed()
        }
    }

    override fun getItemCount() = dataset.size

    override fun getItemId(position: Int) = position.toLong()
}

data class VFDBanks(
    var status: String = "",
    var message: String = "",
    var data: ArrayList<VFDBanksList> = arrayListOf(),
)

data class VFDBanksList(
    var bankName: String = "",
    var bankCode: String = ""
)

data class VFDAccreditedBanks(
    var name: String = "",
    var code: String = ""
)

data class VFDTransferBank(
    var accountNumber: String = "",
    var bankCode: String = ""
)