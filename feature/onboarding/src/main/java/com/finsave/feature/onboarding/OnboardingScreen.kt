package com.finsave.feature.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SyncLock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.airbnb.lottie.compose.*
import com.finsave.core.ui.theme.LocalSpacing
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // State for permissions
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasSmsPermission = permissions[Manifest.permission.READ_SMS] == true
        coroutineScope.launch { pagerState.animateScrollToPage(3) }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onComplete()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // Only show pager indicator if not on the first screen
            AnimatedVisibility(
                visible = pagerState.currentPage > 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.large),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(5) { iteration ->
                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            userScrollEnabled = false // Prevent manual swiping to enforce flow
        ) { page ->
            when (page) {
                0 -> PrivacyPromiseScreen(
                    onNext = { coroutineScope.launch { pagerState.animateScrollToPage(1) } }
                )
                1 -> HowItWorksScreen(
                    onNext = { coroutineScope.launch { pagerState.animateScrollToPage(2) } }
                )
                2 -> SmsPermissionScreen(
                    onGrantClick = {
                        smsPermissionLauncher.launch(
                            arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                        )
                    },
                    onDenyClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(3) }
                    }
                )
                3 -> RegionCurrencyScreen(
                    onNext = { coroutineScope.launch { pagerState.animateScrollToPage(4) } }
                )
                4 -> NotificationPermissionScreen(
                    onCompleteClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onComplete()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PrivacyPromiseScreen(onNext: () -> Unit) {
    val spacing = LocalSpacing.current
    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.privacy_shield))
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition,
        iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LottieAnimation(
            composition = lottieComposition,
            progress = { lottieProgress },
            modifier = Modifier.size(200.dp)
        )
        
        Spacer(modifier = Modifier.height(spacing.large))
        
        // Load string by ID manually instead of R.string (for standalone module compiling simplicity)
        Text(
            text = "Your money. Your secret.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(spacing.small))
        
        Text(
            text = "FinSave works entirely on your phone.\nWe never upload your data to any server.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(spacing.huge))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Continue to FinSave", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun HowItWorksScreen(onNext: () -> Unit) {
    val spacing = LocalSpacing.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SyncLock,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(spacing.large))
        Text(
            text = "Zero Manual Entry",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(spacing.small))
        Text(
            text = "FinSave reads transaction SMS from your bank to automatically track expenses.\nNo bank login required.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(spacing.huge))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("See how it works", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SmsPermissionScreen(onGrantClick: () -> Unit, onDenyClick: () -> Unit) {
    val spacing = LocalSpacing.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Sms,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(spacing.large))
        Text(
            text = "Allow SMS Access",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(spacing.small))
        Text(
            text = "We need SMS permission to auto-import your bank transactions. Your messages never leave this device.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(spacing.huge))
        Button(
            onClick = onGrantClick,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Grant Permission", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(spacing.small))
        TextButton(onClick = onDenyClick) {
            Text("Not Now (Manual Mode)", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RegionCurrencyScreen(onNext: () -> Unit) {
    val spacing = LocalSpacing.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(spacing.large))
        Text(
            text = "Setup your region",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(spacing.small))
        Text(
            text = "FinSave defaults to India (INR). You can change this later in settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(spacing.huge))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Set to India (INR)", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun NotificationPermissionScreen(onCompleteClick: () -> Unit) {
    val spacing = LocalSpacing.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(spacing.large))
        Text(
            text = "Stay on track",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(spacing.small))
        Text(
            text = "Enable notifications for the nightly 10 PM budget digest and expense alerts.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(spacing.huge))
        Button(
            onClick = onCompleteClick,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Start Saving", style = MaterialTheme.typography.titleMedium)
        }
    }
}
