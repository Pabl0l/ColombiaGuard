package com.colombiaguard.ui.whitelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.colombiaguard.domain.entity.PhoneNumbers
import com.colombiaguard.ui.appViewModel
import com.colombiaguard.ui.theme.Error
import com.colombiaguard.ui.theme.OnBackground
import com.colombiaguard.ui.theme.OnPrimary
import com.colombiaguard.ui.theme.OnSurface
import com.colombiaguard.ui.theme.OnSurfaceVariant
import com.colombiaguard.ui.theme.Primary
import com.colombiaguard.ui.theme.Success
import com.colombiaguard.ui.theme.Surface
import com.colombiaguard.ui.theme.SurfaceBorder
import com.colombiaguard.ui.theme.TextTertiary

/**
 * Pantalla para gestionar la whitelist o la blacklist.
 * La validación del número la hace el caso de uso; aquí solo se muestra.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(
	isWhitelist: Boolean,
	viewModel: NumbersViewModel = appViewModel { NumbersViewModel(it) }
) {
	val state by viewModel.state.collectAsState()

	var showAddDialog by remember { mutableStateOf(false) }
	var inputNumber by remember { mutableStateOf("") }
	var inputLabel by remember { mutableStateOf("") }
	var lastAttemptFailed by remember { mutableStateOf(false) }

	LaunchedEffect(isWhitelist) { viewModel.start(isWhitelist) }

	fun closeDialog() {
		showAddDialog = false
		inputNumber = ""
		inputLabel = ""
		lastAttemptFailed = false
		viewModel.clearError()
	}

	Scaffold(
		floatingActionButton = {
			FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Primary) {
				Icon(Icons.Default.Add, contentDescription = "Agregar número", tint = OnPrimary)
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
			item {
				Text(
					text = if (isWhitelist) "Números permitidos" else "Números bloqueados",
					style = MaterialTheme.typography.headlineMedium,
					fontWeight = FontWeight.Bold,
					color = OnBackground
				)
				Spacer(Modifier.height(8.dp))
				Text(
					text = if (isWhitelist) {
						"Estos números siempre podrán llamarte"
					} else {
						"Estos números nunca podrán llamarte"
					},
					style = MaterialTheme.typography.bodyMedium,
					color = OnSurfaceVariant
				)
				Spacer(Modifier.height(16.dp))
			}

			if (state.numbers.isEmpty()) {
				item {
					Card(
						modifier = Modifier.fillMaxWidth(),
						colors = CardDefaults.cardColors(containerColor = Surface)
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
							Spacer(Modifier.height(16.dp))
							Text(
								text = if (isWhitelist) "Sin números permitidos" else "Sin números bloqueados",
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

			items(state.numbers, key = { it.phoneNumber }) { number ->
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
						Spacer(Modifier.width(16.dp))
						Column(modifier = Modifier.weight(1f)) {
							Text(
								text = PhoneNumbers.formatDisplay(number.phoneNumber),
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
						IconButton(onClick = { viewModel.remove(isWhitelist, number.phoneNumber) }) {
							Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Error)
						}
					}
				}
			}
		}
	}

	if (showAddDialog) {
		AlertDialog(
			onDismissRequest = { closeDialog() },
			containerColor = Surface,
			titleContentColor = OnSurface,
			textContentColor = OnSurfaceVariant,
			title = { Text(if (isWhitelist) "Agregar a permitidos" else "Agregar a bloqueados") },
			text = {
				Column {
					OutlinedTextField(
						value = inputNumber,
						onValueChange = {
							inputNumber = it
							lastAttemptFailed = false
						},
						label = { Text("Número de teléfono") },
						placeholder = { Text("+57 300 123 4567") },
						modifier = Modifier.fillMaxWidth(),
						singleLine = true,
						isError = lastAttemptFailed,
						colors = OutlinedTextFieldDefaults.colors(
							focusedBorderColor = Primary,
							unfocusedBorderColor = SurfaceBorder,
							focusedTextColor = OnSurface,
							unfocusedTextColor = OnSurface,
							cursorColor = Primary
						)
					)
					if (lastAttemptFailed) {
						Text(
							text = state.error ?: "Número inválido",
							style = MaterialTheme.typography.bodySmall,
							color = Error,
							modifier = Modifier.padding(top = 4.dp)
						)
					}
					Spacer(Modifier.height(8.dp))
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
						if (inputNumber.isBlank()) {
							lastAttemptFailed = true
						} else {
							viewModel.add(isWhitelist, inputNumber, inputLabel) { saved ->
								if (saved) closeDialog() else lastAttemptFailed = true
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
					onClick = { closeDialog() },
					colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)
				) {
					Text("Cancelar")
				}
			}
		)
	}
}
