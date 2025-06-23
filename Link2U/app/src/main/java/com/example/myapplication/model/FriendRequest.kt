package com.example.myapplication.model

data class FriendRequest(
    val id: String = "",
    val fromEmail: String = "",
    val fromName: String = "",
    val toEmail: String = "",
    val status: String = "",
    val timestamp: Long = 0
) 