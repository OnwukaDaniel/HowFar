package com.azur.howfar.chat

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.azur.howfar.R
import com.azur.howfar.databinding.ActivityChatBoxBinding
import io.socket.client.Socket
import org.json.JSONObject


class ChatBoxActivity : AppCompatActivity(), View.OnClickListener {
    private val binding by lazy { ActivityChatBoxBinding.inflate(layoutInflater) }
    private val testChatAdapter = TestChatAdapter()
    private lateinit var socket: Socket
    private val dataset = arrayListOf<TestChat>()
    private var username = ""
    private val application = SocketApplication()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        testChatAdapter.dataset = dataset
        binding.inputSend.setOnClickListener(this)
        binding.chatRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.chatRv.adapter = testChatAdapter

        username = intent.getStringExtra("name")!!
        socket = application.socket
        try {
            socket.connect()
            socket.emit("join", username)
            socket.on("userjoinedthechat") { data ->
                runOnUiThread { Toast.makeText(this, data.toString(), Toast.LENGTH_SHORT).show(); }
            }
            socket.on("message") { args ->
                val data = args[0] as JSONObject;
                try {
                    println("Data ************************* $data --- $args")
                    val nickname = data.getString("senderNickname");
                    val message = data.getString("message");
                    val chat = TestChat(username = nickname, message = message)
                    dataset.add(chat)
                    runOnUiThread { testChatAdapter.notifyItemInserted(dataset.size) }
                } catch (e: Exception) {
                    println("Error occurred ************************* ${e.printStackTrace()}")
                }
            }
            socket.on("userdisconnect") { data ->
                runOnUiThread { Toast.makeText(this, data.toString(), Toast.LENGTH_LONG).show() }
            }
        } catch (e: Exception) {
            println("Activity socket exception ***************************************** ${e.printStackTrace()}")
        }
    }

    override fun onDestroy() {
        socket.disconnect()
        super.onDestroy()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.input_send -> {
                val input = binding.inputEt.text.trim().toString()
                if (input == "") return
                socket.emit("messagedetection", username, input)
                binding.inputEt.text.clear()
            }
        }
    }
}

class TestChatAdapter : RecyclerView.Adapter<TestChatAdapter.ViewHolder>() {
    private lateinit var context: Context
    var dataset = arrayListOf<TestChat>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nickname: TextView = itemView.findViewById(R.id.nickname)
        val message: TextView = itemView.findViewById(R.id.chat_message)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.test_chat_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val datum = dataset[position]
        holder.message.text = datum.message
        holder.nickname.text = datum.username
    }

    override fun getItemCount() = dataset.size
}

data class TestChat(
    var username: String = "",
    var message: String = ""
)