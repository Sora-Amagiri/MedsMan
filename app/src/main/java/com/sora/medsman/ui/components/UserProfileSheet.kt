package com.sora.medsman.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sora.medsman.data.model.UserProfile
import com.sora.medsman.ui.theme.MedsManTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileSheet(
    profile: UserProfile,
    onSaveProfile: (UserProfile) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember(profile) { mutableStateOf(profile.name) }
    var ageText by remember(profile) { mutableStateOf(if (profile.age > 0) profile.age.toString() else "") }
    var emergencyContactName by remember(profile) { mutableStateOf(profile.emergencyContactName) }
    var emergencyContactPhone by remember(profile) { mutableStateOf(profile.emergencyContactPhone) }
    var allergiesText by remember(profile) { mutableStateOf(profile.allergies.joinToString(", ")) }
    var healthNotes by remember(profile) { mutableStateOf(profile.healthNotes) }
    var email by remember(profile) { mutableStateOf(profile.email) }
    var doctorName by remember(profile) { mutableStateOf(profile.doctorName) }
    var doctorPhone by remember(profile) { mutableStateOf(profile.doctorPhone) }
    var bloodType by remember(profile) { mutableStateOf(profile.bloodType) }

    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(scrollState)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "User Avatar",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "User Profile",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Manage personal details & emergency info",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close Sheet"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Personal Information Section
            Text(
                text = "Personal Information",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Person, contentDescription = "Name")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Age & Blood Type Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it.filter { char -> char.isDigit() } },
                    label = { Text("Age") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Cake, contentDescription = "Age")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = bloodType,
                    onValueChange = { bloodType = it },
                    label = { Text("Blood Type") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Bloodtype, contentDescription = "Blood Type")
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Mail, contentDescription = "Email")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Emergency Contact Section
            Text(
                text = "Emergency Contact",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = emergencyContactName,
                onValueChange = { emergencyContactName = it },
                label = { Text("Contact Name & Relationship") },
                placeholder = { Text("e.g. John Doe (Spouse)") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.ContactPhone, contentDescription = "Emergency Contact Name")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = emergencyContactPhone,
                onValueChange = { emergencyContactPhone = it },
                label = { Text("Contact Phone Number") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Phone, contentDescription = "Emergency Phone")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Health & Medical Notes Section
            Text(
                text = "Allergies & Health Notes",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Allergies Field (Comma separated)
            OutlinedTextField(
                value = allergiesText,
                onValueChange = { allergiesText = it },
                label = { Text("Allergies (comma-separated)") },
                placeholder = { Text("e.g. Penicillin, Sulfa, Peanuts") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Warning, contentDescription = "Allergies", tint = MaterialTheme.colorScheme.error)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Health Notes Field
            OutlinedTextField(
                value = healthNotes,
                onValueChange = { healthNotes = it },
                label = { Text("Health Notes & Medical Conditions") },
                placeholder = { Text("Any specific instructions, conditions, or doctor notes...") },
                leadingIcon = {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Notes, contentDescription = "Health Notes")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Physician / Doctor Info (Optional)
            Text(
                text = "Primary Physician",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = { Text("Doctor Name") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.LocalHospital, contentDescription = "Doctor Name")
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = doctorPhone,
                    onValueChange = { doctorPhone = it },
                    label = { Text("Doctor Phone") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Phone, contentDescription = "Doctor Phone")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save Action Button
            Button(
                onClick = {
                    val ageParsed = ageText.toIntOrNull() ?: 0
                    val allergiesList = allergiesText
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    val updatedProfile = profile.copy(
                        name = name.trim(),
                        age = ageParsed,
                        email = email.trim(),
                        emergencyContactName = emergencyContactName.trim(),
                        emergencyContactPhone = emergencyContactPhone.trim(),
                        allergies = allergiesList,
                        healthNotes = healthNotes.trim(),
                        doctorName = doctorName.trim(),
                        doctorPhone = doctorPhone.trim(),
                        bloodType = bloodType.trim()
                    )

                    onSaveProfile(updatedProfile)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = "Save Profile",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Profile",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UserProfileSheetPreview() {
    MedsManTheme {
        UserProfileSheet(
            profile = UserProfile(
                name = "Jane Doe",
                age = 36,
                email = "jane.doe@example.com",
                emergencyContactName = "John Doe (Spouse)",
                emergencyContactPhone = "+1 (555) 019-2834",
                allergies = listOf("Penicillin", "Sulfa drugs"),
                healthNotes = "Takes morning medication with meals."
            ),
            onSaveProfile = {},
            onDismiss = {}
        )
    }
}
