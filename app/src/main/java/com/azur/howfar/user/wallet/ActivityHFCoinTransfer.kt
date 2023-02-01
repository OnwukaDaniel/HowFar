package com.azur.howfar.user.wallet

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.azur.howfar.R
import com.azur.howfar.databinding.ActivityHfcoinTransferBinding
import com.azur.howfar.livedata.ValueEventLiveData
import com.azur.howfar.models.EventListenerType
import com.azur.howfar.models.UserProfile
import com.azur.howfar.viewmodel.UserProfileViewmodel

class ActivityHFCoinTransfer : AppCompatActivity(), View.OnClickListener, TouchHelper {
    private val binding by lazy { ActivityHfcoinTransferBinding.inflate(layoutInflater) }
    private val transferUserAdapter = TransferUserAdapter()
    private val searchUserAdapter = TransferUserAdapter()
    private val datasetSearchAble: ArrayList<UserProfile> = arrayListOf()
    private val dataSet: ArrayList<UserProfile> = arrayListOf()
    private val profileViewmodel: UserProfileViewmodel by viewModels()
    private lateinit var pref: SharedPreferences
    private var thisActiveUser = UserProfile()
    private var thisUser = UserProfile()
    private lateinit var valueEventLiveData: ValueEventLiveData

    inner class InputTextWatcher : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit

        override fun afterTextChanged(p0: Editable?) {
            if (p0.toString().trim().isEmpty()) normalMode()
            else {
                searchMode(p0.toString().trim().lowercase())
            }
        }
    }

    private fun searchMode(trim: String) {
        datasetSearchAble.clear()
        binding.transferRv.visibility = View.GONE
        binding.searchRv.visibility = View.VISIBLE
        for (i in dataSet) {
            if (trim in i.phone || trim in i.name.lowercase()) {
                datasetSearchAble.add(i)
                searchUserAdapter.notifyItemInserted(datasetSearchAble.size)
            }
        }
    }

    private fun normalMode() {
        binding.transferRv.visibility = View.VISIBLE
        binding.searchRv.visibility = View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        pref = getSharedPreferences(getString(R.string.ALL_PREFERENCE), Context.MODE_PRIVATE)
        val json = pref.getString(getString(R.string.this_user), "")
        thisUser = Gson().fromJson(json, UserProfile::class.java)
        binding.transferSearch.addTextChangedListener(InputTextWatcher())
        initFirebase()

        searchUserAdapter.dataset = datasetSearchAble
        transferUserAdapter.touchHelper = this
        searchUserAdapter.activity = this
        binding.transferRv.adapter = searchUserAdapter
        binding.transferRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        transferUserAdapter.touchHelper = this
        transferUserAdapter.dataset = dataSet
        transferUserAdapter.activity = this
        binding.transferRv.adapter = transferUserAdapter
        binding.transferRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    private fun initFirebase() {
        val dataKeys: ArrayList<String> = arrayListOf()
        val ref = FirebaseDatabase.getInstance().reference.child(USER_DETAILS)
        valueEventLiveData = ValueEventLiveData(ref)
        valueEventLiveData.observe(this) {
            when (it?.second) {
                EventListenerType.onDataChange -> {
                    val json = Gson().toJson((it.first.value as HashMap<*, *>).values)
                    val dd = Gson().fromJson(json, ArrayList::class.java)
                    for (i in dd) {
                        hasData()
                        val jsonX = Gson().toJson(i)
                        val data: UserProfile = Gson().fromJson(jsonX, UserProfile::class.java)
                        if (data.uid !in dataKeys && data.uid != thisUser.uid) {
                            dataKeys.add(data.uid)
                            dataSet.add(data)
                            transferUserAdapter.notifyItemInserted(dataSet.size)
                        }
                    }
                }
            }
        }
    }

    private fun hasData(){
        binding.transferProgress.visibility = View.GONE
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.enter_left_to_right, R.anim.exit_left_to_right)
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
        }
    }

    override fun helpTouch(datum: UserProfile) {
        profileViewmodel.setUserProfile(datum)
    }
    companion object {
        val TRANSFER_HISTORY = "user_coins_transfer"
        val USER_DETAILS = "user_details"
    }
}

class TransferUserAdapter : RecyclerView.Adapter<TransferUserAdapter.ViewHolder>() {
    private lateinit var context: Context
    lateinit var activity: Activity
    lateinit var touchHelper: TouchHelper
    var dataset: ArrayList<UserProfile> = arrayListOf()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.transfer_name)
        val phone: TextView = itemView.findViewById(R.id.transfer_phone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.row_hf_transfer_contact_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val datum = dataset[position]
        holder.name.text = datum.name
        holder.phone.text = datum.phone
        holder.itemView.setOnClickListener {
            touchHelper.helpTouch(datum)
            val json = Gson().toJson(datum)
            val bundle = Bundle()
            bundle.putString("data", json)
            val fragment = FragmentPaymentInput()
            fragment.arguments = bundle
            (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                .addToBackStack("user")
                .setCustomAnimations(R.anim.enter_right_to_left, R.anim.exit_right_to_left)
                .replace(R.id.transfer_root, fragment)
                .commit()
        }
    }

    override fun getItemCount() = dataset.size
}

interface TouchHelper{
    fun helpTouch(datum: UserProfile)
}