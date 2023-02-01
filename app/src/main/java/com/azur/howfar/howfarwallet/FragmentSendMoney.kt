package com.azur.howfar.howfarwallet

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.azur.howfar.databinding.FragmentSendMoneyBinding
import com.azur.howfar.viewmodel.ContactViewModel

class FragmentSendMoney : Fragment() {
    private lateinit var binding: FragmentSendMoneyBinding
    private val sendMoneyViewModel by activityViewModels<SendMoneyViewModel>()
    private val contactViewModel: ContactViewModel by activityViewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSendMoneyBinding.inflate(inflater, container, false)
        binding.amountInput.addTextChangedListener(AmountTextWatcher())
        contactViewModel.userProfile.observe(viewLifecycleOwner) {
            binding.personName.text = it.name
        }
        sendMoneyViewModel.amount.observe(viewLifecycleOwner) {
            binding.amountToSend.text = it
        }
        return binding.root
    }

    inner class AmountTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) = sendMoneyViewModel.inputAmount(s.toString())
    }
}

class SendMoneyViewModel : ViewModel() {
    var amount = MutableLiveData<String>()
    fun inputAmount(input: String) {
        amount.value = input
    }
}