package com.zanwn.online.sync

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zanwn.online.data.ZanwnRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class SyncManager(private val context: Context, private val repository: ZanwnRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    fun syncNow() = scope.launch {
        runCatching {
            val uid = auth.currentUser?.uid ?: return@runCatching
            for (item in repository.dao.pendingQueue()) {
                val remote = mapOf("id" to item.recordId, "action" to item.action, "data" to item.data, "updatedAt" to System.currentTimeMillis(), "syncStatus" to "synced")
                db.collection("users").document(uid).collection(item.tableName).document(item.recordId).set(remote).await()
                repository.dao.markSynced(item.id)
            }
            repository.dao.compactQueue()
        }
    }
    fun startPeriodic() = scope.launch { while (isActive) { syncNow(); delay(60_000) } }
}
