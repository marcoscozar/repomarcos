package com.example.myapplication

import android.app.Application

class Link2U : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        private const val TAG = "MyApplication"
        private lateinit var instance: Link2U

        fun getInstance(): Link2U {
            return instance
        }
    }
} 