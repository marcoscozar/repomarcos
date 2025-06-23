package com.example.myapplication.model

import com.google.firebase.Timestamp

data class Chat(
    val id: String = "",
    val otherUserId: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Timestamp? = null,
    val unreadCount: Int = 0,
    val lastMessageSenderId: String = ""
) 