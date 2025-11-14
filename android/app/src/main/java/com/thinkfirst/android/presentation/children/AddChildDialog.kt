package com.thinkfirst.android.presentation.children

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChildDialog(
    onDismiss: () -> Unit,
    onConfirm: (username: String, password: String, age: Int, gradeLevel: String?) -> Unit,
    errorMessage: String? = null
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var selectedGrade by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showGradeMenu by remember { mutableStateOf(false) }
    
    val gradeLevels = listOf(
        "KINDERGARTEN" to "Kindergarten",
        "FIRST" to "1st Grade",
        "SECOND" to "2nd Grade",
        "THIRD" to "3rd Grade",
        "FOURTH" to "4th Grade",
        "FIFTH" to "5th Grade",
        "SIXTH" to "6th Grade",
        "SEVENTH" to "7th Grade",
        "EIGHTH" to "8th Grade",
        "NINTH" to "9th Grade",
        "TENTH" to "10th Grade",
        "ELEVENTH" to "11th Grade",
        "TWELFTH" to "12th Grade"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Child") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Username Field
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., tommy") }
                )
                
                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) 
                        VisualTransformation.None 
                    else 
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) 
                                    Icons.Default.Visibility 
                                else 
                                    Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) 
                                    "Hide password" 
                                else 
                                    "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Create a password") }
                )
                
                // Age Field
                OutlinedTextField(
                    value = age,
                    onValueChange = { 
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            age = it
                        }
                    },
                    label = { Text("Age") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., 10") }
                )
                
                // Grade Level Dropdown
                ExposedDropdownMenuBox(
                    expanded = showGradeMenu,
                    onExpandedChange = { showGradeMenu = it }
                ) {
                    OutlinedTextField(
                        value = selectedGrade?.let { grade ->
                            gradeLevels.find { it.first == grade }?.second
                        } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Grade Level (Optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showGradeMenu) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        placeholder = { Text("Select grade") }
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showGradeMenu,
                        onDismissRequest = { showGradeMenu = false }
                    ) {
                        gradeLevels.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedGrade = value
                                    showGradeMenu = false
                                }
                            )
                        }
                    }
                }
                
                // Error message
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Validation hint
                Text(
                    text = "Username and password are required. Age must be between 5-18.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ageInt = age.toIntOrNull()
                    if (username.isNotBlank() && 
                        password.isNotBlank() && 
                        ageInt != null && 
                        ageInt in 5..18) {
                        onConfirm(username, password, ageInt, selectedGrade)
                    }
                },
                enabled = username.isNotBlank() && 
                         password.isNotBlank() && 
                         age.toIntOrNull()?.let { it in 5..18 } == true
            ) {
                Text("Add Child")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

