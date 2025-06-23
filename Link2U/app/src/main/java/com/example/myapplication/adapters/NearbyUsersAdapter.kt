package com.example.myapplication.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.User
import com.example.myapplication.ProfileOtroActivity
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class NearbyUsersAdapter : RecyclerView.Adapter<NearbyUsersAdapter.UserViewHolder>() {

    private var users: List<User> = emptyList()

    fun updateUsers(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userName: TextView = itemView.findViewById(R.id.userName)
        private val userAvatar: CircleImageView = itemView.findViewById(R.id.userAvatar)

        fun bind(user: User) {
            userName.text = user.username

            // Load profile image using Glide
            Glide.with(itemView.context)
                .load(user.profileImage)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(userAvatar)

            itemView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, ProfileOtroActivity::class.java).apply {
                    putExtra("username", user.username)
                }
                context.startActivity(intent)
            }
        }
    }
}
