package com.example.myapplication.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapters.NearbyUsersAdapter
import com.example.myapplication.ui.ConnectionState
import com.example.myapplication.ui.NearbyViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var viewModel: NearbyViewModel
    private lateinit var adapter: NearbyUsersAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var statusText: TextView
    private lateinit var emptyView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar ViewModel compartido con la Activity
        viewModel = ViewModelProvider(requireActivity())[NearbyViewModel::class.java]

        // Configurar RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView)
        statusText = view.findViewById(R.id.statusText)
        emptyView = view.findViewById(R.id.emptyView)

        adapter = NearbyUsersAdapter()
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Observar cambios en la lista de usuarios usando coroutines
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.nearbyUsers.collectLatest { users ->
                adapter.updateUsers(users)
                updateUI(users.isEmpty())
            }
        }

        // Observar estado de conexión usando coroutines
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.connectionState.collectLatest { state ->
                statusText.text = when (state) {
                    ConnectionState.DISCONNECTED -> "Desconectado"
                    ConnectionState.ADVERTISING -> "Publicitando..."
                    ConnectionState.DISCOVERING -> "Buscando usuarios..."
                    ConnectionState.CONNECTED -> "Conectado"
                    ConnectionState.ERROR -> "Buscando usuarios..."
                }
            }
        }
    }

    private fun updateUI(isEmpty: Boolean) {
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }
} 