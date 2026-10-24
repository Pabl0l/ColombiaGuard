package com.colombiaguard.ui.whitelist

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.colombiaguard.domain.repository.ListedNumber
import com.colombiaguard.service.ColombiaGuardApp
import com.colombiaguard.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Pantalla para gestionar whitelist o blacklist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(isWhitelist: Boolean) {
    val context = LocalContext.current
    val app = context.applicationContext as ColombiaGuardApp
    val scope = rememberCoroutineScope()

    var numbers by remember { mutableStateOf<List<ListedNumber>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var inputNumber by remember { mutableStateOf("") }
    var inputLabel by remember { mutableStateOf("") }

    // Observe list
    val flow = if (isWhitelist) app.manageNumberListUseCase.observeWhitelist()
               else app.manageNumberListUseCase.observeBlacklist()

    LaunchedEffect(Unit) {
        flow.collect { numbers = it }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar numero", tint = OnPrimary)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            item {
                Text(
                    text = if (isWhitelist) "Numeros Permitidos" else "Numeros Bloqueados",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isWhitelist)
                        "Estos numeros siempre podran llamarte"
                    else
                        "Estos numeros nunca podran llamarte",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Empty state
            if (numbers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (isWhitelist) Icons.Default.CheckCircle else Icons.Default.Block,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = TextTertiary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isWhitelist) "Sin numeros permitidos" else "Sin numeros bloqueados",
                                style = MaterialTheme.typography.titleMedium,
                                color = OnSurface
                            )
                            Text(
                                text = "Toca + para agregar uno",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                    }
                }
            }

            // Number list
            items(numbers) { number ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isWhitelist) Icons.Default.CheckCircle else Icons.Default.Block,
                            contentDescription = null,
                            tint = if (isWhitelist) Success else Error,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = number.phoneNumber,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            if (number.label.isNotBlank()) {
                                Text(
                                    text = number.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (isWhitelist) {
                                        app.manageNumberListUseCase.removeFromAllow(number.phoneNumber)
                                    } else {
                                        app.manageNumberListUseCase.removeFromBlock(number.phoneNumber)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = Error
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Number Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                inputNumber = ""
                inputLabel = ""
            },
            containerColor = Surface,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant,
            title = {
                Text(if (isWhitelist) "Agregar a Permitidos" else "Agregar a Bloqueados")
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputNumber,
                        onValueChange = { inputNumber = it },
                        label = { Text("Numero de telefono") },
                        placeholder = { Text("+57 300 123 4567") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface,
                            cursorColor = Primary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputLabel,
                        onValueChange = { inputLabel = it },
                        label = { Text("Etiqueta (opcional)") },
                        placeholder = { Text("Ej: Contacto conocido") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface,
                            cursorColor = Primary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inputNumber.isNotBlank()) {
                            scope.launch {
                                if (isWhitelist) {
                                    app.manageNumberListUseCase.allowNumber(inputNumber, inputLabel)
                                } else {
                                    app.manageNumberListUseCase.blockNumber(inputNumber, inputLabel)
                                }
                                showAddDialog = false
                                inputNumber = ""
                                inputLabel = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Primary)
                ) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        inputNumber = ""
                        inputLabel = ""
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
