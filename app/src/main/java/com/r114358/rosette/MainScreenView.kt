package com.r114358.rosette

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.r114358.rosette.traductor.Traductor
import com.r114358.rosette.utils.ensureModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.IOException
import java.util.Locale


data class ModelInfo(val fileName: String, val url: String)

data class Language(
    val label: String, val locale: Locale, val model: ModelInfo
)

object Languages {
    val English = Language(
        label = "English", locale = Locale.ENGLISH, model = ModelInfo(
            fileName = "vosk-model-small-en-us-0.15",
            url = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip",
        )
    )
    val French = Language(
        label = "French", locale = Locale.FRANCE, model = ModelInfo(
            fileName = "vosk-model-small-fr-0.22",
            url = "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip",
        )
    )
    val German = Language(
        label = "German", locale = Locale.GERMAN, model = ModelInfo(
            fileName = "vosk-model-small-de-0.15",
            url = "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip"
        )
    )
    val Italian = Language(
        label = "Italian", locale = Locale.ITALIAN, model = ModelInfo(
            fileName = "vosk-model-small-it-0.22",
            url = "https://alphacephei.com/vosk/models/vosk-model-it-0.22.zip",
        )
    )
    val Turkish = Language(
        label = "Turkish", locale = Locale.forLanguageTag("tr"), model = ModelInfo(
            fileName = "vosk-model-small-tr-0.3",
            url = "https://alphacephei.com/vosk/models/vosk-model-small-tr-0.3.zip",
        )
    )
    val Arabic = Language(
        label = "Arabic", locale = Locale.forLanguageTag("ar"), model = ModelInfo(
            fileName = "vosk-model-ar-mgb2-0.4",
            url = "https://alphacephei.com/vosk/models/vosk-model-ar-mgb2-0.4.zip",
        )
    )

    val all = listOf(English, French, German, Italian, Turkish, Arabic)
}


class MainScreenViewModel(
    private val application: Application,
    private val asrLanguage: Language,
    private val ttsLanguage: Language,
    private val viewTag: String,
) : ViewModel(), RecognitionListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var asrService: SpeechService? = null
    private lateinit var asrModel: Model
    var isListening by mutableStateOf(false)
        private set
    private val _asrLang = MutableStateFlow(asrLanguage)
    var asrLang: StateFlow<Language> = _asrLang.asStateFlow()
    val partial_transcript = mutableStateOf("")
    val transcript = mutableStateOf("")

    private var ttsService: TextToSpeech? = null
    var isPlaying by mutableStateOf(false)
        private set
    private val _ttsLang = MutableStateFlow(ttsLanguage)
    var ttsLang: StateFlow<Language> = _ttsLang.asStateFlow()
    var translated = mutableStateOf("")

    val tag = "rosette-$viewTag"

    init {
        setASR(_asrLang.value)
        setTTS(_ttsLang.value)
    }

    fun setASR(language: Language) {
        _asrLang.value = language

        Thread {
            try {
                asrService?.stop()
                val modelDir = ensureModel(application.cacheDir, language.model)
                modelDir.listFiles()?.forEach {
                    Log.i(tag, "Model contents: ${it.name}")
                }
                Log.i(tag, "$modelDir")
                asrModel = Model(modelDir.absolutePath)
                val recognizer = Recognizer(asrModel, 16_000f)
                asrService = SpeechService(recognizer, 16_000f)
                Log.i(tag, "set ASR to ${language.label} (model=${language.model.fileName})")
            } catch (e: IOException) {
                Log.e(tag, "Fail to launch ASR : " + e.toString())
                transcript.value = "Model switch failed"
            }
        }.start()
    }

    fun setTTS(language: Language) {
        _ttsLang.value = language

        ttsService = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = ttsService?.setLanguage(language.locale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(tag, "TTS language is not supported (${language.locale})")
                } else {
                    Log.d(tag, "set TTS to ${language.label}")
                }
            } else {
                Log.e(tag, "TTS init failed $status")
            }
        }
        Log.d(tag, "target language: ${language.label}")
    }

    fun toggleListening() {
        if (!isListening) {
            Thread {
                asrService?.startListening(this)
            }.start()
            isListening = true
            Log.d(tag, "Start listening...")
        } else {
            asrService?.stop()
            isListening = false
            Log.d(tag, "Stop listening")
        }
    }

    fun togglePlaying() {
        if (!isPlaying) {
            ttsService!!.speak(translated.value, TextToSpeech.QUEUE_FLUSH, null, "unique_id")
            isPlaying = true
        } else {
            ttsService!!.stop()
            isPlaying = false
        }
    }

    override fun onPartialResult(hypothesis: String?) {
        hypothesis?.let {
            val text = JSONObject(it).optString("partial")
            partial_transcript.value = text
        }
    }

    override fun onResult(hypothesis: String?) {
        hypothesis?.let {
            val text = JSONObject(it).optString("text")
            Log.i(tag, "DONE '${text}")
            if (text.isNotBlank()) transcript.value += ". " + partial_transcript.value
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        scope.launch {
            Log.i(tag, "launch llm")

            var text = transcript.value
            if (text.isBlank()) {
                text = partial_transcript.value
            }

            translated.value = Traductor.translate(
                application,
                text,
                _asrLang.value,
                _ttsLang.value
            )

            transcript.value = ""

            Log.i(
                tag,
                "transcription:\n\n${transcript.value}\n\ntraduction:\n\n${translated.value}"
            )

        }

    }

    override fun onError(e: Exception?) {
        transcript.value = "Error: ${e?.message}"
    }

    override fun onTimeout() {
        transcript.value = "Timeout"
    }

    override fun onCleared() {
        Log.d(tag, "CLEAR")
        super.onCleared()
        scope.cancel()
    }

    companion object {
        fun factory(selectedLanguage: Language, targetLanguage: Language, tag: String) =
            viewModelFactory {
                initializer {
                    val application =
                        this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                    MainScreenViewModel(application, selectedLanguage, targetLanguage, tag)
                }
            }
    }
}


