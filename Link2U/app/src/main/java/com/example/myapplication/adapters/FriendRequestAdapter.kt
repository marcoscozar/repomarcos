package com.example.myapplication.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.ProfileOtroActivity
import com.example.myapplication.R
import com.example.myapplication.model.FriendRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class FriendRequestAdapter : RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder>() {

    private var requests: List<FriendRequest> = emptyList()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.email ?: ""

    fun updateRequests(newRequests: List<FriendRequest>) {
        requests = newRequests
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(requests[position])
    }

    override fun getItemCount(): Int = requests.size

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textUsername: TextView = itemView.findViewById(R.id.textUsername)
        private val textEmail: TextView = itemView.findViewById(R.id.textEmail)
        private val buttonAccept: Button = itemView.findViewById(R.id.buttonAccept)
        private val buttonReject: Button = itemView.findViewById(R.id.buttonReject)

        fun bind(request: FriendRequest) {
            textUsername.text = request.fromName
            textEmail.text = request.fromEmail

            // Hacer el nombre de usuario clickeable
            textUsername.setOnClickListener {
                val intent = Intent(itemView.context, ProfileOtroActivity::class.java).apply {
                    putExtra("username", request.fromName)
                    putExtra("email", request.fromEmail)
                }
                itemView.context.startActivity(intent)
            }

            buttonAccept.setOnClickListener {
                acceptRequest(request)
            }

            buttonReject.setOnClickListener {
                rejectRequest(request)
            }
        }

        private fun acceptRequest(request: FriendRequest) {
            // Primero actualizar el estado a "accepted"
            db.collection("friend_requests")
                .document(request.id)
                .update("status", "accepted")
                .addOnSuccessListener {
                    // Crear la amistad en la colección friendships
                    val friendship = hashMapOf(
                        "user1_email" to currentUserId,
                        "user2_email" to request.fromEmail,
                        "timestamp" to Date()
                    )

                    db.collection("friendships")
                        .add(friendship)
                        .addOnSuccessListener {
                            // Eliminar la solicitud
                            db.collection("friend_requests")
                                .document(request.id)
                                .delete()
                                .addOnSuccessListener {
                                    // Actualizar la lista de solicitudes
                                    val updatedRequests = requests.filter { it.id != request.id }
                                    updateRequests(updatedRequests)
                                }
                        }
                }
        }

        private fun rejectRequest(request: FriendRequest) {
            // Primero actualizar el estado a "rejected"
            db.collection("friend_requests")
                .document(request.id)
                .update("status", "rejected")
                .addOnSuccessListener {
                    // Luego eliminar la solicitud
                    db.collection("friend_requests")
                        .document(request.id)
                        .delete()
                        .addOnSuccessListener {
                            // Actualizar la lista de solicitudes
                            val updatedRequests = requests.filter { it.id != request.id }
                            updateRequests(updatedRequests)
                        }
                }
        }
    }
} 