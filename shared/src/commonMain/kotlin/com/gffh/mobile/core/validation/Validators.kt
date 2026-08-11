package com.gffh.mobile.core.validation

/** Mirrors the field-level rules in Screen Build Specification SCR-AU-03/04. */
object Validators {

    private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun isValidEmail(value: String): Boolean = EMAIL_REGEX.matches(value.trim())

    /** Minimum 10 characters; no composition rules beyond length, per SCR-AU-03. */
    fun isValidPassword(value: String): Boolean = value.length >= 10

    /** 2-60 characters, per SCR-AU-03's full-name field. */
    fun isValidDisplayName(value: String): Boolean = value.trim().length in 2..60
}
