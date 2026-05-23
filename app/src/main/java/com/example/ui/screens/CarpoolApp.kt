package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.CarpoolViewModel
import java.util.Calendar

// Helper to open WhatsApp URL in Android safely without raw phone exposure
fun openWhatsAppLink(context: Context, rawPhone: String) {
    try {
        val cleanPhone = rawPhone.replace("+", "").replace(" ", "").trim()
        val url = "https://wa.me/$cleanPhone"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback
    }
}

// Dialog helper triggers
fun showTimePicker(context: Context, initialTime: String, onTimeSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    var initHour = calendar.get(Calendar.HOUR_OF_DAY)
    var initMin = calendar.get(Calendar.MINUTE)
    
    if (initialTime.contains(":")) {
        val parts = initialTime.split(":")
        if (parts.size == 2) {
            parts[0].toIntOrNull()?.let { initHour = it }
            parts[1].toIntOrNull()?.let { initMin = it }
        }
    }

    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val formatted = String.format("%02d:%02d", hourOfDay, minute)
            onTimeSelected(formatted)
        },
        initHour,
        initMin,
        true
    ).show()
}

fun showDatePicker(context: Context, onDateSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    DatePickerDialog(
        context,
        { _, selYear, selMonth, selDay ->
            val formatted = String.format("%04d-%02d-%02d", selYear, selMonth + 1, selDay)
            onDateSelected(formatted)
        },
        year,
        month,
        day
    ).show()
}

@Composable
fun CarpoolApp(viewModel: CarpoolViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                "Splash" -> {
                    SplashScreen(viewModel)
                }
                "Onboarding" -> {
                    OnboardingScreen(viewModel)
                }
                "Auth" -> {
                    AuthScreen(viewModel)
                }
                else -> {
                    // Header (Teal with subtitle)
                    HeaderBar(
                        onLogout = { viewModel.logout() },
                        isAdmin = currentUser?.email == "admin@veiliu.com",
                        currentScreen = currentScreen,
                        onNavigateBack = { viewModel.navigateTo("Home") }
                    )

                // In-App Notification Overlay banner (dismissible)
                notifications.filter { !it.isRead }.forEach { alert ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .border(1.dp, CoralAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = SoftPink),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Match Found Alert",
                                tint = CoralAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = alert.message,
                                fontSize = 14.sp,
                                color = DarkCharcoal,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(
                                onClick = { viewModel.dismissNotifications() },
                                modifier = Modifier.testTag("dismiss_alert_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss Alert",
                                    tint = MutedText
                                )
                            }
                        }
                    }
                }

                // Main screen routing
                Box(modifier = Modifier.weight(1f)) {
                    when (currentScreen) {
                        "Home" -> HomeScreen(viewModel)
                        "Profile" -> CommuteProfileScreen(viewModel)
                        "Matches" -> MatchesScreen(viewModel)
                        "OneOff" -> OneOffRequestScreen(viewModel)
                        "OpenRequests" -> OpenRequestsScreen(viewModel)
                        "Admin" -> AdminScreen(viewModel)
                    }
                }

                // Custom Bottom Navigation Bar (Deep Teal + Coral accents)
                if (currentUser?.email != "admin@veiliu.com") {
                    NavigationBar(
                        containerColor = DeepTeal,
                        contentColor = Color.White,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        val items = mutableListOf(
                            Triple("Home", "Dashboard", Icons.Default.Dashboard),
                            Triple("Profile", "Commute", Icons.Default.Person),
                            Triple("Matches", "Matches", Icons.Default.Group),
                            Triple("OneOff", "One-Off", Icons.Default.CalendarMonth)
                        )

                        // If user is a Driver, they can optionally view open requests as secondary nav or tab.
                        // Let's make OpenRequests accessible easily on bottom nav or in tabs.
                        // Bottom nav is perfect to have positive direct access!
                        if (currentUser?.role == "Driver" || currentUser?.role == "Both") {
                            items.add(Triple("OpenRequests", "Open Lifts", Icons.Default.DirectionsCar))
                        }

                        items.forEach { (route, label, icon) ->
                            val isSelected = currentScreen == route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.navigateTo(route) },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) DeepTeal else Color.White.copy(alpha = 0.7f)
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = CoralAccent
                                ),
                                modifier = Modifier.testTag("nav_item_$route")
                            )
                        }
                    }
                } // Ends if (currentUser?.email != "admin@veiliu.com")
            } // Ends else -> of when
        } // Ends when
    } // Ends Column
} // Ends Surface
} // Ends CarpoolApp

@Composable
fun HeaderBar(
    onLogout: () -> Unit,
    isAdmin: Boolean,
    currentScreen: String,
    onNavigateBack: () -> Unit
) {
    Surface(
        color = DeepTeal,
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentScreen != "Home" && currentScreen != "Admin") {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to home",
                        tint = Color.White
                    )
                }
            } else {
                IconButton(onClick = {}, enabled = false) {
                    Text("🔔", fontSize = 18.sp)
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "VeiliuCarpool",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "CONNECT. COMMUTE. SAVE.",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }

            IconButton(onClick = onLogout, modifier = Modifier.testTag("logout_button")) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = Color.White
                )
            }
        }
    }
}

