package com.tqmane.filmsim.core.security

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SecurityCheckUseCaseTest {

    private val context = mockk<Context>()
    private val securityChecker = mockk<SecurityChecker>()

    @Test
    fun `returns true when the checker trusts the environment`() {
        every { securityChecker.isEnvironmentTrusted(context) } returns true

        val useCase = SecurityCheckUseCase(context, securityChecker)

        assertTrue(useCase.isTrusted())
        verify(exactly = 1) { securityChecker.isEnvironmentTrusted(context) }
    }

    @Test
    fun `returns false when the checker rejects the environment`() {
        every { securityChecker.isEnvironmentTrusted(context) } returns false

        val useCase = SecurityCheckUseCase(context, securityChecker)

        assertFalse(useCase.isTrusted())
        verify(exactly = 1) { securityChecker.isEnvironmentTrusted(context) }
    }

    @Test
    fun `refreshTrust invalidates cache before rechecking`() {
        every { securityChecker.invalidateCache() } returns Unit
        every { securityChecker.isEnvironmentTrusted(context) } returns true

        val useCase = SecurityCheckUseCase(context, securityChecker)

        assertTrue(useCase.refreshTrust())
        verifyOrder {
            securityChecker.invalidateCache()
            securityChecker.isEnvironmentTrusted(context)
        }
    }
}
