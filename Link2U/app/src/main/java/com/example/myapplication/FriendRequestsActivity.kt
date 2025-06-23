package com.example.myapplication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapters.FriendRequestAdapter
import com.example.myapplication.model.FriendRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendRequestsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FriendRequestAdapter
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.email ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_requests)
        enableEdgeToEdge()

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.recyclerViewRequests)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = FriendRequestAdapter()
        recyclerView.adapter = adapter

        // Cargar solicitudes
        loadFriendRequests()
    }

    private fun loadFriendRequests() {
        db.collection("friend_requests")
            .whereEqualTo("to_email", currentUserId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    FriendRequest(
                        id = doc.id,
                        fromEmail = doc.getString("from_email") ?: return@mapNotNull null,
                        fromName = doc.getString("from_name") ?: return@mapNotNull null,
                        toEmail = doc.getString("to_email") ?: return@mapNotNull null,
                        status = doc.getString("status") ?: return@mapNotNull null,
                        timestamp = doc.getTimestamp("timestamp")?.seconds ?: 0
                    )
                } ?: emptyList()

                adapter.updateRequests(requests)
            }
    }
} 