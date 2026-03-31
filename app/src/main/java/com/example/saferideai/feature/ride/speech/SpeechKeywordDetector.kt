package com.example.saferideai.feature.ride.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechKeywordDetector(
    context: Context,
    private val onStatusChanged: (String) -> Unit,
    private val onHeardText: (String) -> Unit,
    private val onKeywordDetected: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val speechRecognizer: SpeechRecognizer? =
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }

    private var isListening = false
    private var stopped = false
    private var currentKeyword = ""

    init {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onStatusChanged("Dang lang nghe tu khoa khan cap...")
            }

            override fun onBeginningOfSpeech() {
                onStatusChanged("Dang nghe giong noi...")
            }

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                isListening = false
                if (!stopped) {
                    onStatusChanged("Dang tiep tuc lang nghe...")
                    scheduleRestart()
                }
            }

            override fun onError(error: Int) {
                isListening = false
                if (stopped) return

                onStatusChanged(
                    when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                            "Thieu quyen micro de tiep tuc nghe."
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            "Loi mang khi nhan dien giong noi."
                        SpeechRecognizer.ERROR_NO_MATCH ->
                            "Chua nghe thay tu khoa. Dang thu lai..."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                            "Khong co giong noi. Dang nghe lai..."
                        else -> "Nhan dien giong noi bi gian doan. Dang thu lai..."
                    }
                )

                if (error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    scheduleRestart()
                }
            }

            override fun onResults(results: Bundle) {
                isListening = false
                handleMatches(results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty())
                if (!stopped) {
                    scheduleRestart()
                }
            }

            override fun onPartialResults(partialResults: Bundle) {
                handleMatches(
                    partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                )
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
    }

    fun isRecognitionAvailable(): Boolean = speechRecognizer != null

    fun start(keyword: String) {
        currentKeyword = keyword.trim().lowercase()
        stopped = false
        startListeningInternal()
    }

    fun stop() {
        stopped = true
        mainHandler.removeCallbacksAndMessages(null)
        if (isListening) {
            isListening = false
            speechRecognizer?.stopListening()
        }
    }

    fun destroy() {
        stop()
        speechRecognizer?.destroy()
    }

    private fun startListeningInternal() {
        if (speechRecognizer == null || isListening || stopped) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        isListening = true
        speechRecognizer.startListening(intent)
    }

    private fun scheduleRestart() {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({ startListeningInternal() }, 800L)
    }

    private fun handleMatches(matches: List<String>) {
        if (matches.isEmpty()) return
        val heardText = matches.first()
        onHeardText(heardText)

        if (currentKeyword.isBlank()) return

        val detected = matches.any { candidate ->
            candidate.lowercase().contains(currentKeyword)
        }
        if (detected) {
            stopped = true
            onKeywordDetected()
        }
    }
}