// RESTRICTED AUTHENTICATION SCREEN  
@Composable
fun AuthScreen(viewModel: CarpoolViewModel) {
    var isLoginTab by remember { mutableStateOf(true) }
    val loginError by viewModel.loginError.collectAsState()
    val signupError by viewModel.signupError.collectAsState()

    // Form states
    var loginEmail by remember { mutableStateOf(TextFieldValue("")) }
    var signupEmail by remember { mutableStateOf(TextFieldValue("")) }
    var fullName by remember { mutableStateOf(TextFieldValue("")) }
    var phoneWhatsapp by remember { mutableStateOf(TextFieldValue("")) }
    var gender by remember { mutableStateOf("Male") }
    var role by remember { mutableStateOf("Rider") }

    val domainsList = listOf("govnet.gov.fj", "usp.ac.fj", "cwm.org.fj", "anz.com.fj", "bsp.com.fj", "gmail.com")

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = DarkCharcoal,
        unfocusedTextColor = DarkCharcoal,
        focusedLabelColor = DeepTeal,
        unfocusedLabelColor = DarkCharcoal.copy(alpha = 0.7f),
        focusedBorderColor = DeepTeal,
        unfocusedBorderColor = DarkCharcoal.copy(alpha = 0.3f),
        focusedPlaceholderColor = DarkCharcoal.copy(alpha = 0.5f),
        unfocusedPlaceholderColor = DarkCharcoal.copy(alpha = 0.5f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSand)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        // App visual Identity logo placeholder
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(DeepTeal),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = "Carpool Logo",
                tint = CoralAccent,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "VeiliuCarpool",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = DeepTeal
        )
        Text(
            text = "Fiji's Corporate Carpooling Platform",
            fontSize = 14.sp,
            color = MutedText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Tab Selector
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(Color.White)
                .padding(4.dp)
        ) {
            Button(
                onClick = { isLoginTab = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLoginTab) DeepTeal else Color.Transparent,
                    contentColor = if (isLoginTab) Color.White else DeepTeal
                ),
                modifier = Modifier.weight(1f).testTag("tab_login")
            ) {
                Text("Login")
            }
            Button(
                onClick = { isLoginTab = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isLoginTab) DeepTeal else Color.Transparent,
                    contentColor = if (!isLoginTab) Color.White else DeepTeal
                ),
                modifier = Modifier.weight(1f).testTag("tab_signup")
            ) {
                Text("Sign Up")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoginTab) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Welcome Back",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepTeal
                    )
                    Text(
                        text = "Sign in with your verified institutional email address.",
                        fontSize = 13.sp,
                        color = MutedText
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    loginError?.let {
                        Text(
                            text = it,
                            color = Color.Red,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = loginEmail,
                        onValueChange = { loginEmail = it },
                        label = { Text("Institutional Email") },
                        placeholder = { Text("username@usp.ac.fj") },
                        modifier = Modifier.fillMaxWidth().testTag("login_email_input"),
                        singleLine = true,
                        colors = textFieldColors
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.loginWithEmail(loginEmail.text) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CoralAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Sign In Safely", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Create Verified Account",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepTeal
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    signupError?.let {
                        Text(
                            text = it,
                            color = Color.Red,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        placeholder = { Text("e.g. Siteri Sukuna") },
                        modifier = Modifier.fillMaxWidth().testTag("signup_name_input"),
                        singleLine = true,
                        colors = textFieldColors
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = signupEmail,
                        onValueChange = { signupEmail = it },
                        label = { Text("Institutional Email") },
                        placeholder = { Text("e.g. username@usp.ac.fj") },
                        modifier = Modifier.fillMaxWidth().testTag("signup_email_input"),
                        singleLine = true,
                        colors = textFieldColors
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phoneWhatsapp,
                        onValueChange = { phoneWhatsapp = it },
                        label = { Text("WhatsApp Phone Number (with Country Code)") },
                        placeholder = { Text("e.g. 6799991234") },
                        modifier = Modifier.fillMaxWidth().testTag("signup_phone_input"),
                        singleLine = true,
                        colors = textFieldColors
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Gender Select
                    Text("Gender", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Male", "Female", "Prefer not to say").forEach { option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { gender = option }
                            ) {
                                RadioButton(
                                    selected = gender == option,
                                    onClick = { gender = option },
                                    colors = RadioButtonDefaults.colors(selectedColor = DeepTeal)
                                )
                                Text(option, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Role Select
                    Text("Role Category", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Rider", "Driver", "Both").forEach { option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { role = option }
                            ) {
                                RadioButton(
                                    selected = role == option,
                                    onClick = { role = option },
                                    colors = RadioButtonDefaults.colors(selectedColor = DeepTeal)
                                )
                                Text(option, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.registerUser(
                                fullName.text,
                                signupEmail.text,
                                phoneWhatsapp.text,
                                gender,
                                role
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("signup_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CoralAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Register and Validate", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Authorized Domains in Suva, Fiji:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = DeepTeal
        )
        domainsList.forEach { domain ->
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Eligible",
                    tint = Color(0xFF1B5C55),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "@$domain", fontSize = 12.sp, color = DarkCharcoal)
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// HOME / DASHBOARD SCREEN
@Composable
fun HomeScreen(viewModel: CarpoolViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val userMatches by viewModel.userMatches.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val usersMap by viewModel.usersMap.collectAsState()

    val context = LocalContext.current

    // Find "Your Match Today"
    // Usually is a match with status = "Pending" or "Confirmed" for today
    // Let's grab the first non-completed match
    val matchToday = userMatches.firstOrNull { it.status != "Completed" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSand)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Institutional Badge (Bula, Avinesh / USP verified badge)
        item {
            val firstName = currentUser?.fullName?.split(" ")?.firstOrNull() ?: ""

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bula, $firstName",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = DeepTeal
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Verified institutional badge inline Row with small dot indicator
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(50.dp))
                            .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(50.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("verified_badge"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF4CAF50), androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentUser?.badgeLabel ?: "Verified USP Member",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Profile card item (bento border style)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(1.dp, DeepTeal.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👤", fontSize = 22.sp, textAlign = TextAlign.Center)
                }
            }
        }

        // Active match today card formatted beautifully for Bento Grid
        item {
            if (matchToday != null) {
                // Find other person details
                val me = currentUser ?: return@item
                val isDriver = matchToday.driverUserId == me.id
                val otherUserId = if (isDriver) matchToday.riderUserId else matchToday.driverUserId
                val otherUser = usersMap[otherUserId]
                
                val peerName = otherUser?.fullName ?: "Verified Partner"
                val peerPhone = otherUser?.phoneWhatsapp ?: ""
                val peerGender = otherUser?.gender ?: ""
                val peerRoleStr = if (isDriver) "Rider" else "Driver"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, DeepTeal.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "YOUR MATCH TODAY",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = DeepTeal.copy(alpha = 0.5f),
                                letterSpacing = 1.sp
                            )
                            
                            // Status code badge
                            Surface(
                                color = if (matchToday.status == "Confirmed") CoralAccent else DeepTeal,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = matchToday.status.uppercase(),
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar circle container from bento theme
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(DeepTeal.copy(alpha = 0.05f), androidx.compose.foundation.shape.CircleShape)
                                    .border(1.dp, DeepTeal.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isDriver) "🚗" else "👤",
                                    fontSize = 24.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = peerName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepTeal
                                    )
                                    if (peerGender == "Female") {
                                        Text(" 🌸", fontSize = 14.sp)
                                    }
                                }
                                Text(
                                    text = matchToday.routeDescription,
                                    fontSize = 14.sp,
                                    color = DeepTeal.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "${matchToday.departureTime} Departure",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralAccent,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // WhatsApp Action
                        Button(
                            onClick = { openWhatsAppLink(context, peerPhone) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), // WhatsApp Official Green
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("contact_whatsapp_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("💬 Contact on WhatsApp", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        }

                        // Complete / Confirm controls
                        if (matchToday.status == "Pending") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.updateMatchStatus(matchToday.id, "Confirmed") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("confirm_match_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Confirm Route", fontWeight = FontWeight.Bold)
                            }
                        } else if (matchToday.status == "Confirmed") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.updateMatchStatus(matchToday.id, "Completed") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("complete_match_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = CoralAccent),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Mark Trip Completed", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, DeepTeal.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "No Match",
                            tint = MutedText,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No match yet — complete your commute profile below to trigger automated matches.",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = MutedText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Stats Row Bento
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Total Matches
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, DeepTeal.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "TOTAL MATCHES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepTeal.copy(alpha = 0.4f),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${stats.totalMatches}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepTeal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+5 this week",
                            fontSize = 10.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Card 2: Fuel Saved
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, DeepTeal.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "FUEL SAVED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepTeal.copy(alpha = 0.4f),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val fuelSaved = stats.totalMatches * 4.50
                        Text(
                            text = String.format("$%.2f", fuelSaved),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = CoralAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "FJD equivalent",
                            fontSize = 10.sp,
                            color = DeepTeal.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Action Grid Bento
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Button 1: Commute Profile (DeepTeal background)
                Card(
                    onClick = { viewModel.navigateTo("Profile") },
                    modifier = Modifier
                        .weight(1f)
                        .height(115.dp)
                        .testTag("quick_action_profile"),
                    colors = CardDefaults.cardColors(containerColor = DeepTeal),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("📅", fontSize = 24.sp)
                        Text(
                            text = "Commute\nProfile",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Button 2: One-Off Request (White background with 2.dp DeepTeal border)
                Card(
                    onClick = { viewModel.navigateTo("OneOff") },
                    modifier = Modifier
                        .weight(1f)
                        .height(115.dp)
                        .testTag("quick_action_oneoff"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(2.dp, DeepTeal),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("⚡", fontSize = 24.sp)
                        Text(
                            text = "One-Off\nRequest",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepTeal,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// MY COMMUTE PROFILE SCREEN
@Composable
fun CommuteProfileScreen(viewModel: CarpoolViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val myCommuteProfile by viewModel.myCommuteProfile.collectAsState()

    val context = LocalContext.current

    // Form inputs state holding local changes
    var homeSuburb by remember { mutableStateOf("") }
    var workLocation by remember { mutableStateOf("") }
    var departureTime by remember { mutableStateOf("08:00") }
    var returnTime by remember { mutableStateOf("17:00") }
    var seatsAvailable by remember { mutableStateOf(1) }
    var womenOnly by remember { mutableStateOf(false) }
    var isActive by remember { mutableStateOf(true) }

    val daysList = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
    val selectedDays = remember { mutableStateListOf<String>() }

    // Init form when database value is loaded
    LaunchedEffect(myCommuteProfile) {
        myCommuteProfile?.let {
            homeSuburb = it.homeSuburb
            workLocation = it.workLocation
            departureTime = it.departureTime
            returnTime = it.returnTime
            seatsAvailable = it.seatsAvailable
            womenOnly = it.womenOnly
            isActive = it.isActive
            selectedDays.clear()
            selectedDays.addAll(it.daysActive)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSand)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "My Commute Profile",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = DeepTeal
            )
            Text(
                text = "Set up your recurring daily commute variables to enable automatic AI matching.",
                fontSize = 14.sp,
                color = MutedText
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, DeepTeal.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Home Suburb input
                    OutlinedTextField(
                        value = homeSuburb,
                        onValueChange = { homeSuburb = it },
                        label = { Text("Home Suburb") },
                        placeholder = { Text("e.g. Nakasi, Nausori, Lami, Flagstaff") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_home_input"),
                        singleLine = true,
                        trailingIcon = {
                            LocationSelectorTrailingIcon(
                                onLocationFound = { homeSuburb = it },
                                isWorkOrDest = false
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Work Campus Location input
                    OutlinedTextField(
                        value = workLocation,
                        onValueChange = { workLocation = it },
                        label = { Text("Work / Campus Location") },
                        placeholder = { Text("e.g. Suva Central, USP Laucala Campus") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_work_input"),
                        singleLine = true,
                        trailingIcon = {
                            LocationSelectorTrailingIcon(
                                onLocationFound = { workLocation = it },
                                isWorkOrDest = true
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Departure & Return time selectors clicking timepicker dialog
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                showTimePicker(context, departureTime) {
                                    departureTime = it
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftSand),
                            border = BorderStroke(1.dp, MutedText.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f).height(55.dp).testTag("select_departure_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Departure Time", fontSize = 10.sp, color = MutedText)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = DeepTeal)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(departureTime, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                showTimePicker(context, returnTime) {
                                    returnTime = it
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftSand),
                            border = BorderStroke(1.dp, MutedText.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f).height(55.dp).testTag("select_return_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Return Time", fontSize = 10.sp, color = MutedText)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = DeepTeal)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(returnTime, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Days Active Checklist
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, DeepTeal.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Days Active Match", fontWeight = FontWeight.Bold, color = DeepTeal, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        daysList.forEach { day ->
                            val checked = selectedDays.contains(day)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    if (checked) selectedDays.remove(day) else selectedDays.add(day)
                                }
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        if (it == true) selectedDays.add(day) else selectedDays.remove(day)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = DeepTeal)
                                )
                                Text(day, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Driver settings optional segment
        val isDriverSegment = currentUser?.role == "Driver" || currentUser?.role == "Both"
        if (isDriverSegment) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, DeepTeal.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Driver & Preferences", fontWeight = FontWeight.Bold, color = DeepTeal, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Seats scale
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Seats Available (1-4)", fontSize = 14.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (seatsAvailable > 1) seatsAvailable-- },
                                    modifier = Modifier.testTag("seats_deg_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                                }
                                Text("$seatsAvailable", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(
                                    onClick = { if (seatsAvailable < 4) seatsAvailable++ },
                                    modifier = Modifier.testTag("seats_inc_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = "Increase")
                                }
                            }
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // Women Only Carpool Circle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text("Women-Only Circle 🌸", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Switch(
                                checked = womenOnly,
                                onCheckedChange = { womenOnly = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = CoralAccent, checkedTrackColor = DeepTeal),
                                modifier = Modifier.testTag("women_only_switch")
                            )
                        }
                        Text(
                            text = "Only match me with women drivers/riders",
                            fontSize = 12.sp,
                            color = MutedText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }
        }

        // Pause Profile active switcher
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, DeepTeal.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isActive) "Profile Active & Matching" else "Profile Paused",
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) DeepTeal else MutedText,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Pause matching without deleting commute parameters.",
                            fontSize = 11.sp,
                            color = MutedText
                        )
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        modifier = Modifier.testTag("active_status_switch"),
                        colors = SwitchDefaults.colors(checkedThumbColor = CoralAccent, checkedTrackColor = DeepTeal)
                    )
                }
            }
        }

        // Save Button actions
        item {
            Button(
                onClick = {
                    viewModel.saveCommuteProfile(
                        homeSuburb = homeSuburb,
                        workLocation = workLocation,
                        departureTime = departureTime,
                        returnTime = returnTime,
                        daysActive = selectedDays.toList(),
                        seatsAvailable = seatsAvailable,
                        womenOnly = womenOnly,
                        isActive = isActive,
                        onSuccess = {
                            viewModel.navigateTo("Home")
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_profile_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CoralAccent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save and Trigger Match Algorithm", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun FijianFuelShareCalculator(
    initialRouteDesc: String,
    modifier: Modifier = Modifier
) {
    val routes = listOf(
        "Suva - Nausori Corridor (18 km)" to 18.0,
        "Lami - Suva Corridor (6 km)" to 6.0,
        "Pacific Harbour - Suva Corridor (50 km)" to 50.0,
        "Nadi - Lautoka Corridor (25 km)" to 25.0,
        "Custom Distance" to 15.0
    )

    val defaultIndex = remember(initialRouteDesc) {
        val desc = initialRouteDesc.lowercase()
        when {
            desc.contains("nausori") -> 0
            desc.contains("lami") -> 1
            desc.contains("pacific") -> 2
            desc.contains("nadi") || desc.contains("lautoka") -> 3
            else -> 4 // Custom
        }
    }

    var selectedRouteIndex by remember { mutableStateOf(defaultIndex) }
    var customDistance by remember { mutableStateOf(15) }
    var fuelPrice by remember { mutableStateOf(2.95f) } // default FJD 2.95
    var fuelConsumption by remember { mutableStateOf(8.0f) } // L/100km
    var splitCount by remember { mutableStateOf(3) } // Partners count split (including driver)
    var isRoundTrip by remember { mutableStateOf(true) }

    val distance = if (selectedRouteIndex == 4) customDistance.toDouble() else routes[selectedRouteIndex].second
    val multiplier = if (isRoundTrip) 2 else 1
    val totalDistance = distance * multiplier
    val totalFuelLitres = (totalDistance * fuelConsumption) / 100.0
    val totalCost = totalFuelLitres * fuelPrice
    val sharePerRider = if (splitCount > 0) totalCost / splitCount else 0.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LightSand.copy(alpha = 0.5f))
            .border(1.dp, DeepTeal.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.LocalGasStation,
                contentDescription = null,
                tint = DeepTeal,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Fijian Fuel-Share Calculator",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DeepTeal
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))

        // Route selection dropdown-like buttons
        Text(
            text = "Select Route Corridor:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MutedText
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            routes.forEachIndexed { index, (label, _) ->
                val isSelected = selectedRouteIndex == index
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) DeepTeal else Color.White)
                        .clickable { selectedRouteIndex = index }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { selectedRouteIndex = index },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color.White,
                            unselectedColor = DeepTeal
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else DarkCharcoal
                    )
                }
            }
        }

        if (selectedRouteIndex == 4) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Custom Distance:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkCharcoal
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (customDistance > 1) customDistance-- },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = DeepTeal)
                    }
                    Text(
                        text = "$customDistance km",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkCharcoal,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(
                        onClick = { if (customDistance < 500) customDistance++ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = DeepTeal)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Fuel Price Controller
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Fiji Gas Price (FJD/L):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = DarkCharcoal
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (fuelPrice > 1.50f) fuelPrice = (fuelPrice - 0.05f).coerceAtLeast(1.50f) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease Fuel Price", tint = DeepTeal)
                }
                Text(
                    text = String.format("$%.2f", fuelPrice),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkCharcoal,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = { if (fuelPrice < 5.00f) fuelPrice = (fuelPrice + 0.05f).coerceAtMost(5.00f) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase Fuel Price", tint = DeepTeal)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Average Consumption Controller
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Car Fuel Efficiency (L/100km):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = DarkCharcoal
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (fuelConsumption > 3.0f) fuelConsumption = (fuelConsumption - 0.5f).coerceAtLeast(3.0f) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease Efficiency", tint = DeepTeal)
                }
                Text(
                    text = String.format("%.1f L", fuelConsumption),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkCharcoal,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = { if (fuelConsumption < 25.0f) fuelConsumption = (fuelConsumption + 0.5f).coerceAtMost(25.0f) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase Efficiency", tint = DeepTeal)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Riders Split Count Controller
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Total Split Count:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkCharcoal
                )
                Text(
                    text = "Including driver & riders",
                    fontSize = 10.sp,
                    color = MutedText
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (splitCount > 1) splitCount-- },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease Split Count", tint = DeepTeal)
                }
                Text(
                    text = "$splitCount people",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkCharcoal,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = { if (splitCount < 10) splitCount++ },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase Split Count", tint = DeepTeal)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Round Trip Return Toggle Option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .clickable { isRoundTrip = !isRoundTrip }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isRoundTrip,
                onCheckedChange = { isRoundTrip = it },
                colors = CheckboxDefaults.colors(checkedColor = DeepTeal)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "Calculate Round Trip (Return)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkCharcoal
                )
                Text(
                    text = "Multiplies distance by 2 for Suva return commute",
                    fontSize = 10.sp,
                    color = MutedText
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Final suggestions panel
        Surface(
            color = GoldenHighlight.copy(alpha = 0.95f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Fuel Needed:",
                        fontSize = 12.sp,
                        color = DarkCharcoal
                    )
                    Text(
                        text = String.format("%.2f L", totalFuelLitres),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkCharcoal
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Gas Cost:",
                        fontSize = 12.sp,
                        color = DarkCharcoal
                    )
                    Text(
                        text = String.format("FJD $%.2f", totalCost),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkCharcoal
                    )
                }

                HorizontalDivider(
                    color = DarkCharcoal.copy(alpha = 0.15f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Suggested per-rider share:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepTeal
                        )
                        Text(
                            text = "Equal splitting among all in car",
                            fontSize = 9.sp,
                            color = DarkCharcoal.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = String.format("FJD $%.2f", sharePerRider),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = DeepTeal
                    )
                }
            }
        }
    }
}

// MY MATCHES SCREEN
@Composable
fun MatchesScreen(viewModel: CarpoolViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val userMatches by viewModel.userMatches.collectAsState()
    val usersMap by viewModel.usersMap.collectAsState()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSand)
            .padding(16.dp)
    ) {
        Text(
            text = "My Commute Matches",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = DeepTeal
        )
        Text(
            text = "List of current recurring direct ride contracts matches generated in system.",
            fontSize = 14.sp,
            color = MutedText,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (userMatches.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MutedText.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No commute matches found in system yet.\nComplete commute profile to auto-match!",
                        color = MutedText,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(userMatches) { match ->
                    val isDriver = match.driverUserId == currentUser?.id
                    val otherUserId = if (isDriver) match.riderUserId else match.driverUserId
                    val otherUser = usersMap[otherUserId]
                    val otherUserName = otherUser?.fullName ?: "Verified Member"
                    val otherPhone = otherUser?.phoneWhatsapp ?: ""
                    val otherGender = otherUser?.gender ?: ""
                    val org = otherUser?.organisation ?: ""

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, DeepTeal.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Header row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = DeepTeal.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = if (isDriver) "Driving" else "Riding",
                                            color = DeepTeal,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = match.matchDate,
                                        fontSize = 12.sp,
                                        color = MutedText
                                    )
                                }

                                Surface(
                                    color = when (match.status) {
                                        "Confirmed" -> Color(0xFFE3F2FD)
                                        "Completed" -> Color(0xFFE8F5E9)
                                        else -> Color(0xFFFFECEF)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = match.status,
                                        fontSize = 11.sp,
                                        color = when (match.status) {
                                            "Confirmed" -> Color(0xFF1565C0)
                                            "Completed" -> Color(0xFF2E7D32)
                                            else -> CoralAccent
                                        },
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Name
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = otherUserName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkCharcoal
                                )
                                if (otherGender == "Female") {
                                    Text(" 🌸", fontSize = 14.sp)
                                }
                            }
                            Text(
                                text = "Organisation: $org",
                                fontSize = 12.sp,
                                color = MutedText
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Route & Duration info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("COMMUTE ROUTE", fontSize = 10.sp, color = MutedText)
                                    Text(match.routeDescription, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("DEPARTURE TIME", fontSize = 10.sp, color = MutedText)
                                    Text(match.departureTime, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action button WhatsApp always opens chat link
                            Button(
                                onClick = { openWhatsAppLink(context, otherPhone) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chat via WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            var showCalculator by remember { mutableStateOf(false) }

                            OutlinedButton(
                                onClick = { showCalculator = !showCalculator },
                                border = BorderStroke(1.dp, DeepTeal.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepTeal)
                            ) {
                                Icon(
                                    imageVector = if (showCalculator) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (showCalculator) "Hide Fuel Calculator" else "Calculate Fijian Fuel-Share",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            AnimatedVisibility(
                                visible = showCalculator,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                             ) {
                                 Column {
                                     Spacer(modifier = Modifier.height(12.dp))
                                     FijianFuelShareCalculator(
                                         initialRouteDesc = match.routeDescription
                                     )
                                 }
                             }
                        }
                    }
                }
            }
        }
    }
}

// REQUEST A ONE-OFF RIDE  
@Composable
fun OneOffRequestScreen(viewModel: CarpoolViewModel) {
    val myRequests by viewModel.myRequests.collectAsState()
    val context = LocalContext.current

    var dateNeeded by remember { mutableStateOf("2026-05-25") }
    var pickupSuburb by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var preferredTime by remember { mutableStateOf("08:00") }
    var notes by remember { mutableStateOf("") }

    var isAddingRequest by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSand)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "One-Off Commutes",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = DeepTeal
                    )
                    Text(
                        text = "Need an irregular immediate lift? Create single date requests.",
                        fontSize = 13.sp,
                        color = MutedText
                    )
                }
                
                Button(
                    onClick = { isAddingRequest = !isAddingRequest },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isAddingRequest) DeepTeal else CoralAccent),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (isAddingRequest) "View My List" else "Apply New", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (isAddingRequest) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, DeepTeal.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("One-Off Request Intake Form", fontWeight = FontWeight.Bold, color = DeepTeal, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Date Needed Click Picker
                        Button(
                            onClick = {
                                showDatePicker(context) { dateNeeded = it }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftSand),
                            border = BorderStroke(1.dp, MutedText.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("oneoff_date_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp), tint = DeepTeal)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Date Needed: $dateNeeded", fontWeight = FontWeight.Bold, color = DarkCharcoal)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = pickupSuburb,
                            onValueChange = { pickupSuburb = it },
                            label = { Text("Pickup Suburb") },
                            placeholder = { Text("e.g. Lami, Nakasi, Nakasi, Tamavua") },
                            modifier = Modifier.fillMaxWidth().testTag("oneoff_pickup_input"),
                            singleLine = true,
                            trailingIcon = {
                                LocationSelectorTrailingIcon(
                                    onLocationFound = { pickupSuburb = it },
                                    isWorkOrDest = false
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = destination,
                            onValueChange = { destination = it },
                            label = { Text("Destination Location") },
                            placeholder = { Text("e.g. USP, CWM, Govnet building") },
                            modifier = Modifier.fillMaxWidth().testTag("oneoff_dest_input"),
                            singleLine = true,
                            trailingIcon = {
                                LocationSelectorTrailingIcon(
                                    onLocationFound = { destination = it },
                                    isWorkOrDest = true
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Time Picker
                        Button(
                            onClick = {
                                showTimePicker(context, preferredTime) { preferredTime = it }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftSand),
                            border = BorderStroke(1.dp, MutedText.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("oneoff_time_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp), tint = DeepTeal)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Preferred Arrival Time: $preferredTime", fontWeight = FontWeight.Bold, color = DarkCharcoal)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Optional Notes / Preferences") },
                            placeholder = { Text("e.g. flexible, carrying small luggage") },
                            modifier = Modifier.fillMaxWidth().testTag("oneoff_notes_input")
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.createOneOffRequest(
                                    dateNeeded = dateNeeded,
                                    pickupSuburb = pickupSuburb,
                                    destination = destination,
                                    preferredTime = preferredTime,
                                    notes = notes,
                                    onSuccess = {
                                        isAddingRequest = false
                                        pickupSuburb = ""
                                        destination = ""
                                        notes = ""
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralAccent),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("oneoff_submit_btn"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Submit Custom Request", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            if (myRequests.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(36.dp), tint = MutedText)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No pending one-off ride requested yet.", color = MutedText, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(myRequests) { request ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, DeepTeal.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = DeepTeal)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(request.dateNeeded, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                
                                Surface(
                                    color = if (request.isFulfilled) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (request.isFulfilled) "Fulfilled" else "Pending Lift",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (request.isFulfilled) Color(0xFF2E7D32) else Color(0xFFE65100),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Route: ${request.pickupSuburb} → ${request.destination}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Time Preferred: ${request.preferredTime}",
                                fontSize = 12.sp,
                                color = MutedText
                            )
                            if (request.notes.isNotEmpty()) {
                                Text(
                                    text = "Notes: \"${request.notes}\"",
                                    fontSize = 12.sp,
                                    color = MutedText,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// OPEN RIDE REQUESTS (VISIBLE TO DRIVERS WITHIN THE SAME ORGANISATION ONLY)
@Composable
fun OpenRequestsScreen(viewModel: CarpoolViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val openRequests by viewModel.openRequests.collectAsState()
    val usersMap by viewModel.usersMap.collectAsState()

    val context = LocalContext.current

    // "A list of all pending one-off requests from other users in the same organisation, showing: Requester's name, pickup suburb, destination, date, time, and notes"
    val filteredRequests = openRequests.filter { request ->
        val requester = usersMap[request.requesterUserId]
        // Filter out my own, must match same organisation
        requester != null && request.requesterUserId != currentUser?.id && requester.organisation == currentUser?.organisation
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSand)
            .padding(16.dp)
    ) {
        Text(
            text = "Open Cohort Lift Requests",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = DeepTeal
        )
        Text(
            text = "One-off commuter lift requests issued by fellow ${currentUser?.organisation} members.",
            fontSize = 13.sp,
            color = MutedText,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (filteredRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Commute, contentDescription = null, modifier = Modifier.size(50.dp), tint = MutedText)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No open requests in ${currentUser?.organisation}\nCheck back late!",
                        color = MutedText,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredRequests) { request ->
                    val requester = usersMap[request.requesterUserId]
                    val requesterName = requester?.fullName ?: "Verified Member"
                    val requesterPhone = requester?.phoneWhatsapp ?: ""
                    val requesterGender = requester?.gender ?: ""

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, DeepTeal.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Header matching
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = requesterName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = DarkCharcoal
                                    )
                                    if (requesterGender == "Female") {
                                        Text(" 🌸", fontSize = 14.sp, modifier = Modifier.testTag("women_only_badge"))
                                    }
                                }

                                Surface(
                                    color = Color(0xFFFFECEF),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "OPEN",
                                        fontWeight = FontWeight.Bold,
                                        color = CoralAccent,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("PICKUP", fontSize = 10.sp, color = MutedText)
                                    Text(request.pickupSuburb, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("DESTINATION", fontSize = 10.sp, color = MutedText)
                                    Text(request.destination, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("DATE", fontSize = 10.sp, color = MutedText)
                                    Text(request.dateNeeded, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("PREFERRED TIME", fontSize = 10.sp, color = MutedText)
                                    Text(request.preferredTime, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            if (request.notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Notes: \"${request.notes}\"",
                                    fontSize = 12.sp,
                                    color = MutedText,
                                    style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.fulfillOneOffRequest(request.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Fulfill Lift", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { openWhatsAppLink(context, requesterPhone) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Chat WhatsApp", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ADMIN DASHBOARD SCREEN (FOR admin@veiliu.com ONLY)
@Composable
fun AdminScreen(viewModel: CarpoolViewModel) {
    val stats by viewModel.stats.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val allMatches by viewModel.allMatches.collectAsState()
    val usersMap by viewModel.usersMap.collectAsState()

    var statusToChangeId by remember { mutableStateOf<Int?>(null) }
    var showDropdownForMatchId by remember { mutableStateOf<Int?>(null) }

    // Group users by Organisation for breakdown list
    val orgBreakdown = allUsers.groupBy { it.organisation }.mapValues { it.value.size }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSand)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Platform Master Admin Panel",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = DeepTeal
            )
            Text(
                text = "Live analytics, institutional cohort metrics, and direct carpool manual sync states override.",
                fontSize = 13.sp,
                color = MutedText
            )
        }

        // Stats card breakdown list
        item {
            Text(text = "System Statistics", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepTeal)
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, DeepTeal.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${stats.totalUsers}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = DeepTeal)
                            Text("Total Users", fontSize = 11.sp, color = MutedText)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${stats.activeProfiles}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = DeepTeal)
                            Text("Active Profiles", fontSize = 11.sp, color = MutedText)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${stats.totalMatches}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = DeepTeal)
                            Text("Total Matches", fontSize = 11.sp, color = MutedText)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${stats.oneOffRequests}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = DeepTeal)
                            Text("One-Offs", fontSize = 11.sp, color = MutedText)
                        }
                    }
                }
            }
        }

        // Organization Breakdown table
        item {
            Text(text = "Member Breakdown by Corporate Entity", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepTeal)
            Spacer(modifier = Modifier.height(4.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, DeepTeal.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    orgBreakdown.forEach { (org, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(CoralAccent)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(org, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text("$count profiles", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepTeal)
                        }
                    }
                }
            }
        }

        // Full Ride Matches Table List with Status Overrides
        item {
            Text(text = "Platform Ride Matches Master Registry", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepTeal)
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (allMatches.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, DeepTeal.copy(alpha = 0.05f))
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No ride matches made globally yet.", color = MutedText)
                    }
                }
            }
        } else {
            items(allMatches) { match ->
                val dUser = usersMap[match.driverUserId]
                val rUser = usersMap[match.riderUserId]
                val dName = dUser?.fullName ?: "Unknown Driver"
                val rName = rUser?.fullName ?: "Unknown Rider"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, DeepTeal.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Match Date: ${match.matchDate}", fontSize = 12.sp, color = MutedText)
                            
                            Surface(
                                color = when (match.status) {
                                    "Confirmed" -> Color(0xFFE3F2FD)
                                    "Completed" -> Color(0xFFE8F5E9)
                                    else -> Color(0xFFFFECEF)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    showDropdownForMatchId = if (showDropdownForMatchId == match.id) null else match.id
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = match.status,
                                        fontSize = 11.sp,
                                        color = when (match.status) {
                                            "Confirmed" -> Color(0xFF1565C0)
                                            "Completed" -> Color(0xFF2E7D32)
                                            else -> CoralAccent
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(12.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Match parties Flow Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = dName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "paired with", modifier = Modifier.size(14.dp), tint = CoralAccent)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = rName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Route: ${match.routeDescription}", fontSize = 12.sp)
                        Text(text = "Departure: ${match.departureTime}", fontSize = 12.sp, color = MutedText)

                        // If Dropdown clicked, show manual status override actions
                        if (showDropdownForMatchId == match.id) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.updateMatchStatus(match.id, "Confirmed")
                                        showDropdownForMatchId = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                                    modifier = Modifier.weight(1.0f).height(38.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Confirm", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        viewModel.updateMatchStatus(match.id, "Completed")
                                        showDropdownForMatchId = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CoralAccent),
                                    modifier = Modifier.weight(1.0f).height(38.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Complete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun LocationSelectorTrailingIcon(
    onLocationFound: (String) -> Unit,
    isWorkOrDest: Boolean
) {
    var isLocating by remember { mutableStateOf(false) }
    
    if (isLocating) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = DeepTeal
        )
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1000)
            val locations = if (isWorkOrDest) {
                listOf(
                    "USP Laucala Campus, Suva",
                    "MHCC Downtown, Suva",
                    "CWM Hospital, Suva",
                    "Fiji Government Buildings",
                    "Nausori Town Bus Terminal"
                )
            } else {
                listOf(
                    "Nakasi, Nausori Corridor",
                    "Lami Town",
                    "Valelevu, Nasinu",
                    "Flagstaff, Suva",
                    "Namaka, Nadi"
                )
            }
            onLocationFound(locations.random())
            isLocating = false
        }
    } else {
        IconButton(
            onClick = { isLocating = true },
            modifier = Modifier.size(24.dp).testTag(if (isWorkOrDest) "gps_dest_btn" else "gps_suburb_btn")
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Get GPS Location",
                tint = DeepTeal,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SplashScreen(
    viewModel: CarpoolViewModel,
    modifier: Modifier = Modifier
) {
    val scale = remember { androidx.compose.animation.core.Animatable(0.7f) }
    val opacity = remember { androidx.compose.animation.core.Animatable(0f) }
    
    LaunchedEffect(Unit) {
        this.launch {
            scale.animateTo(
                targetValue = 1.0f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 1000,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            )
        }
        this.launch {
            opacity.animateTo(
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 850)
            )
        }
        
        kotlinx.coroutines.delay(2200)
        
        if (viewModel.currentUser.value != null) {
            viewModel.navigateTo("Home")
        } else {
            viewModel.navigateTo("Onboarding")
        }
    }
    
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.3f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepTeal),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = CoralAccent.copy(alpha = 0.04f),
                radius = size.minDimension * 0.7f,
                center = androidx.compose.ui.geometry.Offset(0f, 0f)
            )
            drawCircle(
                color = LightSand.copy(alpha = 0.03f),
                radius = size.minDimension * 0.5f,
                center = androidx.compose.ui.geometry.Offset(size.width, size.height)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                alpha = opacity.value
            }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer {
                            scaleX = pulse
                            scaleY = pulse
                            alpha = ringAlpha
                        }
                        .clip(RoundedCornerShape(50.dp))
                        .background(CoralAccent)
                )

                Surface(
                    modifier = Modifier
                        .size(100.dp)
                        .border(2.dp, LightSand, RoundedCornerShape(32.dp)),
                    color = CoralAccent,
                    shape = RoundedCornerShape(32.dp),
                    shadowElevation = 8.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Logo icon",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Veiliu Carpool",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Fiji's Safe Trans-Corridor Ride Share",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = LightSand.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Na Bisini • Na Sota • Na Tiko Cegu",
                fontSize = 11.sp,
                color = GoldenHighlight,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Working together, connecting our community safely",
                fontSize = 10.sp,
                color = LightSand.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun OnboardingScreen(
    viewModel: CarpoolViewModel,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableStateOf(0) }
    var selectedRoleOnboarding by remember { mutableStateOf("Both") }

    val totalPages = 3

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LightSand)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "VEILIU CARPOOL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = DeepTeal
            )
            if (currentPage < totalPages - 1) {
                Text(
                    text = "Skip",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoralAccent,
                    modifier = Modifier.clickable { currentPage = totalPages - 1 }
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                (slideInHorizontally { width -> width } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
            },
            modifier = Modifier.weight(0.8f),
            label = "onboarding_animation"
        ) { pageIndex ->
            when (pageIndex) {
                0 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            modifier = Modifier.size(150.dp),
                            shape = RoundedCornerShape(40.dp),
                            color = DeepTeal.copy(alpha = 0.08f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = DeepTeal,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Solve Fiji's Corridor Commute",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepTeal,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Perfect for Suva-Nausori daily peaks, Lami shortcuts, or longer Pacific Harbour drives. Share spaces, save hours of waiting in bus stands, and travel under secure air-conditioned comfort.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MutedText,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
                1 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            modifier = Modifier.size(150.dp),
                            shape = RoundedCornerShape(40.dp),
                            color = GoldenHighlight.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = DeepTeal,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Transparent Fuel Sharing",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepTeal,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Skip the awkward contribution negotiations! Our localized Fiji fuel price and vehicle efficiency calculator provides clear split recommendations instantly within matching views.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MutedText,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
                2 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            modifier = Modifier.size(110.dp),
                            shape = RoundedCornerShape(40.dp),
                            color = SoftPink
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = CoralAccent,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Choose Your Commute Role",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepTeal,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Are you seeking regular rides, or driving and offering seats on your daily corridor route?",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MutedText,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Rider" to "I am a Rider",
                                "Driver" to "I am a Driver",
                                "Both" to "I am Both"
                            ).forEach { (role, label) ->
                                val isSelected = selectedRoleOnboarding == role
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedRoleOnboarding = role }
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) DeepTeal else DeepTeal.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) SoftPink else Color.White
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = when(role) {
                                                "Rider" -> Icons.Default.Person
                                                "Driver" -> Icons.Default.DirectionsCar
                                                else -> Icons.Default.Group
                                            },
                                            contentDescription = null,
                                            tint = if (isSelected) DeepTeal else MutedText,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) DeepTeal else DarkCharcoal,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(totalPages) { pageIndex ->
                    val isSelected = currentPage == pageIndex
                    Box(
                        modifier = Modifier
                            .size(height = 6.dp, width = if (isSelected) 20.dp else 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isSelected) DeepTeal else DeepTeal.copy(alpha = 0.15f))
                    )
                }
            }

            Button(
                onClick = {
                    if (currentPage < totalPages - 1) {
                        currentPage++
                    } else {
                        viewModel.navigateTo("Auth")
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
            ) {
                if (currentPage < totalPages - 1) {
                    Text("Next", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("Get Started", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}
