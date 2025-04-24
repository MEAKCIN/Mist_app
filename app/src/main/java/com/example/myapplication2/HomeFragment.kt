// File: app/src/main/java/com/example/myapplication2/HomeFragment.kt
package com.example.myapplication2

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.fragment.app.activityViewModels

class HomeFragment : Fragment() {

    private val emotionViewModel: EmotionViewModel by activityViewModels()

    private lateinit var emotionTextView: TextView
    private lateinit var sliderSprayPeriod: Slider
    private lateinit var valueSprayPeriod: TextView
    private lateinit var sliderSprayLength: Slider
    private lateinit var valueSprayLength: TextView
    private lateinit var bigSwitch: SwitchMaterial

    private lateinit var mainActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        emotionTextView = view.findViewById(R.id.homeEmotionTextView)
        sliderSprayPeriod = view.findViewById(R.id.sliderSprayPeriod)
        valueSprayPeriod = view.findViewById(R.id.valueSprayPeriod)
        sliderSprayLength = view.findViewById(R.id.sliderSprayLength)
        valueSprayLength = view.findViewById(R.id.valueSprayLength)
        bigSwitch = view.findViewById(R.id.bigSwitch)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivity = activity as MainActivity

        // Set the default switch state from MainActivity
        bigSwitch.isChecked = mainActivity.bigSwitchState

        sliderSprayPeriod.value = mainActivity.sprayPeriodValue.toFloat()
        sliderSprayLength.value = mainActivity.sprayLengthValue.toFloat()
        valueSprayPeriod.text = getString(R.string.slider_value_format, mainActivity.sprayPeriodValue)
        valueSprayLength.text = getString(R.string.slider_value_format, mainActivity.sprayLengthValue)

        // Update emotion text only when the switch is on.
        emotionViewModel.currentEmotion.observe(viewLifecycleOwner) { emotion ->
            if (bigSwitch.isChecked) {
                updateEmotionText(emotion)
            }
        }

        setupSliders()
        setupBigSwitch()
    }

    private fun setupSliders() {
        sliderSprayPeriod.addOnChangeListener { _, value, _ ->
            val intValue = value.toInt()
            valueSprayPeriod.text = getString(R.string.slider_value_format, intValue)
            mainActivity.sprayPeriodValue = intValue
        }
        sliderSprayLength.addOnChangeListener { _, value, _ ->
            val intValue = value.toInt()
            valueSprayLength.text = getString(R.string.slider_value_format, intValue)
            mainActivity.sprayLengthValue = intValue
        }
    }

    private fun setupBigSwitch() {
        bigSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Update the switch state in MainActivity
            mainActivity.bigSwitchState = isChecked
            if (isChecked) {
                emotionViewModel.currentEmotion.value?.let { updateEmotionText(it) }
            } else {
                emotionTextView.text = "Turned Off"
            }
        }
    }

    private fun updateEmotionText(emotion: Emotion) {
        val prefix = getString(R.string.emotion_label_prefix)
        val emotionString = when (emotion) {
            Emotion.HAPPY -> getString(R.string.emotion_happy)
            Emotion.SAD -> getString(R.string.emotion_sad)
            Emotion.ANGRY -> getString(R.string.emotion_angry)
            Emotion.NEUTRAL -> getString(R.string.emotion_neutral)
        }
        emotionTextView.text = "$prefix $emotionString"
    }
}