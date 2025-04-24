// File: app/src/main/java/com/example/myapplication2/ManualFragment.kt
package com.example.myapplication2

import android.os.Bundle
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.switchmaterial.SwitchMaterial

class ManualFragment : Fragment() {

    private val emotionViewModel: EmotionViewModel by activityViewModels()

    private lateinit var switchHappy: SwitchMaterial
    private lateinit var switchSad: SwitchMaterial
    private lateinit var switchAngry: SwitchMaterial
    private lateinit var switchNeutral: SwitchMaterial
    private lateinit var emotionSwitches: List<SwitchMaterial>
    private lateinit var manualEmotionTextView: TextView
    private lateinit var mainActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manual, container, false)
        switchHappy = view.findViewById(R.id.switchHappy)
        switchSad = view.findViewById(R.id.switchSad)
        switchAngry = view.findViewById(R.id.switchAngry)
        switchNeutral = view.findViewById(R.id.switchNeutral)
        emotionSwitches = listOf(switchHappy, switchSad, switchAngry, switchNeutral)
        manualEmotionTextView = view.findViewById(R.id.manualEmotionTextView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivity = activity as MainActivity
        setupSwitchListeners()
    }

    private fun setupSwitchListeners() {
        val listener = object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                if (buttonView == null) return
                // Prevent manual switch changes if device is turned off.
                if (!mainActivity.bigSwitchState) {
                    Toast.makeText(requireContext(), "Device is turned off", Toast.LENGTH_SHORT).show()
                    buttonView.setOnCheckedChangeListener(null)
                    buttonView.isChecked = !isChecked
                    buttonView.setOnCheckedChangeListener(this)
                    return
                }
                when (buttonView.id) {
                    R.id.switchHappy,
                    R.id.switchSad,
                    R.id.switchAngry,
                    R.id.switchNeutral -> {
                        if (isChecked) {
                            // When turning on, uncheck the others.
                            emotionSwitches.forEach { switch ->
                                if (switch.id != buttonView.id && switch.isChecked) {
                                    switch.setOnCheckedChangeListener(null)
                                    switch.isChecked = false
                                    switch.setOnCheckedChangeListener(this)
                                }
                            }
                            val selectedEmotion = when (buttonView.id) {
                                R.id.switchHappy -> Emotion.HAPPY
                                R.id.switchSad -> Emotion.SAD
                                R.id.switchAngry -> Emotion.ANGRY
                                R.id.switchNeutral -> Emotion.NEUTRAL
                                else -> return
                            }
                            emotionViewModel.updateEmotion(selectedEmotion)
                            updateEmotionText(selectedEmotion)
                        } else {
                            manualEmotionTextView.text = "Turned Off"
                        }
                    }
                }
            }
        }
        emotionSwitches.forEach { it.setOnCheckedChangeListener(listener) }
    }

    private fun updateEmotionText(emotion: Emotion) {
        val prefix = getString(R.string.emotion_label_prefix)
        val emotionString = when (emotion) {
            Emotion.HAPPY -> getString(R.string.emotion_happy)
            Emotion.SAD -> getString(R.string.emotion_sad)
            Emotion.ANGRY -> getString(R.string.emotion_angry)
            Emotion.NEUTRAL -> getString(R.string.emotion_neutral)
        }
        manualEmotionTextView.text = "$prefix $emotionString"
    }
}