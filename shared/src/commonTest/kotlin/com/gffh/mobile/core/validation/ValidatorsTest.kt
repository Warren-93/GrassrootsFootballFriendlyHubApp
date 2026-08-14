package com.gffh.mobile.core.validation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidatorsTest {

    @Test
    fun acceptsAPlausibleEmail() {
        assertTrue(Validators.isValidEmail("manager@denny-warriors.example.com"))
    }

    @Test
    fun rejectsAnEmailMissingAnAtSign() {
        assertFalse(Validators.isValidEmail("manager.example.com"))
    }

    @Test
    fun rejectsAnEmailMissingADomainDot() {
        assertFalse(Validators.isValidEmail("manager@example"))
    }

    @Test
    fun rejectsAnEmailContainingWhitespace() {
        assertFalse(Validators.isValidEmail("man ager@example.com"))
    }

    @Test
    fun trimsSurroundingWhitespaceBeforeValidatingEmail() {
        assertTrue(Validators.isValidEmail("  manager@example.com  "))
    }

    @Test
    fun passwordShorterThanTenCharactersIsInvalid() {
        assertFalse(Validators.isValidPassword("short9!"))
    }

    @Test
    fun passwordOfExactlyTenCharactersIsValid() {
        assertTrue(Validators.isValidPassword("1234567890"))
    }

    @Test
    fun displayNameOfOneCharacterIsTooShort() {
        assertFalse(Validators.isValidDisplayName("A"))
    }

    @Test
    fun displayNameOfSixtyCharactersIsValid() {
        assertTrue(Validators.isValidDisplayName("A".repeat(60)))
    }

    @Test
    fun displayNameOfSixtyOneCharactersIsTooLong() {
        assertFalse(Validators.isValidDisplayName("A".repeat(61)))
    }

    @Test
    fun displayNameIsTrimmedBeforeLengthIsChecked() {
        assertTrue(Validators.isValidDisplayName("  Jo  "))
        assertFalse(Validators.isValidDisplayName("  J  "))
    }
}
