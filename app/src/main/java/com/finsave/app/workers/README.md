# FinSave Workers

This package contains WorkManager workers for background tasks in FinSave.

## BudgetCheckWorker

**Purpose:** Evaluates budget thresholds (80% and 90%) after each transaction insert and posts notifications when thresholds are crossed.

**How it works:**
1. Queries all active budgets from `BudgetRepository`
2. For each budget, computes the current period range using `Budget.currentPeriodRange()`
3. Calculates total DEBIT spending in that period
4. Checks if spending crosses 80% or 90% thresholds
5. Posts notifications via `NotificationHelper.postBudgetAlert()` if threshold crossed
6. Uses `PreferencesManager` with keys scoped by budget ID, threshold, and period start to prevent duplicate notifications

**Usage in ViewModels:**

To trigger budget checks after a transaction insert, inject `BudgetCheckScheduler` and call `scheduleBudgetCheck()`:

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val budgetCheckScheduler: BudgetCheckScheduler
) : ViewModel() {

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val result = addTransactionUseCase(transaction)
            if (result.isSuccess) {
                // Enqueue budget check worker
                budgetCheckScheduler.scheduleBudgetCheck()
            }
        }
    }
}
```

**Requirements satisfied:**
- Requirement 8.1: 80% threshold notification
- Requirement 8.2: 90% threshold notification
- Requirement 8.3: At most one notification per threshold per period
- Requirement 8.4: Reset flags when new period begins (automatic via date-scoped keys)
- Requirement 8.5: Graceful handling of missing notification permission
- Requirement 8.6: Enqueued as OneTimeWorkRequest with tag WORK_BUDGET_CHECK

## BudgetCheckScheduler

**Purpose:** Helper class to enqueue `BudgetCheckWorker` from ViewModels.

**Usage:** Inject into any ViewModel that handles transaction creation/updates and call `scheduleBudgetCheck()` after successful transaction insert.

```kotlin
@Singleton
class BudgetCheckScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scheduleBudgetCheck() {
        val workRequest = OneTimeWorkRequestBuilder<BudgetCheckWorker>()
            .addTag(Constants.WORK_BUDGET_CHECK)
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
```

## Notes

- The worker is configured as a `@HiltWorker` and uses Hilt dependency injection
- No manifest registration is required for Hilt workers
- The `FinSaveApplication` class already configures `HiltWorkerFactory` for WorkManager
- Notification channels are created in `FinSaveApplication.onCreate()` via `NotificationHelper.createChannels()`
