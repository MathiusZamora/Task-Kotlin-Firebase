package com.example.todoapp.repository

import com.example.todoapp.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TaskRepository {
    private val db = FirebaseFirestore.getInstance()
    private val tasksCollection = db.collection("tasks")
    private val auth = FirebaseAuth.getInstance()

    fun getTasks(): Flow<List<Task>> = callbackFlow {
        val listener = tasksCollection
            .whereEqualTo("uid", auth.currentUser?.uid ?: "")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val tasks = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Task::class.java)?.copy(id = doc.id)
                    }
                    trySend(tasks).isSuccess
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun addTask(task: Task) {
        val taskWithUid = task.copy(uid = auth.currentUser?.uid ?: "")
        tasksCollection.add(taskWithUid).await()
    }

    suspend fun updateTask(task: Task) {
        val taskWithUid = task.copy(uid = auth.currentUser?.uid ?: "")
        tasksCollection.document(task.id).set(taskWithUid).await()
    }

    suspend fun deleteTask(taskId: String) {
        tasksCollection.document(taskId).delete().await()
    }
}