package com.finsave.feature.transactions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.finsave.core.ui.theme.LocalSpacing
import com.finsave.domain.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    transaction: com.finsave.domain.model.Transaction? = null,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    val amountPaise by viewModel.amountPaise.collectAsState()
    val transactionType by viewModel.transactionType.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val merchantName by viewModel.merchantName.collectAsState()
    val merchantSuggestions by viewModel.merchantSuggestions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()

    var amountString by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

    fun startVoiceListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Voice input is not available on this device.", Toast.LENGTH_SHORT).show()
            return
        }

        speechRecognizer?.destroy()
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                isListening = false
                Toast.makeText(context, "Could not understand. Please try again.", Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val recognizedText = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()

                if (recognizedText.isBlank()) {
                    Toast.makeText(context, "Could not understand. Please try again.", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.processVoiceInput(recognizedText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        try {
            recognizer.startListening(recognizerIntent)
            isListening = true
        } catch (e: SecurityException) {
            isListening = false
            Toast.makeText(context, "Microphone permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceListening()
        } else {
            Toast.makeText(context, "Microphone permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    // Initialize with transaction data if in edit mode
    LaunchedEffect(transaction) {
        if (transaction != null) {
            viewModel.initializeWithTransaction(transaction)
            // Convert paise to rupees string for display
            val rupees = transaction.amountPaise / 100.0
            amountString = if (rupees % 1.0 == 0.0) {
                rupees.toInt().toString()
            } else {
                String.format(Locale.ROOT, "%.2f", rupees)
            }
        }
    }

    LaunchedEffect(amountPaise) {
        if (amountStringToPaise(amountString) != amountPaise) {
            amountString = formatPaiseForInput(amountPaise)
        }
    }

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is AddTransactionViewModel.UiEvent.Success -> onDismissRequest()
                is AddTransactionViewModel.UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        val spacing = LocalSpacing.current
        val scrollState = rememberScrollState()

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.large)
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
            ) {
                // Header: Type Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.medium),
                    horizontalArrangement = Arrangement.Center
                ) {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = transactionType == TransactionType.DEBIT,
                            onClick = { viewModel.onTypeChange(TransactionType.DEBIT) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Expense")
                        }
                        SegmentedButton(
                            selected = transactionType == TransactionType.CREDIT,
                            onClick = { viewModel.onTypeChange(TransactionType.CREDIT) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Income")
                        }
                    }
                }

                // Amount Input — opens system decimal keyboard
                val amountColor = if (transactionType == TransactionType.DEBIT)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary

                OutlinedTextField(
                    value = amountString,
                    onValueChange = { raw ->
                        // Allow only digits + single decimal point, max 2 decimal places
                        val filtered = raw.filter { it.isDigit() || it == '.' }
                        val parts = filtered.split('.')
                        if (parts.size <= 2) {
                            val intPart = parts[0].take(9).trimStart('0').ifEmpty { if (parts.size == 2) "0" else "" }
                            val validated = if (parts.size == 2) "$intPart.${parts[1].take(2)}" else intPart
                            amountString = validated
                            viewModel.onAmountChange(validated)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.displaySmall.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    ),
                    placeholder = {
                        Text(
                            "0",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    },
                    prefix = {
                        Text(
                            "₹",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = amountColor
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = amountColor,
                        unfocusedBorderColor = amountColor.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.height(spacing.medium))

                // Merchant Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    OutlinedTextField(
                        value = merchantName,
                        onValueChange = viewModel::onMerchantChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Merchant or Note") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    IconButton(
                        onClick = {
                            if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                startVoiceListening()
                            } else {
                                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                if (isListening) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice quick add",
                            tint = if (isListening) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isListening) {
                    Spacer(modifier = Modifier.height(spacing.small))
                    VoiceListeningIndicator()
                }

                if (merchantSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(spacing.small))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                        items(merchantSuggestions) { suggestion ->
                            SuggestionChip(
                                onClick = { viewModel.onMerchantSuggestionSelect(suggestion) },
                                label = { Text(suggestion, maxLines = 1) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.medium))

                DateSelectorRow(
                    selectedDate = selectedDate,
                    dateFormatter = dateFormatter,
                    onOpenDatePicker = { showDatePicker = true },
                    onTodayClick = { viewModel.onDateSelect(LocalDate.now(ZoneId.of("Asia/Kolkata"))) },
                    onYesterdayClick = { viewModel.onDateSelect(LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(1)) }
                )

                Spacer(modifier = Modifier.height(spacing.medium))

                AccountSelectorRow(
                    accounts = accounts,
                    selectedAccountId = selectedAccountId,
                    onAccountSelect = viewModel::onAccountSelect
                )

                if (accounts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(spacing.medium))
                }

                // Category Chips
                Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(spacing.small))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { viewModel.onCategorySelect(category.id) },
                            label = { Text(category.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing.medium))

                // Delete button in edit mode
                if (isEditMode) {
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = spacing.medium),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Delete Transaction", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Save / Update button
                Button(
                    onClick = { viewModel.saveTransaction() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (isEditMode) "Update" else "Save Transaction",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(spacing.large))
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Transaction") },
                text = { Text("Are you sure you want to delete this transaction? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteTransaction()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate.toEpochDay() * 86400000L
            )

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val millis = datePickerState.selectedDateMillis ?: return@TextButton
                            val date = LocalDate.ofInstant(
                                Instant.ofEpochMilli(millis),
                                ZoneId.of("Asia/Kolkata")
                            )
                            viewModel.onDateSelect(date)
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@Composable
private fun DateSelectorRow(
    selectedDate: LocalDate,
    dateFormatter: DateTimeFormatter,
    onOpenDatePicker: () -> Unit,
    onTodayClick: () -> Unit,
    onYesterdayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .clickable { onOpenDatePicker() }
                .padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Select transaction date",
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = selectedDate.format(dateFormatter),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
            AssistChip(
                onClick = onTodayClick,
                label = { Text("Today") }
            )
            AssistChip(
                onClick = onYesterdayClick,
                label = { Text("Yesterday") }
            )
        }
    }
}

@Composable
private fun AccountSelectorRow(
    accounts: List<com.finsave.domain.model.Account>,
    selectedAccountId: Long?,
    onAccountSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (accounts.isEmpty()) return

    val spacing = LocalSpacing.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        Text(
            text = "Account",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
            items(accounts, key = { it.id }) { account ->
                FilterChip(
                    selected = selectedAccountId == account.id,
                    onClick = { onAccountSelect(account.id) },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(accountColor(account.colorHex))
                            )
                            Text(account.name)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun accountColor(colorHex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }
}


private fun amountStringToPaise(value: String): Long {
    if (value.isBlank() || value == ".") return 0L
    return try {
        BigDecimal(value.trimEnd('.'))
            .multiply(BigDecimal("100"))
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    } catch (e: NumberFormatException) {
        0L
    } catch (e: ArithmeticException) {
        0L
    }
}

private fun formatPaiseForInput(paise: Long): String {
    if (paise <= 0L) return ""
    val rupees = paise / 100
    val remainingPaise = paise % 100
    return if (remainingPaise == 0L) {
        rupees.toString()
    } else {
        "$rupees.${String.format(Locale.ROOT, "%02d", remainingPaise)}"
    }
}

@Composable
private fun VoiceListeningIndicator() {
    val pulse by rememberInfiniteTransition().animateFloat(
        initialValue = 0.75f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = "Listening...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

