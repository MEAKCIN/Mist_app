package com.example.myapplication2 // Replace with your actual package name

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

// Enum defined outside or imported if in another file
// enum class Emotion { HAPPY, SAD, ANGRY, NEUTRAL }

class EmotionViewModel : ViewModel() {

    // Private MutableLiveData that can be modified within the ViewModel
    private val _currentEmotion = MutableLiveData<Emotion>(Emotion.NEUTRAL) // Default value

    // Public LiveData that Fragments can observe (read-only)
    val currentEmotion: LiveData<Emotion> = _currentEmotion

    /**
     * Updates the current emotion state.
     * Called by ManualFragment when a switch is selected.
     */
    fun updateEmotion(newEmotion: Emotion) {
        // Only update if the value has actually changed
        if (_currentEmotion.value != newEmotion) {
            _currentEmotion.value = newEmotion
        }
    }
}
