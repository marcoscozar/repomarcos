package com.example.myapplication.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.example.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore

class FriendRequestDialog(
    context: Context,
    private val requestId: String,
    private val fromName: String,
    private val fromEmail: String,
    private val toEmail: String
) : Dialog(context) {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_friend_request)

        val messageTextView = findViewById<TextView>(R.id.textViewRequestMessage)
        val acceptButton = findViewById<Button>(R.id.buttonAccept)
        val ignoreButton = findViewById<Button>(R.id.buttonIgnore)

        messageTextView.text = "$fromName quiere ser tu amigo"

        acceptButton.setOnClickListener {
            acceptFriendRequest()
            dismiss()
        }

        ignoreButton.setOnClickListener {
            ignoreFriendRequest()
            dismiss()
        }
    }

    private fun acceptFriendRequest() {
        // Update request status
        db.collection("friend_requests")
            .document(requestId)
            .update("status", "accepted")
            .addOnSuccessListener {
                // Add to friends collection for both users
                val friendship = hashMapOf(
                    "user1_email" to fromEmail,
                    "user2_email" to toEmail,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )

                db.collection("friendships")
                    .add(friendship)
            }
    }

    private fun ignoreFriendRequest() {
        db.collection("friend_requests")
            .document(requestId)
            .update("status", "ignored")
    }
} 