package com.zanwn.online.data

import android.content.Context
import androidx.room.Room
import org.json.JSONObject
import java.util.UUID

class ZanwnRepository(context: Context) {
    private val db = Room.databaseBuilder(context, ZanwnDatabase::class.java, "zanwn.db").build()
    val dao: ZanwnDao = db.dao()
    suspend fun queue(table: String, action: String, id: String, data: String) = dao.enqueue(SyncQueueEntity(UUID.randomUUID().toString(), action, table, id, data))
    suspend fun localSnapshot(): JSONObject = JSONObject().apply {
        put("products", dao.products().map { JSONObject(it.payload) })
        put("sales", dao.sales().map { JSONObject(it.payload) })
        put("customers", dao.customers().map { JSONObject(it.payload) })
        put("expenses", dao.expenses().map { JSONObject(it.payload) })
    }
}
