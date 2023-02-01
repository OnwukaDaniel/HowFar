package com.azur.howfar.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.azur.howfar.activity.ThreeSignUpData
import com.azur.howfar.models.UserSignUp
import java.io.ByteArrayInputStream

class SignUpViewModel: ViewModel() {
    var startCountDown = MutableLiveData( false)
    var gender = MutableLiveData( "")
    var three = MutableLiveData<ThreeSignUpData>()
    var imageStream = MutableLiveData<Pair<ByteArrayInputStream, ByteArray>>()
    var userSignUp = MutableLiveData(UserSignUp())
    fun setUserSignUp(input: UserSignUp){
        userSignUp.value = input
    }
    fun setStartCountdown(input: Boolean){
        startCountDown.value = input
    }
    fun setGender(input: String){
        gender.value = input
    }
    fun setThreeInput(input: ThreeSignUpData){
        three.value = input
    }
    fun setImageStream(input: Pair<ByteArrayInputStream, ByteArray>){
        imageStream.value = input
    }
}