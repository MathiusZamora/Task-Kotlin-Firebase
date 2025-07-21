
package com.example.todoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.todoapp.model.Task
import com.example.todoapp.repository.TaskRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.UUID
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(navController: NavController, taskId: String?) {
    val repository = TaskRepository()
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Formateador para la hora
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // Load existing task if editing
    LaunchedEffect(taskId) {
        if (taskId != null) {
            repository.getTasks().collect { tasks ->
                tasks.find { it.id == taskId }?.let { task ->
                    name = task.name
                    description = task.description
                    time = task.time
                }
            }
        }
    }

    // Verificar si hay un usuario autenticado
    val currentUser = FirebaseAuth.getInstance().currentUser
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            errorMessage = "No se pudo autenticar. Intenta de nuevo."
        } else {
            errorMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (taskId == null) "Add Task" else "Edit Task") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Task Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = time,
                onValueChange = { /* No editable directamente */ },
                label = { Text("Time (e.g., 14:30)") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }) {
                        Icon(Icons.Default.AccessTime, contentDescription = "Select Time")
                    }
                }
            )
            if (showTimePicker) {
                TimePickerDialog(
                    onConfirm = { hour, minute ->
                        time = LocalTime.of(hour, minute).format(timeFormatter)
                        showTimePicker = false
                    },
                    onCancel = { showTimePicker = false }
                )
            }
            Button(
                onClick = {
                    scope.launch {
                        if (currentUser == null) {
                            errorMessage = "No estás autenticado. Intenta de nuevo."
                            return@launch
                        }
                        if (time.isBlank()) {
                            errorMessage = "Por favor, selecciona una hora."
                            return@launch
                        }
                        try {
                            val task = Task(
                                id = taskId ?: UUID.randomUUID().toString(),
                                name = name,
                                description = description,
                                time = time,
                                uid = currentUser.uid
                            )
                            if (taskId == null) {
                                repository.addTask(task)
                            } else {
                                repository.updateTask(task)
                            }
                            navController.popBackStack()
                        } catch (e: Exception) {
                            errorMessage = "Error al guardar la tarea: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && time.isNotBlank() && currentUser != null
            ) {
                Text(if (taskId == null) "Add Task" else "Update Task")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onConfirm: (Int, Int) -> Unit,
    onCancel: () -> Unit
) {
    val timePickerState = rememberTimePickerState()

    AlertDialog(
        onDismissRequest = { onCancel() },
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = { onCancel() }) {
                Text("Cancel")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}