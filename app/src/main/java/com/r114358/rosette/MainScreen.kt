package com.r114358.rosette

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.r114358.rosette.traductor.Traductor


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Item(viewModel: MainScreenViewModel, modifier: Modifier = Modifier, title: String) {
    val transcript by viewModel.partial_transcript
    val translated by viewModel.translated
    val selectedLanguage = viewModel.asrLang
    var expanded: Boolean by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
//            .border(2.dp, Color.Gray, RectangleShape)
            .padding(16.dp)
            .fillMaxSize()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                readOnly = true,
                value = selectedLanguage.value.label,
                onValueChange = {},
                label = { Text("Language") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier.menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Languages.all.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language.label) },
                        onClick = {
                            viewModel.setASR(language)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { viewModel.toggleListening() },
            modifier = Modifier
                .height(120.dp)
                .width(120.dp),
            shape = CircleShape,
        ) { Text(fontSize = 18.sp, text = if (viewModel.isListening) "Stop" else "Rec") }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { viewModel.togglePlaying() },
            modifier = Modifier
                .height(120.dp)
                .width(120.dp),
            shape = CircleShape,
        ) { Text(fontSize = 18.sp, text = if (viewModel.isPlaying) "Stop" else "Play") }

        Spacer(Modifier.height(20.dp))

        Text(
            text = transcript,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = translated,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MainScreen(doctorVM: MainScreenViewModel, patientVM: MainScreenViewModel) {
    val context = LocalContext.current

    var llmReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                Traductor.ensureLoaded(context)
            }
            llmReady = true
            Log.d("rosette-main", "LLM loaded")
        } catch (t: Throwable) {
            Log.e("rosette-main", "Failed to load LLM", t)
        }
    }

    val doctorLang  by doctorVM.asrLang.collectAsState()
    LaunchedEffect(doctorLang)  { patientVM.setTTS(doctorLang) }
    val patientLang by patientVM.asrLang.collectAsState()
    LaunchedEffect(patientLang) { doctorVM.setTTS(patientLang) }

    Row(Modifier.fillMaxSize()) {
        if (!llmReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
//                contentAlignment = Alignment.TopCenter
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(12.dp))
                    Text("Loading LLM…", fontSize = 14.sp)
                }
            }
        }
        Item(doctorVM, Modifier.weight(1f), "Doctor")
        Item(patientVM, Modifier.weight(1f), "Patient")
    }
}
