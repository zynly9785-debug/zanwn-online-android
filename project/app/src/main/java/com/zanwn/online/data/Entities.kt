package com.zanwn.online.data

import androidx.room.Entity
import androidx.room.PrimaryKey

interface SyncRecord { val id: String; val createdAt: Long; val updatedAt: Long; val syncStatus: String; val isDeleted: Boolean; val payload: String }

@Entity(tableName = "products")
data class ProductEntity(@PrimaryKey override val id: String, override val createdAt: Long, override val updatedAt: Long, override val syncStatus: String = "pending", override val isDeleted: Boolean = false, override val payload: String): SyncRecord
@Entity(tableName = "sales")
data class SaleEntity(@PrimaryKey override val id: String, override val createdAt: Long, override val updatedAt: Long, override val syncStatus: String = "pending", override val isDeleted: Boolean = false, override val payload: String): SyncRecord
@Entity(tableName = "customers")
data class CustomerEntity(@PrimaryKey override val id: String, override val createdAt: Long, override val updatedAt: Long, override val syncStatus: String = "pending", override val isDeleted: Boolean = false, override val payload: String): SyncRecord
@Entity(tableName = "expenses")
data class ExpenseEntity(@PrimaryKey override val id: String, override val createdAt: Long, override val updatedAt: Long, override val syncStatus: String = "pending", override val isDeleted: Boolean = false, override val payload: String): SyncRecord
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(@PrimaryKey val id: String, val action: String, val tableName: String, val recordId: String, val data: String, val status: String = "pending", val createdAt: Long = System.currentTimeMillis())
