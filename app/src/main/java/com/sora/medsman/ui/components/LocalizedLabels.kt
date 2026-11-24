package com.sora.medsman.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sora.medsman.R
import java.util.Locale

/** Localized label for a stored medication form value. */
@Composable
fun localizedFormLabel(form: String): String {
    val labelRes = when (form.trim().lowercase(Locale.ROOT)) {
        "tablet", "pill" -> R.string.form_tablet
        "capsule" -> R.string.form_capsule
        "liquid" -> R.string.form_liquid
        "injection" -> R.string.form_injection
        "other" -> R.string.form_other
        else -> null
    }
    return labelRes?.let { stringResource(it) } ?: form
}

/**
 * Localized label for a stored inventory unit. The built-in "pills" default is
 * kept in the database so it can follow the current app language.
 */
@Composable
fun localizedUnit(unit: String): String {
    val trimmed = unit.trim()
    return if (trimmed.isEmpty() || trimmed.equals("pills", ignoreCase = true) ||
        trimmed.equals("pill", ignoreCase = true)
    ) {
        stringResource(R.string.default_unit)
    } else {
        trimmed
    }
}
