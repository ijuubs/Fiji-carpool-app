package com.example.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class CarpoolRepository(private val db: AppDatabase) {
    val userDao = db.userDao()
    val commuteProfileDao = db.commuteProfileDao()
    val rideMatchDao = db.rideMatchDao()
    val oneOffRequestDao = db.oneOffRequestDao()
    val notificationDao = db.notificationDao()

    // Users
    suspend fun getUserByEmail(email: String): UserEntity? {
        return userDao.getUserByEmail(email.trim().lowercase())
    }

    suspend fun getUserById(id: Int): UserEntity? {
        return userDao.getUserById(id)
    }

    fun getUserByIdFlow(id: Int): Flow<UserEntity?> {
        return userDao.getUserByIdFlow(id)
    }

    fun getAllUsers(): Flow<List<UserEntity>> {
        return userDao.getAllUsers()
    }

    suspend fun registerUser(user: UserEntity): Long {
        val cleanedUser = user.copy(email = user.email.trim().lowercase())
        val id = userDao.insertUser(cleanedUser)
        return id
    }

    // Commute Profiles
    suspend fun getCommuteProfileByUserId(userId: Int): CommuteProfileEntity? {
        return commuteProfileDao.getCommuteProfileByUserId(userId)
    }

    fun getCommuteProfileByUserIdFlow(userId: Int): Flow<CommuteProfileEntity?> {
        return commuteProfileDao.getCommuteProfileByUserIdFlow(userId)
    }

    suspend fun saveCommuteProfile(profile: CommuteProfileEntity): Long {
        val existing = commuteProfileDao.getCommuteProfileByUserId(profile.userId)
        val id = if (existing != null) {
            val updated = profile.copy(id = existing.id)
            commuteProfileDao.updateProfile(updated)
            existing.id.toLong()
        } else {
            commuteProfileDao.insertProfile(profile)
        }
        
        // Match automatically (if the profile is active)
        val savedProfile = commuteProfileDao.getCommuteProfileByUserId(profile.userId)
        if (savedProfile != null && savedProfile.isActive) {
            triggerMatchingForProfile(savedProfile)
        }
        
        return id
    }

    // Matches
    fun getAllMatches(): Flow<List<RideMatchEntity>> {
        return rideMatchDao.getAllMatches()
    }

    fun getMatchesForUser(userId: Int): Flow<List<RideMatchEntity>> {
        return rideMatchDao.getMatchesForUser(userId)
    }

    suspend fun updateMatchStatus(matchId: Int, newStatus: String) {
        val existing = rideMatchDao.getMatchById(matchId)
        if (existing != null) {
            val updated = existing.copy(status = newStatus)
            rideMatchDao.updateMatch(updated)
        }
    }

    // One-Off Requests
    fun getAllOneOffRequests(): Flow<List<OneOffRequestEntity>> {
        return oneOffRequestDao.getAllRequests()
    }

    fun getOneOffRequestsForUser(userId: Int): Flow<List<OneOffRequestEntity>> {
        return oneOffRequestDao.getRequestsForUser(userId)
    }

    fun getOpenOneOffRequests(): Flow<List<OneOffRequestEntity>> {
        return oneOffRequestDao.getOpenRequests()
    }

    suspend fun createOneOffRequest(request: OneOffRequestEntity): Long {
        return oneOffRequestDao.insertRequest(request)
    }

    suspend fun fulfillOneOffRequest(requestId: Int, isFulfilled: Boolean) {
        val existing = oneOffRequestDao.getRequestById(requestId)
        if (existing != null) {
            val updated = existing.copy(isFulfilled = isFulfilled)
            oneOffRequestDao.updateRequest(updated)
        }
    }

    // Notifications
    fun getNotificationsForUser(userId: Int): Flow<List<NotificationEntity>> {
        return notificationDao.getNotificationsForUser(userId)
    }

    suspend fun markNotificationsAsRead(userId: Int) {
        notificationDao.markAsRead(userId)
    }

    // Statistics
    suspend fun getStats(): PlatformStats {
        val totalUsers = userDao.getUsersCount()
        val activeProfiles = commuteProfileDao.getActiveProfilesCount()
        val totalMatches = rideMatchDao.getMatchesCount()
        val oneOffRequests = oneOffRequestDao.getRequestsCount()
        return PlatformStats(totalUsers, activeProfiles, totalMatches, oneOffRequests)
    }

    // Seeding helper to seed default values if db is empty
    suspend fun seedMockDataIfEmpty() {
        val userCount = userDao.getUsersCount()
        if (userCount > 0) return

        // Seed Admin Account
        userDao.insertUser(
            UserEntity(
                fullName = "System Administrator",
                email = "admin@veiliu.com",
                phoneWhatsapp = "6799999999",
                gender = "Prefer not to say",
                role = "Both",
                organisation = "Veiliu",
                badgeLabel = "Platform Admin"
            )
        )

        // Seed 5 Corporate/Student Users
        val user1 = UserEntity(
            fullName = "Anare Ravula",
            email = "anre.ravula@govnet.gov.fj",
            phoneWhatsapp = "6798881234",
            gender = "Male",
            role = "Driver",
            organisation = "GovNet",
            badgeLabel = "Verified GovNet Member"
        )
        val id1 = userDao.insertUser(user1).toInt()

        val user2 = UserEntity(
            fullName = "Sulueti Vakaloloma",
            email = "sulu.v@usp.ac.fj",
            phoneWhatsapp = "6797775678",
            gender = "Female",
            role = "Rider",
            organisation = "USP",
            badgeLabel = "Verified USP Member"
        )
        val id2 = userDao.insertUser(user2).toInt()

        val user3 = UserEntity(
            fullName = "Kitione Waqabaca",
            email = "kiti.waqa@bsp.com.fj",
            phoneWhatsapp = "6795554321",
            gender = "Male",
            role = "Both",
            organisation = "BSP Bank",
            badgeLabel = "Verified BSP Bank Member"
        )
        val id3 = userDao.insertUser(user3).toInt()

        val user4 = UserEntity(
            fullName = "Priya Chand",
            email = "priya.chand@cwm.org.fj",
            phoneWhatsapp = "6799112233",
            gender = "Female",
            role = "Rider",
            organisation = "CWM Hospital",
            badgeLabel = "Verified CWM Hospital Member"
        )
        val id4 = userDao.insertUser(user4).toInt()

        val user5 = UserEntity(
            fullName = "John Smith",
            email = "john.smith@anz.com.fj",
            phoneWhatsapp = "6793366991",
            gender = "Male",
            role = "Driver",
            organisation = "ANZ Bank",
            badgeLabel = "Verified ANZ Bank Member"
        )
        val id5 = userDao.insertUser(user5).toInt()

        // Seed Commute Profiles
        // Anare: GovNet Driver, Nakasi -> Suva Central, 07:45 to 16:30
        commuteProfileDao.insertProfile(
            CommuteProfileEntity(
                userId = id1,
                homeSuburb = "Nakasi",
                workLocation = "Suva Central",
                departureTime = "07:45",
                returnTime = "16:30",
                daysActive = listOf("Mon", "Tue", "Wed", "Thu", "Fri"),
                seatsAvailable = 3,
                womenOnly = false,
                isActive = true
            )
        )

        // Sulueti: USP Rider, Nakasi -> Laucala Campus (USP), 08:00 to 17:00
        commuteProfileDao.insertProfile(
            CommuteProfileEntity(
                userId = id2,
                homeSuburb = "Nakasi",
                workLocation = "Laucala Campus",
                departureTime = "08:00",
                returnTime = "17:00",
                daysActive = listOf("Mon", "Tue", "Wed", "Thu", "Fri"),
                seatsAvailable = 1,
                womenOnly = false,
                isActive = true
            )
        )

        // Kitione: BSP Both, Nausori -> Suva Central, 08:15 to 17:15
        commuteProfileDao.insertProfile(
            CommuteProfileEntity(
                userId = id3,
                homeSuburb = "Nausori",
                workLocation = "Suva Central",
                departureTime = "08:15",
                returnTime = "17:15",
                daysActive = listOf("Mon", "Tue", "Wed", "Thu"),
                seatsAvailable = 2,
                womenOnly = false,
                isActive = true
            )
        )

        // Priya: CWM Rider, Lami -> Suva Central, 07:30 to 16:00, Women Only
        commuteProfileDao.insertProfile(
            CommuteProfileEntity(
                userId = id4,
                homeSuburb = "Lami",
                workLocation = "Suva Central",
                departureTime = "07:30",
                returnTime = "16:00",
                daysActive = listOf("Mon", "Tue", "Wed", "Thu", "Fri"),
                seatsAvailable = 1,
                womenOnly = true,
                isActive = true
            )
        )

        // Seed some matches manually
        rideMatchDao.insertMatch(
            RideMatchEntity(
                driverUserId = id1,
                riderUserId = id2,
                routeDescription = "Nakasi → Suva / Laucala",
                departureTime = "07:45",
                matchDate = "2026-05-23",
                status = "Pending"
            )
        )

        // Seed some one-off requests
        oneOffRequestDao.insertRequest(
            OneOffRequestEntity(
                requesterUserId = id2,
                dateNeeded = "2026-05-25",
                pickupSuburb = "Nausori",
                destination = "USP Laucala Campus",
                preferredTime = "08:30",
                notes = "Need early morning lift, happy to split fuel cost.",
                isFulfilled = false
            )
        )
        oneOffRequestDao.insertRequest(
            OneOffRequestEntity(
                requesterUserId = id4,
                dateNeeded = "2026-05-26",
                pickupSuburb = "Lami",
                destination = "CWM Hospital",
                preferredTime = "07:15",
                notes = "Flexible +- 15 mins. Women only, please.",
                isFulfilled = false
            )
        )
    }

    // Automated Matching Implementation
    suspend fun triggerMatchingForProfile(newProfile: CommuteProfileEntity) {
        val activeProfiles = commuteProfileDao.getAllActiveProfiles()
        val ownerUser = userDao.getUserById(newProfile.userId) ?: return

        for (other in activeProfiles) {
            if (other.userId == newProfile.userId) continue

            // 1. Matches home suburb (case-insensitive)
            val homeMatch = other.homeSuburb.trim().equals(newProfile.homeSuburb.trim(), ignoreCase = true)
            if (!homeMatch) continue

            // 2. Matches work location (case-insensitive)
            val workMatch = other.workLocation.trim().equals(newProfile.workLocation.trim(), ignoreCase = true)
            if (!workMatch) continue

            // 3. Time buffer is within 30 minutes
            val time1 = parseTimeToMinutes(newProfile.departureTime)
            val time2 = parseTimeToMinutes(other.departureTime)
            if (time1 == null || time2 == null) continue
            val timeDiff = abs(time1 - time2)
            if (timeDiff > 30) continue

            // 4. Overlapping days
            val daysOverlap = newProfile.daysActive.any { other.daysActive.contains(it) }
            if (!daysOverlap) continue

            // 5. Role filter
            val otherUser = userDao.getUserById(other.userId) ?: continue
            val isCompatible = matchesRole(ownerUser.role, otherUser.role)
            if (!isCompatible) continue

            // 6. Women-only filter
            // "if either profile has women_only = Yes, only match with profiles also marked women_only = Yes"
            if (newProfile.womenOnly || other.womenOnly) {
                if (!(newProfile.womenOnly && other.womenOnly)) {
                    continue // mismatch in women_only filter
                }
            }

            // Check if match already exists
            val todayStr = getTodayString()
            // To decide who is driver and who is rider:
            val driverId: Int
            val riderId: Int
            if (ownerUser.role == "Driver") {
                driverId = ownerUser.id
                riderId = other.userId
            } else if (otherUser.role == "Driver") {
                driverId = other.userId
                riderId = ownerUser.id
            } else {
                // Roles are "Both" or combinations, pick arbitrarily
                driverId = if (ownerUser.id < other.userId) ownerUser.id else other.userId
                riderId = if (driverId == ownerUser.id) other.userId else ownerUser.id
            }

            val existing = rideMatchDao.getMatch(driverId, riderId, todayStr)
            if (existing == null) {
                val routeDesc = "${newProfile.homeSuburb} → ${newProfile.workLocation}"
                val match = RideMatchEntity(
                    driverUserId = driverId,
                    riderUserId = riderId,
                    routeDescription = routeDesc,
                    departureTime = newProfile.departureTime,
                    matchDate = todayStr,
                    status = "Pending"
                )
                rideMatchDao.insertMatch(match)

                // Notifications to BOTH
                val dUser = userDao.getUserById(driverId)
                val rUser = userDao.getUserById(riderId)
                if (dUser != null && rUser != null) {
                    val msgToDriver = "🎉 Match found! ${rUser.fullName} is your carpool partner for $routeDesc at ${match.departureTime}. Tap to view and contact them on WhatsApp."
                    val msgToRider = "🎉 Match found! ${dUser.fullName} is your carpool partner for $routeDesc at ${match.departureTime}. Tap to view and contact them on WhatsApp."
                    
                    notificationDao.insertNotification(NotificationEntity(userId = driverId, message = msgToDriver))
                    notificationDao.insertNotification(NotificationEntity(userId = riderId, message = msgToRider))
                }
            }
        }
    }

    private fun parseTimeToMinutes(timeStr: String): Int? {
        return try {
            val parts = timeStr.split(":")
            if (parts.size != 2) return null
            val hour = parts[0].toInt()
            val min = parts[1].toInt()
            hour * 60 + min
        } catch (e: Exception) {
            null
        }
    }

    private fun matchesRole(role1: String, role2: String): Boolean {
        if (role1 == "Both" || role2 == "Both") return true
        return role1 != role2
    }

    fun getTodayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}

data class PlatformStats(
    val totalUsers: Int,
    val activeProfiles: Int,
    val totalMatches: Int,
    val oneOffRequests: Int
)
