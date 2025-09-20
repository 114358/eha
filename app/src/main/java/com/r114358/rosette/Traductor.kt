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
    val Gemma3_4b_Q8 = ModelInfo(
        fileName = "gemma-3-4b-it-Q8_0.gguf",
        url = "https://huggingface.co/ggml-org/gemma-3-4b-it-GGUF/resolve/main/gemma-3-4b-it-Q8_0.gguf"
    )
    val Tower_Plus_2B_Q3 = ModelInfo(
        fileName = "tower-Plus-2B.Q8_0M.gguf",
        url = "https://huggingface.co/DZgas/Tower-Plus-2B-GGUF/resolve/main/Tower-Plus-2B.Q8_0.gguf",
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
//            val weights = ensureModel(ctx.cacheDir, LLMModels.Gemma3_4b_Q8)
            val weights = ensureModel(ctx.cacheDir, LLMModels.Tower_Plus_2B_Q3)
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
<start_of_turn>user
Translate the following ${from.label} source text to ${to.label}.
Reply with the translation only. No explanations. No extra text.
${from.label}: ${text} 
${to.label}: 
<end_of_turn>
<start_of_turn>model
""".trimIndent()

        Log.d("rosette-llm", chat)

        val answer = buildString {
            llama.send(chat, false)
                .collect { append(it) }
        }.substringBefore("<eos>")
            .substringBefore("<bos>")
            .substringBefore("<end_of_turn>")
            .substringBefore("<|eot_id|>")
            .trim()

        Log.d("llm", "output: ${answer}")

        return@withContext answer
    }
}
