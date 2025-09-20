package com.r114358.rosette

import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.r114358.rosette.traductor.Traductor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch




class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
        }


        setContent {
            val doctorLanguage = Languages.French
            val patientLanguage = Languages.English

            val doctorVM: MainScreenViewModel = viewModel(
                key = "doctor",
                factory = MainScreenViewModel.factory(doctorLanguage, patientLanguage, "doctor")
            )
            val patientVM: MainScreenViewModel = viewModel(
                key = "patient",
                factory = MainScreenViewModel.factory(patientLanguage, doctorLanguage, "patient")
            )

            MainScreen(doctorVM, patientVM)
        }
    }
}
