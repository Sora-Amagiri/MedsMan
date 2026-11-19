package com.sora.medsman.util

/**
 * Formats a dose for display. A single unit keeps the plain dosage label
 * ("10 mg"), while multi-unit doses lead with the quantity ("2 × 10 mg").
 */
fun formatDoseAmount(unitsPerDose: Int, dosage: String): String {
    val units = unitsPerDose.coerceAtLeast(1)
    val strength = dosage.trim()
    return when {
        units <= 1 -> strength
        strength.isEmpty() -> "$units units"
        else -> "$units × $strength"
    }
}
