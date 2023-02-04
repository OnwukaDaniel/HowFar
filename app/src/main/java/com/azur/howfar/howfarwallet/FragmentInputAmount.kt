package com.azur.howfar.howfarwallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.azur.howfar.databinding.FragmentInputAmountBinding
import com.azur.howfar.viewmodel.ContactViewModel

class FragmentInputAmount : Fragment() {
    private lateinit var binding: FragmentInputAmountBinding
    private val contactViewModel: ContactViewModel by activityViewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentInputAmountBinding.inflate(inflater, container, false)
        contactViewModel.userProfile.observe(viewLifecycleOwner) {
            binding.inputUsername.text = it.name
        }
        return binding.root
    }
}