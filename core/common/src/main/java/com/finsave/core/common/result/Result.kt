package com.finsave.core.common.result

/**
 * Sealed interface representing the result of any operation.
 * Used throughout FinSave to handle success/error/loading states
 * in a type-safe, exhaustive-when-checked manner.
 *
 * Usage:
 * ```
 * when (result) {
 *     is Result.Success -> showData(result.data)
 *     is Result.Error -> showError(result.message)
 *     is Result.Loading -> showLoader()
 * }
 * ```
 */
sealed interface Result<out T> {

    /**
     * Operation completed successfully with data.
     */
    data class Success<T>(val data: T) : Result<T>

    /**
     * Operation failed with an error message and optional exception.
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : Result<Nothing>

    /**
     * Operation is in progress.
     */
    data object Loading : Result<Nothing>
}

// ── Extension Functions ────────────────────────────────────────────────────

/**
 * Returns the data if this is a [Result.Success], or null otherwise.
 */
fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    else -> null
}

/**
 * Returns the data if this is a [Result.Success], or the [defaultValue] otherwise.
 */
fun <T> Result<T>.getOrDefault(defaultValue: T): T = when (this) {
    is Result.Success -> data
    else -> defaultValue
}

/**
 * Maps the data of a [Result.Success] using the provided [transform] function.
 * [Result.Error] and [Result.Loading] pass through unchanged.
 */
fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    is Result.Loading -> this
}

/**
 * Executes [action] if this is a [Result.Success].
 * Returns the original result for chaining.
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

/**
 * Executes [action] if this is a [Result.Error].
 * Returns the original result for chaining.
 */
inline fun <T> Result<T>.onError(action: (String, Throwable?) -> Unit): Result<T> {
    if (this is Result.Error) action(message, exception)
    return this
}

/**
 * Returns true if this result is [Result.Success].
 */
val <T> Result<T>.isSuccess: Boolean
    get() = this is Result.Success

/**
 * Returns true if this result is [Result.Error].
 */
val <T> Result<T>.isError: Boolean
    get() = this is Result.Error

/**
 * Returns true if this result is [Result.Loading].
 */
val <T> Result<T>.isLoading: Boolean
    get() = this is Result.Loading
