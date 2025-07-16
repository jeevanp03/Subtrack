package com.example.subtrack.ui.account

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: Account)

    @Query("SELECT * FROM accounts WHERE email = :email LIMIT 1")
    suspend fun getAccountByEmail(email: String): Account?
    
    @Query("SELECT * FROM accounts WHERE id = :userId LIMIT 1")
    suspend fun getAccountById(userId: Long): Account?
}
