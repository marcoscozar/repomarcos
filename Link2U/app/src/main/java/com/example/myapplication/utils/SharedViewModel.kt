package com.example.myapplication.utils

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

// SharedViewModel.kt
class SharedViewModel : ViewModel() {
    val email = MutableLiveData<String>()
}
