package com.example.todoapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.todoapp.ui.theme.TodoAppTheme
import com.example.todoapp.ui.screens.TaskListScreen
import com.example.todoapp.ui.screens.AddEditTaskScreen
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Verificar Google Play Services
        if (!checkGooglePlayServices()) {
            Toast.makeText(this, "Google Play Services no está disponible", Toast.LENGTH_LONG).show()
            return
        }
        // Iniciar sesión anónima
        FirebaseAuth.getInstance().signInAnonymously()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Error en autenticación: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        setContent {
            TodoAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TodoApp()
                }
            }
        }
    }

    private fun checkGooglePlayServices(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, 9000)?.show()
            } else {
                println("Google Play Services no está disponible en este dispositivo")
            }
            return false
        }
        return true
    }
}

@Composable
fun TodoApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "task_list") {
        composable("task_list") {
            TaskListScreen(navController = navController)
        }
        composable("add_edit_task") {
            AddEditTaskScreen(navController = navController, taskId = null)
        }
        composable("add_edit_task/{taskId}") { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")
            AddEditTaskScreen(navController = navController, taskId = taskId)
        }
    }
}