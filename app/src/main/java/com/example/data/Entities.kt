package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val email: String,
    val phoneWhatsapp: String,
    val gender: String, // "Male" / "Female" / "Prefer not to say"
    val role: String, // "Driver" / "Rider" / "Both"
    val organisation: String, // e.g. "USP", "GovNet", etc.
    val badgeLabel: String, // e.g. "Verified USP Member", "Verified GovNet Member"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "commute_profiles")
data class CommuteProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val homeSuburb: String,
    val workLocation: String,
    val departureTime: String, // format "HH:mm"
    val returnTime: String, // format "HH:mm"
    val daysActive: List<String>, // list like ["Mon", "Tue"]
    val seatsAvailable: Int = 1, // 1–4
    val womenOnly: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ride_matches")
data class RideMatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val driverUserId: Int,
    val riderUserId: Int,
    val routeDescription: String,
    val departureTime: String,
    val matchDate: String, // e.g. "2026-05-23"
    val status: String = "Pending", // "Pending" / "Confirmed" / "Completed"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "one_off_requests")
data class OneOffRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val requesterUserId: Int,
    val dateNeeded: String, // "2026-05-23"
    val pickupSuburb: String,
    val destination: String,
    val preferredTime: String,
    val notes: String = "",
    val isFulfilled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val message: String,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

class Converters {
    @TypeConverter
    fun fromString(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(",")
    }

    @TypeConverter
    fun fromList(list: List<String>?): String {
        if (list.isNullOrEmpty()) return ""
        return list.joinToString(",")
    }
}
