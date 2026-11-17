package com.sora.medsman

import com.sora.medsman.data.local.Converters
import com.sora.medsman.data.model.*
import org.junit.Assert.*
import org.junit.Test

class MedsManRepositoryTest {

    private val converters = Converters()

    @Test
    fun testConvertersIntList() {
        val list = listOf(1, 2, 3, 4, 5, 6, 7)
        val string = converters.fromIntList(list)
        assertEquals("1,2,3,4,5,6,7", string)

        val restored = converters.toIntList(string)
        assertEquals(list, restored)
    }

    @Test
    fun testConvertersStringList() {
        val list = listOf("Penicillin", "Sulfa drugs")
        val string = converters.fromStringList(list)
        assertEquals("Penicillin;;Sulfa drugs", string)

        val restored = converters.toStringList(string)
        assertEquals(list, restored)
    }

    @Test
    fun testConvertersTakeStatus() {
        val status = TakeStatus.TAKEN
        val string = converters.fromTakeStatus(status)
        assertEquals("TAKEN", string)

        val restored = converters.toTakeStatus(string)
        assertEquals(status, restored)
    }

    @Test
    fun testMedicationModel() {
        val med = Medication(
            id = 1,
            name = "Lisinopril",
            dosage = "10 mg",
            form = "Tablet",
            category = "Prescription"
        )
        assertEquals("Lisinopril", med.name)
        assertEquals("10 mg", med.dosage)
        assertFalse(med.isPRN)
    }

    @Test
    fun testInventoryModel() {
        val inv = Inventory(
            medicationId = 1,
            currentStock = 15,
            refillThreshold = 5
        )
        assertEquals(15, inv.currentStock)
        assertFalse(inv.currentStock <= inv.refillThreshold)
    }

    @Test
    fun testUserProfileModel() {
        val profile = UserProfile(
            name = "Jane Doe",
            email = "jane@example.com",
            allergies = listOf("Aspirin")
        )
        assertEquals("Jane Doe", profile.name)
        assertEquals(1, profile.allergies.size)
    }
}
