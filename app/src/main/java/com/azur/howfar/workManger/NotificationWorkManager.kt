package com.azur.howfar.workManger

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.azur.howfar.R
import com.azur.howfar.activity.BaseActivity
import com.azur.howfar.broadcasts.DirectReceiver
import com.azur.howfar.broadcasts.MessageMarkAsRead
import com.azur.howfar.dilog.IncomingCallDialog
import com.azur.howfar.howfarchat.chat.ChatActivity2
import com.azur.howfar.models.*
import com.azur.howfar.utils.TimeUtils
import com.azur.howfar.utils.Util
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class NotificationWorkManager(val context: Context, val params: WorkerParameters) : Worker(context, params) {
    private var auth = FirebaseAuth.getInstance().currentUser
    private var callRef = FirebaseDatabase.getInstance().reference
    private var messagesRef = FirebaseDatabase.getInstance().reference
    private val scope = CoroutineScope(Dispatchers.Main)
    val dataset: ArrayList<ChatData> = arrayListOf()
    private lateinit var pref: SharedPreferences
    private val contactList: ArrayList<Pair<String, String>> = arrayListOf()
    private val phoneList: ArrayList<String> = arrayListOf()
    var timesObserved = 0L

    override fun doWork(): Result {
        pref = context.getSharedPreferences(context.getString(R.string.ALL_PREFERENCE), Context.MODE_PRIVATE)
        pref = context.getSharedPreferences(context.getString(R.string.ALL_PREFERENCE), Context.MODE_PRIVATE)

        if (FirebaseAuth.getInstance().currentUser == null) Result.failure()
        val myAuth = FirebaseAuth.getInstance().currentUser!!.uid
        callRef = FirebaseDatabase.getInstance().reference.child(ChatActivity2.CALL_REFERENCE).child(myAuth)
        messagesRef = FirebaseDatabase.getInstance().reference.child(BaseActivity.CHAT_REFERENCE).child(myAuth)

        messagesRef.keepSynced(false)
        callRef.keepSynced(false)
        callRef.addValueEventListener(callListener)
        messagesRef.addValueEventListener(messageListener)
        return Result.success()
    }

    private val callListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists()) {
                var callData = snapshot.getValue(CallData::class.java)!!
                if (FirebaseAuth.getInstance().currentUser == null) return
                if (callData.timeCalled == "") return
                val myAuth = FirebaseAuth.getInstance().currentUser!!.uid
                val callTime = TimeUtils.UTCToLocal(callData.timeCalled).toLong() / 1000 // CALL EXPIRED IMPLEMENTATION
                val timeNow = Calendar.getInstance().timeInMillis / 1000 // CALL EXPIRED IMPLEMENTATION
                val ansType = callData.answerType
                when {
                    ansType == CallAnswerType.CANCELLED -> return
                    callData.callerUid == myAuth -> return
                    callData.timeCalled == "" -> return
                    timesObserved == callTime -> return
                    timeNow > callTime + 30 -> return
                }
                timesObserved = callTime
                Toast.makeText(context, "Call", Toast.LENGTH_LONG).show()
                callData.answerType = CallAnswerType.RECEIVED
                callRef.setValue(callData).addOnSuccessListener {
                    val intent = Intent(context, IncomingCallDialog::class.java)
                    callData.engagementType = CallEngagementType.JOIN
                    intent.putExtra("callData", Gson().toJson(callData))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    scope.launch {
                        delay(60_000)
                        return@launch
                    }
                }
            }
        }

        override fun onCancelled(error: DatabaseError) = Unit
    }

    private val messageListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (FirebaseAuth.getInstance().currentUser == null) return
            val myAuth = FirebaseAuth.getInstance().currentUser!!.uid
            if (snapshot.exists()) {
                dataset.clear()
                for (i in snapshot.children) {
                    val lastMsg = i.children.last().getValue(ChatData::class.java)!!
                    if (!lastMsg.read) {
                        dataset.add(lastMsg)
                    }
                }
                dataset.sortWith(compareByDescending { it.uniqueQuerableTime })

                for ((index, lastMsg) in dataset.withIndex()) {
                    if (lastMsg.senderuid == myAuth || lastMsg.read) return
                    val remoteInput: RemoteInput = RemoteInput.Builder(lastMsg.uniqueQuerableTime).run {
                        setLabel("Your reply")
                        build()
                    }
                    val activityIntent = Intent(context, ChatActivity2::class.java).apply {
                        putExtra("data", otherParticipant(lastMsg.participants))
                    }
                    val activityPendingIntent: PendingIntent =
                        PendingIntent.getActivity(context, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                    var replyPendingIntent: PendingIntent

                    val replyIntent = Intent(context, DirectReceiver::class.java).apply {
                        val json = Gson().toJson(lastMsg)
                        putExtra("index", index)
                        putExtra("json", json)
                    }
                    replyPendingIntent = PendingIntent.getBroadcast(applicationContext, index, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT)

                    val action: NotificationCompat.Action = NotificationCompat.Action
                        .Builder(R.drawable.app_logo_round, "Reply", replyPendingIntent)
                        .addRemoteInput(remoteInput)
                        .build()

                    val markAsReadIntent = Intent(context, MessageMarkAsRead::class.java).apply {
                        val json = Gson().toJson(lastMsg)
                        putExtra("json", json)
                    }
                    val markAsReadPendingIntent = PendingIntent.getBroadcast(context, index, markAsReadIntent, PendingIntent.FLAG_ONE_SHOT)

                    val actionMarkAsRead = NotificationCompat.Action
                        .Builder(R.drawable.app_icon_sec, "Mark as red", markAsReadPendingIntent)
                        .build()

                    sendNotification(lastMsg, action, activityPendingIntent)
                }
            }
        }

        override fun onCancelled(error: DatabaseError) = Unit
    }

    private fun otherParticipant(participants: ArrayList<String>): String {
        val myAuth = FirebaseAuth.getInstance().currentUser!!.uid
        for (i in participants) return if (i != myAuth) i else participants[1]
        return ""
    }

    private fun sendNotification(lastMsg: ChatData, action: NotificationCompat.Action, pendingIntent: PendingIntent) {
        val formattedPhone = Util.formatNumber(lastMsg.myPhone)
        var nameOrPhone = formattedPhone
        if (formattedPhone in phoneList) for (x in contactList) if (x.second == formattedPhone) {
            nameOrPhone = x.first
            break
        }
        val tempParticipantTempData = otherParticipant(lastMsg)
        if (tempParticipantTempData.uid != "") nameOrPhone = tempParticipantTempData.tempName

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, "Messages")
            .setSmallIcon(R.drawable.app_icon_sec)
            .setContentTitle(nameOrPhone)
            .addAction(action)
            .setColor(Color.BLUE)
            .setContentText(lastMsg.msg)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(lastMsg.msg))
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(context)) {
            val no = if (lastMsg.myPhone.length >= 5) Util.formatNumber(lastMsg.myPhone).substring(0, 5).toInt() else 0
            val inPhoneValue = pref.getInt(context.getString(R.string.in_chat_phone_key), 0)
            if (inPhoneValue == 0) {
                notify(no, builder.build())
            }
        }
    }

    private fun otherParticipant(chatUser: ChatData): ParticipantTempData {
        val myAuth = FirebaseAuth.getInstance().currentUser!!.uid
        for (i in chatUser.participantsTempData) if (i.uid != myAuth) return i
        return ParticipantTempData()
    }

    override fun onStopped() {
        super.onStopped()
    }

    companion object {
        const val CALL_REFERENCE = "call_reference"
        const val USER_DETAILS = "user_details"
        const val CONTACTS = "CONTACTS"
    }
}