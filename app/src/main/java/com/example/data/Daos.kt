package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserByIdFlow(id: Int): Flow<UserEntity?>

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUsersCount(): Int
}

@Dao
interface CommuteProfileDao {
    @Query("SELECT * FROM commute_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getCommuteProfileByUserId(userId: Int): CommuteProfileEntity?

    @Query("SELECT * FROM commute_profiles WHERE userId = :userId LIMIT 1")
    fun getCommuteProfileByUserIdFlow(userId: Int): Flow<CommuteProfileEntity?>

    @Query("SELECT * FROM commute_profiles WHERE isActive = 1")
    suspend fun getAllActiveProfiles(): List<CommuteProfileEntity>

    @Query("SELECT COUNT(*) FROM commute_profiles WHERE isActive = 1")
    suspend fun getActiveProfilesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: CommuteProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: CommuteProfileEntity)
}

@Dao
interface RideMatchDao {
    @Query("SELECT * FROM ride_matches ORDER BY createdAt DESC")
    fun getAllMatches(): Flow<List<RideMatchEntity>>

    @Query("SELECT * FROM ride_matches WHERE id = :id LIMIT 1")
    suspend fun getMatchById(id: Int): RideMatchEntity?

    @Query("SELECT * FROM ride_matches WHERE driverUserId = :userId OR riderUserId = :userId ORDER BY createdAt DESC")
    fun getMatchesForUser(userId: Int): Flow<List<RideMatchEntity>>

    @Query("SELECT * FROM ride_matches WHERE (driverUserId = :driverId AND riderUserId = :riderId AND matchDate = :date) LIMIT 1")
    suspend fun getMatch(driverId: Int, riderId: Int, date: String): RideMatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: RideMatchEntity): Long

    @Update
    suspend fun updateMatch(match: RideMatchEntity)

    @Query("SELECT COUNT(*) FROM ride_matches")
    suspend fun getMatchesCount(): Int
}

@Dao
interface OneOffRequestDao {
    @Query("SELECT * FROM one_off_requests ORDER BY createdAt DESC")
    fun getAllRequests(): Flow<List<OneOffRequestEntity>>

    @Query("SELECT * FROM one_off_requests WHERE id = :id LIMIT 1")
    suspend fun getRequestById(id: Int): OneOffRequestEntity?

    @Query("SELECT * FROM one_off_requests WHERE requesterUserId = :userId ORDER BY createdAt DESC")
    fun getRequestsForUser(userId: Int): Flow<List<OneOffRequestEntity>>

    @Query("SELECT * FROM one_off_requests WHERE isFulfilled = 0 ORDER BY createdAt DESC")
    fun getOpenRequests(): Flow<List<OneOffRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: OneOffRequestEntity): Long

    @Update
    suspend fun updateRequest(request: OneOffRequestEntity)

    @Query("SELECT COUNT(*) FROM one_off_requests")
    suspend fun getRequestsCount(): Int
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun getNotificationsForUser(userId: Int): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAsRead(userId: Int)
}
