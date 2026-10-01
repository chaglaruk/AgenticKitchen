package com.agentickitchen.shared.ai

sealed interface AiResult<out T> {
    data class Success<T>(
        val value: T,
        val provider: AiProviderId,
        val model: String
    ) : AiResult<T>

    data class Failure(
        val type: AiFailureType,
        val retryable: Boolean,
        val userMessage: String,
        val technicalMessage: String? = null
    ) : AiResult<Nothing>

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = (this as? Success)?.value

    fun getOrDefault(default: @UnsafeVariance T): T = (this as? Success)?.value ?: default

    fun failureOrNull(): Failure? = this as? Failure
}
