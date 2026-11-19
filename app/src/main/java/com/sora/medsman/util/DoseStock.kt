package com.sora.medsman.util

import com.sora.medsman.data.model.TakeStatus

/**
 * Stock change when an existing dose log is corrected. A negative value means
 * inventory is consumed; a positive value means it is returned.
 */
fun stockDeltaForStatusChange(
    existingStatus: TakeStatus,
    existingUnitsPerDose: Int,
    newStatus: TakeStatus,
    newUnitsPerDose: Int
): Int {
    val previouslyTaken = if (existingStatus == TakeStatus.TAKEN) {
        existingUnitsPerDose.coerceAtLeast(1)
    } else {
        0
    }
    val nowTaken = if (newStatus == TakeStatus.TAKEN) {
        newUnitsPerDose.coerceAtLeast(1)
    } else {
        0
    }
    return previouslyTaken - nowTaken
}
