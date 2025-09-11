package com.r114358.rosette.traductor

import android.content.Context
import android.util.Log
import com.llamacpp.llama.LLamaAndroid
import com.r114358.rosette.Language
import com.r114358.rosette.ModelInfo
import com.r114358.rosette.utils.ensureModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext


object LLMModels {
    val Gemma3_1b_Q4 = ModelInfo(
        fileName = "gemma-3-1b-it-Q4_K_M.gguf",
        url = "https://huggingface.co/ggml-org/gemma-3-1b-it-GGUF/resolve/main/gemma-3-1b-it-Q4_K_M.gguf"
    )
    val Gemma3_4b_Q8 = ModelInfo(
        fileName = "gemma-3-4b-it-Q8_0.gguf",
        url = "https://huggingface.co/ggml-org/gemma-3-4b-it-GGUF/resolve/main/gemma-3-4b-it-Q8_0.gguf"
    )
    val Gemma3n_4b_Q8 = ModelInfo(
        fileName = "gemma-3n-E2B-it-Q8_0.gguf",
        url = "https://huggingface.co/ggml-org/gemma-3n-E2B-it-GGUF/resolve/main/gemma-3n-E2B-it-Q8_0.gguf"
    )
    val Gwen3_4b_Q8 = ModelInfo(
        fileName = "Qwen3-4B-Q8_0.gguf",
        url = "https://huggingface.co/ggml-org/Qwen3-4B-GGUF/resolve/main/Qwen3-4B-Q8_0.gguf"
    )
}

object Traductor {
    private var llama = LLamaAndroid.instance()
    private var ready = false
    private val mutex = Mutex()

    val tag = "rosette-traductor"

     suspend fun ensureLoaded(ctx: Context) {
        if (ready) return

        mutex.withLock {
            if (ready) return

            Log.d(tag, "Loading LLM...")
//            val weights = ensureModel(ctx.cacheDir, LLMModels.Gemma3n_4b_Q8)
            val weights = ensureModel(ctx.cacheDir, LLMModels.Gemma3_4b_Q8)
            Log.d(tag, "$weights")
            require(weights.exists()) { "Model file missing" }
            require(weights.length() > 0L) { "Model file is empty" }

            llama.load(weights.absolutePath)
            ready = true
            Log.d(tag, "LLM is ready")
        }
    }

    suspend fun translate(
        ctx: Context,
        text: String,
        from: Language,
        to: Language
    ): String = withContext(Dispatchers.IO) {
        ensureLoaded(ctx)

        val chat = """
<bos><start_of_turn>user
Translate everything I say from ${from.label} into ${to.label}.
Answer ONLY with the translation, no extra text.
$text<end_of_turn>
<start_of_turn>model
""".trimIndent()

        Log.d("llm", chat)

        val answer = buildString {
            llama.send(chat, true)
                .collect { append(it) }
        }.trim()

        Log.d("llm", "output: ${answer}")

        return@withContext answer
    }
}
