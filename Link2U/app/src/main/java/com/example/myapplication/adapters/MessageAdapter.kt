package com.example.myapplication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.Message
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private var messages: List<Message> = emptyList()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.email ?: ""

    fun updateMessages(newMessages: List<Message>) {
        messages = newMessages.sortedBy { it.timestamp.seconds }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textMessage: TextView = itemView.findViewById(R.id.textMessage)
        private val textTime: TextView = itemView.findViewById(R.id.textTime)

        fun bind(message: Message) {
            textMessage.text = message.content
            
            // Formatear la hora del mensaje
            val date = Date(message.timestamp.seconds * 1000)
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            textTime.text = sdf.format(date)

            // Alinear el mensaje según el remitente
            val params = textMessage.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            if (message.senderId == currentUserId) {
                // Mensaje propio (derecha)
                params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                params.horizontalBias = 1f
                textMessage.setBackgroundResource(R.drawable.message_background_sent)
            } else {
                // Mensaje del otro (izquierda)
                params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                params.horizontalBias = 0f
                textMessage.setBackgroundResource(R.drawable.message_background)
            }
            textMessage.layoutParams = params
        }
    }
} 