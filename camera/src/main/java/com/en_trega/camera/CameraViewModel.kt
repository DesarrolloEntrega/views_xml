package com.en_trega.camera

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CameraViewModel : ViewModel() {
    private val currentPhotoPath: MutableLiveData<String?> = MutableLiveData()

    fun setPhotoPath(path: String) {
        currentPhotoPath.value = path
    }

    fun getPhotoPath(): String? {
        return currentPhotoPath.value
    }
}
