package com.zanwn.online.data

import androidx.room.*

@Dao
interface ZanwnDao {
    @Query("SELECT * FROM sync_queue WHERE status = 'pending' ORDER BY createdAt") suspend fun pendingQueue(): List<SyncQueueEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun enqueue(item: SyncQueueEntity)
    @Query("UPDATE sync_queue SET status = 'synced' WHERE id = :id") suspend fun markSynced(id: String)
    @Query("DELETE FROM sync_queue WHERE status = 'synced'") suspend fun compactQueue()
    @Query("SELECT * FROM products WHERE isDeleted = 0") suspend fun products(): List<ProductEntity>
    @Query("SELECT * FROM sales WHERE isDeleted = 0") suspend fun sales(): List<SaleEntity>
    @Query("SELECT * FROM customers WHERE isDeleted = 0") suspend fun customers(): List<CustomerEntity>
    @Query("SELECT * FROM expenses WHERE isDeleted = 0") suspend fun expenses(): List<ExpenseEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertProduct(item: ProductEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSale(item: SaleEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCustomer(item: CustomerEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertExpense(item: ExpenseEntity)
}

@Database(entities = [ProductEntity::class, SaleEntity::class, CustomerEntity::class, ExpenseEntity::class, SyncQueueEntity::class], version = 1, exportSchema = false)
abstract class ZanwnDatabase : RoomDatabase() { abstract fun dao(): ZanwnDao }
