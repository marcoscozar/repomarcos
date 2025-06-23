package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.adapters.FriendsAdapter
import com.example.myapplication.databinding.FragmentLikesBinding
import com.example.myapplication.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class LikesFragment : Fragment() {
    private var _binding: FragmentLikesBinding? = null
    private val binding get() = _binding!!
    private lateinit var friendsAdapter: FriendsAdapter
    private val db = FirebaseFirestore.getInstance()
    private var listener1: ListenerRegistration? = null
    private var listener2: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLikesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        friendsAdapter = FriendsAdapter()
        binding.recyclerViewFriends.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = friendsAdapter
        }

        // Load friends
        loadFriends()
    }

    private fun loadFriends() {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: return

        // Crear una consulta que combine ambas condiciones
        val query1 = db.collection("friendships")
            .whereEqualTo("user1_email", currentUserEmail)
        val query2 = db.collection("friendships")
            .whereEqualTo("user2_email", currentUserEmail)

        // Escuchar cambios en ambas consultas
        listener1 = query1.addSnapshotListener { snapshot1, e1 ->
            if (e1 != null) return@addSnapshotListener
            updateFriendsList(currentUserEmail)
        }

        listener2 = query2.addSnapshotListener { snapshot2, e2 ->
            if (e2 != null) return@addSnapshotListener
            updateFriendsList(currentUserEmail)
        }

        // Realizar la primera actualización
        updateFriendsList(currentUserEmail)
    }

    private fun updateFriendsList(currentUserEmail: String) {
        val friendEmails = mutableSetOf<String>()

        // Obtener amigos donde el usuario actual es user1
        db.collection("friendships")
            .whereEqualTo("user1_email", currentUserEmail)
            .get()
            .addOnSuccessListener { snapshot1 ->
                snapshot1.documents.forEach { doc ->
                    doc.getString("user2_email")?.let { friendEmails.add(it) }
                }

                // Obtener amigos donde el usuario actual es user2
                db.collection("friendships")
                    .whereEqualTo("user2_email", currentUserEmail)
                    .get()
                    .addOnSuccessListener { snapshot2 ->
                        snapshot2.documents.forEach { doc ->
                            doc.getString("user1_email")?.let { friendEmails.add(it) }
                        }

                        // Obtener detalles de los amigos
                        if (friendEmails.isNotEmpty()) {
                            db.collection("usuarios")
                                .whereIn("e_mail", friendEmails.toList())
                                .get()
                                .addOnSuccessListener { result ->
                                    val friends = result.toObjects(Usuario::class.java)
                                    friendsAdapter.updateFriends(friends)
                                }
                        } else {
                            friendsAdapter.updateFriends(emptyList())
                        }
                    }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar los listeners cuando el fragmento se destruya
        listener1?.remove()
        listener2?.remove()
        listener1 = null
        listener2 = null
        _binding = null
    }
} 