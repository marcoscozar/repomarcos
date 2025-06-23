package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.adapters.NearbyUsersAdapter
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.model.Usuario
import com.example.myapplication.services.FriendRequestService
import com.example.myapplication.ui.ConnectionState
import com.example.myapplication.ui.NearbyViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: NearbyViewModel
    private lateinit var usersAdapter: NearbyUsersAdapter
    private lateinit var friendRequestService: FriendRequestService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        parentFragmentManager.setFragmentResultListener("email_key", this) { _, bundle ->
            val email = bundle.getString("email") ?: return@setFragmentResultListener

            Toast.makeText(requireContext(), "Email recibido: $email", Toast.LENGTH_SHORT).show()

            val db = FirebaseFirestore.getInstance()
            db.collection("usuarios").whereEqualTo("e_mail", email).get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val usuario = documents.first().toObject(Usuario::class.java)
                        val username = usuario.nombre_usuario

                        Toast.makeText(requireContext(), "Bienvenido $username", Toast.LENGTH_SHORT).show()
                        viewModel.setUsername(username)

                        // Iniciar Nearby directamente, ya que los permisos se solicitan en MainActivity
                            startNearby()
                    } else {
                        Toast.makeText(requireContext(), "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error consultando usuario", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let {
            WindowCompat.setDecorFitsSystemWindows(it.window, false)
        }

        viewModel = ViewModelProvider(requireActivity())[NearbyViewModel::class.java]

        setupRecyclerView()
        setupButtons()
        observeConnectionState()
        observeNearbyUsers()

        friendRequestService = FriendRequestService(requireContext())

        FirebaseAuth.getInstance().currentUser?.email?.let { email ->
            friendRequestService.startListeningForRequests(email)
        }
    }

    private fun observeNearbyUsers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.nearbyUsers.collectLatest { users ->
                usersAdapter.updateUsers(users)
                binding.emptyView.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (users.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun observeConnectionState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.connectionState.collectLatest { state ->
                binding.statusText.text = when (state) {
                    ConnectionState.DISCONNECTED -> "Desconectado"
                    ConnectionState.ADVERTISING -> "Publicitando..."
                    ConnectionState.DISCOVERING -> "Buscando usuarios..."
                    ConnectionState.CONNECTED -> "Conectado"
                    ConnectionState.ERROR -> "Buscando usuarios..."
                }
            }
        }
    }

    private fun setupButtons() {
        binding.buttonFriendRequests.setOnClickListener {
            val intent = Intent(activity, FriendRequestsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        usersAdapter = NearbyUsersAdapter()
        binding.recyclerView.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = usersAdapter
        }
    }

    private fun startNearby() {
        viewModel.startAdvertising(requireContext())
        viewModel.startDiscovery(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        friendRequestService.stopListening()
        _binding = null
    }
}
