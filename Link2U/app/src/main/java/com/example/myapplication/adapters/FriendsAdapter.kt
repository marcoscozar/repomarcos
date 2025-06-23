package com.example.myapplication.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.ProfileOtroActivity
import com.example.myapplication.model.Usuario
import com.bumptech.glide.Glide
import com.example.myapplication.ProfileAmigoActivity
import de.hdodenhof.circleimageview.CircleImageView

class FriendsAdapter : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {
    private var friends: List<Usuario> = emptyList()

    fun updateFriends(newFriends: List<Usuario>) {
        friends = newFriends
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(friends[position])
    }

    override fun getItemCount(): Int = friends.size

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: CircleImageView = itemView.findViewById(R.id.friendProfileImage)
        private val userName: TextView = itemView.findViewById(R.id.friendName)

        fun bind(friend: Usuario) {
            userName.text = friend.nombre_usuario

            Glide.with(itemView.context)
                .load(friend.imagen_perfil)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(profileImage)

            itemView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, ProfileAmigoActivity::class.java).apply {
                    putExtra("username", friend.nombre_usuario)
                }
                context.startActivity(intent)
            }
        }
    }
} 