package com.azur.howfar.utils

import com.azur.howfar.models.*
import com.azur.howfar.retrofit.Const
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

object HFCoinUtils {
    const val TRANSFER_HISTORY = "user_coins_transfer"

    fun checkBalance(it: DataSnapshot): Float {
        var available = 0F
        for (i in it.children) {
            val dataCurrency: Currency = i.getValue(Currency::class.java)!!
            when (dataCurrency.transactionType) {
                TransactionType.SENT -> available -= dataCurrency.hfcoin
                TransactionType.EARNED -> available += dataCurrency.hfcoin
                TransactionType.BOUGHT -> available += dataCurrency.hfcoin
                TransactionType.RECEIVED -> available += dataCurrency.hfcoin
                TransactionType.APP_GIFT -> available += dataCurrency.hfcoin
            }
        }
        return available
    }

    fun sendHFCoin(amount: Float, othersRef: DatabaseReference, creatorUid: String, myProfile: UserProfile, time: String) {
        var currency = Currency(senderUid = myProfile.uid, receiverUid = creatorUid, transactionType = TransactionType.SENT, hfcoin = amount)
        var md = when (amount) {
            Const.LIKE_VALUE -> MomentDetails(
                timeMomentPosted = time,
                likes = MomentLike(profileName = myProfile.name, profilePhoto = myProfile.image, profileUid = myProfile.uid)
            )
            Const.LOVE_VALUE -> MomentDetails(loves = MomentLove(profileName = myProfile.name, profilePhoto = myProfile.image, profileUid = myProfile.uid))
            else -> MomentDetails(loves = MomentLove(profileName = myProfile.name, profilePhoto = myProfile.image, profileUid = myProfile.uid))
        }
        val timeRef = FirebaseDatabase.getInstance().reference.child("time").child(myProfile.uid)
        timeRef.setValue(ServerValue.TIMESTAMP).addOnSuccessListener {
            timeRef.get().addOnSuccessListener { timeSnapshot ->
                if (timeSnapshot.exists()) {
                    val timeSent = timeSnapshot.value.toString()
                    md.time = timeSent
                    currency.timeOfTransaction = timeSent
                    othersRef.child(timeSent).setValue(md)
                    val receiverRef = FirebaseDatabase.getInstance().reference.child(TRANSFER_HISTORY).child(creatorUid).child(timeSent)
                    val senderRef = FirebaseDatabase.getInstance().reference.child(TRANSFER_HISTORY).child(myProfile.uid).child(timeSent)
                    senderRef.setValue(currency).addOnSuccessListener {
                        currency.transactionType = TransactionType.EARNED
                        receiverRef.setValue(currency)
                    }
                }
            }
        }
    }
}