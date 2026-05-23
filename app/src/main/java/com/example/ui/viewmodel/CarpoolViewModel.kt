package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CarpoolViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CarpoolRepository(AppDatabase.getDatabase(application))

    // States
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _signupError = MutableStateFlow<String?>(null)
    val signupError: StateFlow<String?> = _signupError.asStateFlow()

    // Flows observed reactively
    val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMatches: StateFlow<List<RideMatchEntity>> = repository.getAllMatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val openRequests: StateFlow<List<OneOffRequestEntity>> = repository.getOpenOneOffRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _userMatches = MutableStateFlow<List<RideMatchEntity>>(emptyList())
    val userMatches: StateFlow<List<RideMatchEntity>> = _userMatches.asStateFlow()

    private val _myRequests = MutableStateFlow<List<OneOffRequestEntity>>(emptyList())
    val myRequests: StateFlow<List<OneOffRequestEntity>> = _myRequests.asStateFlow()

    private val _myCommuteProfile = MutableStateFlow<CommuteProfileEntity?>(null)
    val myCommuteProfile: StateFlow<CommuteProfileEntity?> = _myCommuteProfile.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    private val _stats = MutableStateFlow<PlatformStats>(PlatformStats(0, 0, 0, 0))
    val stats: StateFlow<PlatformStats> = _stats.asStateFlow()

    // Screen State / Navigation (simplifies state management in single MainActivity)
    private val _currentScreen = MutableStateFlow<String>("Splash") // "Splash" / "Onboarding" / "Auth" / "Home" / "Profile" / "Matches" / "OneOff" / "OpenRequests" / "Admin"
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Helper mapped lists (e.g. usernames for matches)
    private val _usersMap = MutableStateFlow<Map<Int, UserEntity>>(emptyMap())
    val usersMap: StateFlow<Map<Int, UserEntity>> = _usersMap.asStateFlow()

    init {
        viewModelScope.launch {
            // Seed mock profiles/users if db is empty on first startup
            repository.seedMockDataIfEmpty()
            
            // Collect all users to keep map populated for lookup
            repository.getAllUsers().collect { list ->
                _usersMap.value = list.associateBy { it.id }
                refreshStats()
            }
        }
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
        viewModelScope.launch {
            refreshStats()
        }
    }

    suspend fun refreshStats() {
        _stats.value = repository.getStats()
    }

    fun logout() {
        _currentUser.value = null
        _myCommuteProfile.value = null
        _userMatches.value = emptyList()
        _myRequests.value = emptyList()
        _notifications.value = emptyList()
        _currentScreen.value = "Auth"
        _loginError.value = null
        _signupError.value = null
    }

    fun loginWithEmail(email: String) {
        _loginError.value = null
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isEmpty()) {
            _loginError.value = "Please enter an email address."
            return
        }

        viewModelScope.launch {
            val user = repository.getUserByEmail(trimmedEmail)
            if (user != null) {
                _currentUser.value = user
                onUserSessionStarted(user)
            } else {
                // Check if the domain is valid for sign-up
                val orgBadge = extractOrgAndBadge(trimmedEmail)
                if (orgBadge == null) {
                    _loginError.value = "VeiliuCarpool is currently invite-only for verified organisations."
                } else {
                    _loginError.value = "Account not found. Please register using the sign-up tab below!"
                }
            }
        }
    }

    fun registerUser(
        fullName: String,
        email: String,
        phoneWhatsapp: String,
        gender: String,
        role: String
    ) {
        _signupError.value = null
        val trimmedEmail = email.trim().lowercase()

        if (fullName.trim().isEmpty()) {
            _signupError.value = "Full Name cannot be empty"
            return
        }
        if (phoneWhatsapp.trim().isEmpty()) {
            _signupError.value = "WhatsApp Number cannot be empty"
            return
        }
        if (trimmedEmail.isEmpty()) {
            _signupError.value = "Email address cannot be empty"
            return
        }

        val orgBadge = extractOrgAndBadge(trimmedEmail)
        if (orgBadge == null) {
            _signupError.value = "VeiliuCarpool is currently invite-only for verified organisations."
            return
        }

        val (organisation, badgeLabel) = orgBadge

        viewModelScope.launch {
            val existing = repository.getUserByEmail(trimmedEmail)
            if (existing != null) {
                _signupError.value = "Email is already registered. Please login instead."
                return@launch
            }

            val newUser = UserEntity(
                fullName = fullName.trim(),
                email = trimmedEmail,
                phoneWhatsapp = phoneWhatsapp.trim(),
                gender = gender,
                role = role,
                organisation = organisation,
                badgeLabel = badgeLabel
            )

            val id = repository.registerUser(newUser)
            val registeredUser = repository.getUserById(id.toInt())
            if (registeredUser != null) {
                _currentUser.value = registeredUser
                onUserSessionStarted(registeredUser)
            } else {
                _signupError.value = "Failed to register user. Please try again."
            }
        }
    }

    private fun onUserSessionStarted(user: UserEntity) {
        if (user.email == "admin@veiliu.com") {
            _currentScreen.value = "Admin"
        } else {
            _currentScreen.value = "Home"
        }

        // Start observing user-specific data
        viewModelScope.launch {
            // Observe matches
            repository.getMatchesForUser(user.id).collect {
                _userMatches.value = it
            }
        }

        viewModelScope.launch {
            // Observe commute profile
            repository.getCommuteProfileByUserIdFlow(user.id).collect {
                _myCommuteProfile.value = it
            }
        }

        viewModelScope.launch {
            // Observe my requests
            repository.getOneOffRequestsForUser(user.id).collect {
                _myRequests.value = it
            }
        }

        viewModelScope.launch {
            // Observe notifications
            repository.getNotificationsForUser(user.id).collect {
                _notifications.value = it
            }
        }

        viewModelScope.launch {
            refreshStats()
        }
    }

    fun saveCommuteProfile(
        homeSuburb: String,
        workLocation: String,
        departureTime: String,
        returnTime: String,
        daysActive: List<String>,
        seatsAvailable: Int,
        womenOnly: Boolean,
        isActive: Boolean,
        onSuccess: () -> Unit
    ) {
        val user = _currentUser.value ?: return
        val profile = CommuteProfileEntity(
            userId = user.id,
            homeSuburb = homeSuburb,
            workLocation = workLocation,
            departureTime = departureTime,
            returnTime = returnTime,
            daysActive = daysActive,
            seatsAvailable = seatsAvailable,
            womenOnly = womenOnly,
            isActive = isActive
        )

        viewModelScope.launch {
            repository.saveCommuteProfile(profile)
            _myCommuteProfile.value = repository.getCommuteProfileByUserId(user.id)
            refreshStats()
            onSuccess()
        }
    }

    fun createOneOffRequest(
        dateNeeded: String,
        pickupSuburb: String,
        destination: String,
        preferredTime: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        val user = _currentUser.value ?: return
        val request = OneOffRequestEntity(
            requesterUserId = user.id,
            dateNeeded = dateNeeded,
            pickupSuburb = pickupSuburb,
            destination = destination,
            preferredTime = preferredTime,
            notes = notes,
            isFulfilled = false
        )

        viewModelScope.launch {
            repository.createOneOffRequest(request)
            refreshStats()
            onSuccess()
        }
    }

    fun updateMatchStatus(matchId: Int, newStatus: String) {
        viewModelScope.launch {
            repository.updateMatchStatus(matchId, newStatus)
            refreshStats()
        }
    }

    fun fulfillOneOffRequest(requestId: Int) {
        viewModelScope.launch {
            repository.fulfillOneOffRequest(requestId, isFulfilled = true)
            refreshStats()
        }
    }

    fun dismissNotifications() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.markNotificationsAsRead(user.id)
        }
    }

    private fun extractOrgAndBadge(email: String): Pair<String, String>? {
        val clean = email.trim().lowercase()
        if (clean == "admin@veiliu.com") {
            return Pair("Veiliu", "Platform Admin")
        }
        return when {
            clean.endsWith("@govnet.gov.fj") -> Pair("GovNet", "Verified GovNet Member")
            clean.endsWith("@usp.ac.fj") -> Pair("USP", "Verified USP Member")
            clean.endsWith("@cwm.org.fj") -> Pair("CWM Hospital", "Verified CWM Hospital Member")
            clean.endsWith("@anz.com.fj") -> Pair("ANZ Bank", "Verified ANZ Bank Member")
            clean.endsWith("@bsp.com.fj") -> Pair("BSP Bank", "Verified BSP Bank Member")
            clean.endsWith("@gmail.com") -> Pair("Gmail", "Verified Gmail Member")
            else -> null
        }
    }
}
