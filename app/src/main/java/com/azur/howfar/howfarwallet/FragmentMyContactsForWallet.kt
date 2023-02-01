package com.azur.howfar.howfarwallet

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.azur.howfar.R
import com.azur.howfar.databinding.FragmentMyContactsForWalletBinding
import com.azur.howfar.howfarchat.groupChat.Contact
import com.azur.howfar.howfarchat.groupChat.ContactSelectHelper
import com.azur.howfar.models.UserProfile
import com.azur.howfar.utils.Util
import com.azur.howfar.viewmodel.ContactViewModel
import kotlinx.coroutines.*

class FragmentMyContactsForWallet : Fragment(), WalletContactSelectionHelper {
    private lateinit var binding: FragmentMyContactsForWalletBinding
    private val scope = CoroutineScope(Dispatchers.IO)
    private var contactList: java.util.ArrayList<Contact> = arrayListOf()
    private val contactFormattedList: java.util.ArrayList<String> = arrayListOf()
    private var userContacts: ArrayList<UserProfile> = arrayListOf()
    private val myAuth = FirebaseAuth.getInstance().currentUser!!.uid
    private var listOfUsers: ArrayList<UserProfile> = arrayListOf()
    private var usersRef = FirebaseDatabase.getInstance().reference
    private val walletContactsAdapter = WalletContactsAdapter()
    private val contactViewModel: ContactViewModel by activityViewModels()

    @SuppressLint("NotifyDataSetChanged")
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        when {
            permissions[Manifest.permission.READ_CONTACTS] == true -> {
            }
            permissions[Manifest.permission.READ_CONTACTS] == false -> {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED) {
                    Snackbar.make(binding.root, "HowFar needs permission to find registered contacts from your contact", Snackbar.LENGTH_LONG).show()
                    runBlocking {
                        delay(3000)
                        if (activity != null && isAdded) requireActivity().runOnUiThread {
                            requireActivity().onBackPressed()
                        } else {
                            try {
                                requireActivity().onBackPressed()
                            } catch (e: Exception){}
                        }
                    }
                }
            }
        }
    }

    private fun askPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
    }

    val listener = object : ValueEventListener {
        @SuppressLint("NotifyDataSetChanged")
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists()) {
                val values = (snapshot.value as HashMap<*, *>).values
                val json = Gson().toJson(values)
                val list = Gson().fromJson(json, java.util.ArrayList::class.java)
                for (i in list) {
                    val user: UserProfile = Gson().fromJson(Gson().toJson(i), UserProfile::class.java)
                    if (user !in listOfUsers) listOfUsers.add(user)
                }
                for (contact in contactList) contactFormattedList.add(Util.formatNumber(contact.mobileNumber))
                for (user in listOfUsers) if (Util.formatNumber(user.phone) in contactFormattedList) userContacts.add(user)

                // DUPLICATE
                val holderList: ArrayList<UserProfile> = arrayListOf()
                for (i in userContacts) holderList.add(i)

                for (i in holderList) if (i.uid == myAuth) userContacts.remove(i)
                walletContactsAdapter.notifyDataSetChanged()
            }
        }

        override fun onCancelled(error: DatabaseError) = Unit
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        askPermission()
        binding = FragmentMyContactsForWalletBinding.inflate(inflater, container, false)
        usersRef = usersRef.child("user_details")
        usersRef.addValueEventListener(listener)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        usersRef.removeEventListener(listener)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        runBlocking {
            val contactsJob = scope.async { Util.getAllSavedContacts(requireActivity()).first }
            contactsJob.join()
            contactList = contactsJob.await()

            walletContactsAdapter.dataset = userContacts
            walletContactsAdapter.contactViewModel = contactViewModel
            walletContactsAdapter.activity = requireActivity()
            binding.walletContactsRv.adapter = walletContactsAdapter
            walletContactsAdapter.walletContactSelectionHelper = this@FragmentMyContactsForWallet
            binding.walletContactsRv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
    }

    override fun selectContact(user: UserProfile) {
        contactViewModel.setUserProfile(user)
    }
}

class WalletContactsAdapter : RecyclerView.Adapter<WalletContactsAdapter.ViewHolder>() {

    lateinit var walletContactSelectionHelper: WalletContactSelectionHelper
    var dataset: ArrayList<UserProfile> = arrayListOf()
    lateinit var contactViewModel: ContactViewModel
    var selectedDataset: ArrayList<UserProfile> = arrayListOf()
    lateinit var context: Context
    lateinit var activity: Activity
    lateinit var contactSelectHelper: ContactSelectHelper

    init {
        setHasStableIds(true)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ShapeableImageView = itemView.findViewById(R.id.contact_card_image)
        val name: TextView = itemView.findViewById(R.id.contact_card_name)
        val number: TextView = itemView.findViewById(R.id.contact_card_phone)
        val card: CardView = itemView.findViewById(R.id.row_contact_card)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.row_contact_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val datum = dataset[position]
        holder.name.text = datum.name
        holder.number.text = datum.phone
        if (datum in selectedDataset) {
            holder.card.setCardBackgroundColor(Color.parseColor("#4961FF"))
            holder.image.setImageResource(R.drawable.ic_check_circle)
        } else {
            holder.card.setCardBackgroundColor(Color.parseColor("#222222"))
            holder.image.setImageResource(R.drawable.ic_avatar)
        }

        holder.card.setOnClickListener {
            val selectedDatum = dataset[holder.bindingAdapterPosition]
            walletContactSelectionHelper.selectContact(selectedDatum)
            contactViewModel.setUserProfile(selectedDatum)
            (activity as AppCompatActivity).supportFragmentManager.beginTransaction().addToBackStack("send").replace(
                R.id.wallet_root,
                FragmentSendMoney()
            ).commit()
        }
    }

    override fun getItemCount() = dataset.size

    override fun getItemId(position: Int) = position.toLong()
}

interface WalletContactSelectionHelper {
    fun selectContact(user: UserProfile)
}