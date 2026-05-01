package com.finsave.feature.onboarding

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.airbnb.lottie.compose.*
import com.finsave.core.common.startup.AppLaunchCoordinator
import com.finsave.core.ui.theme.LocalSpacing
import com.finsave.domain.usecase.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appLaunchCoordinator: AppLaunchCoordinator,
    private val syncManager: SyncManager
) : ViewModel() {
    fun completeOnboarding() {
        appLaunchCoordinator.markOnboardingComplete()
        // Run an immediate initial import so first Dashboard load shows data
        // instead of waiting for periodic WorkManager cadence.
        syncManager.triggerManualSync(0L)
    }
}

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var hasRequestedSmsPermission by rememberSaveable { mutableStateOf(false) }
    var showOpenSettingsDialog by rememberSaveable { mutableStateOf(false) }
    val activity = context.findActivity()

    // State for permissions
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasRequestedSmsPermission = true
        val readGranted = permissions[Manifest.permission.READ_SMS] == true
        val receiveGranted = permissions[Manifest.permission.RECEIVE_SMS] == true
        hasSmsPermission = readGranted && receiveGranted

        if (hasSmsPermission) {
            coroutineScope.launch { pagerState.animateScrollToPage(3) }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("SMS permission is required to continue.")
            }
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.completeOnboarding()
        onComplete()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
        LaunchedEffect(pagerState.currentPage, hasSmsPermission) {
            if (pagerState.currentPage > 2 && !hasSmsPermission) {
                pagerState.animateScrollToPage(2)
                snackbarHostState.showSnackbar("Grant SMS permission before continuing.")
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            userScrollEnabled = pagerState.currentPage < 2 || hasSmsPermission
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
                        if (hasSmsPermission) {
                            coroutineScope.launch { pagerState.animateScrollToPage(3) }
                            return@SmsPermissionScreen
                        }

                        val isReadPermanentlyDenied = hasRequestedSmsPermission &&
                            activity != null &&
                            !ActivityCompat.shouldShowRequestPermissionRationale(
                                activity,
                                Manifest.permission.READ_SMS
                            )
                        val isReceivePermanentlyDenied = hasRequestedSmsPermission &&
                            activity != null &&
                            !ActivityCompat.shouldShowRequestPermissionRationale(
                                activity,
                                Manifest.permission.RECEIVE_SMS
                            )

                        if (isReadPermanentlyDenied || isReceivePermanentlyDenied) {
                            showOpenSettingsDialog = true
                        } else {
                            smsPermissionLauncher.launch(
                                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                            )
                        }
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
                            viewModel.completeOnboarding()
                            onComplete()
                        }
                    }
                )
            }
        }
    }

    if (showOpenSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showOpenSettingsDialog = false },
            title = { Text("Enable SMS Permission") },
            text = {
                Text(
                    "Android is not showing the permission prompt anymore. " +
                        "Please enable SMS permission from App Settings to continue."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOpenSettingsDialog = false
                        context.openAppSettings()
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
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
private fun SmsPermissionScreen(onGrantClick: () -> Unit) {
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
            text = "SMS permission is required to auto-import your bank transactions. Your messages never leave this device.",
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
