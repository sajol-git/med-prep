package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGen.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 2000)
            // Need to release it after playing
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                toneGen.release()
            }, 2500)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
