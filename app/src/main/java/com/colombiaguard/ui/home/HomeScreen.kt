package com.colombiaguard.ui.home

import android.app.role.RoleManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.colombiaguard.domain.entity.FilterConfig
import com.colombiaguard.domain.entity.FilterLevel
import com.colombiaguard.domain.repository.CallStats
import com.colombiaguard.ui.appViewModel
import com.colombiaguard.ui.theme.Error
import com.colombiaguard.ui.theme.ErrorMuted
import com.colombiaguard.ui.theme.IconNormal
import com.colombiaguard.ui.theme.IconPermissive
import com.colombiaguard.ui.theme.IconStrict
import com.colombiaguard.ui.theme.OnBackground
import com.colombiaguard.ui.theme.OnPrimary
import com.colombiaguard.ui.theme.OnSurface
import com.colombiaguard.ui.theme.OnSurfaceVariant
import com.colombiaguard.ui.theme.Primary
import com.colombiaguard.ui.theme.PrimaryMuted
import com.colombiaguard.ui.theme.StatusNormal
import com.colombiaguard.ui.theme.StatusPermissive
import com.colombiaguard.ui.theme.StatusStrict
import com.colombiaguard.ui.theme.Success
import com.colombiaguard.ui.theme.SuccessMuted
import com.colombiaguard.ui.theme.Surface
import com.colombiaguard.ui.theme.SurfaceVariant
import com.colombiaguard.ui.theme.Warning
import com.colombiaguard.ui.theme.WarningMuted

@Composable
fun HomeScreen(viewModel: HomeViewModel = appViewModel { HomeViewModel(it) }) {
	val state by viewModel.state.collectAsState()
	val context = LocalContext.current

	LaunchedEffect(Unit) { viewModel.refresh() }

	LazyColumn(
		modifier = Modifier
			.fillMaxSize()
			.padding(16.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp)
	) {
		item { StatusCard(state.config) }

		item { ScreeningRoleCard() }

		item {
			Text(
				text = "Estadísticas",
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.Bold,
				color = OnBackground
			)
		}

		item {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(12.dp)
			) {
				StatCard(Modifier.weight(1f), "Total", "${state.stats?.totalCalls ?: 0}", Icons.Default.Phone, Primary)
				StatCard(Modifier.weight(1f), "Bloqueadas", "${state.stats?.blockedCalls ?: 0}", Icons.Default.Block, Error)
			}
		}

		item {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(12.dp)
			) {
				StatCard(Modifier.weight(1f), "Permitidas", "${state.stats?.allowedCalls ?: 0}", Icons.Default.Check, Success)
				StatCard(Modifier.weight(1f), "Extranjeras", "${state.stats?.foreignBlocked ?: 0}", Icons.Default.Public, Warning)
			}
		}

		item {
			Text(
				text = "Nivel de filtro",
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.Bold,
				modifier = Modifier.padding(top = 8.dp),
				color = OnBackground
			)
		}

		item {
			FilterLevelSelector(
				currentLevel = state.config.level,
				onLevelChanged = viewModel::setLevel
			)
		}
	}
}

/**
 * Estado real del permiso de Android: sin el rol `ROLE_CALL_SCREENING` la app
 * no recibe ninguna llamada, así que conviene mostrarlo y ofrecer el atajo.
 */
@Composable
fun ScreeningRoleCard() {
	val context = LocalContext.current
	val roleManager = remember { context.getSystemService(RoleManager::class.java) }

	fun isHeld(): Boolean = roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true

	var held by remember { mutableStateOf(isHeld()) }
	val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
		held = isHeld()
	}

	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(containerColor = if (held) StatusNormal else StatusStrict)
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Icon(
					imageVector = if (held) Icons.Default.Check else Icons.Default.Warning,
					contentDescription = null,
					tint = if (held) IconNormal else IconStrict,
					modifier = Modifier.size(28.dp)
				)
				Spacer(Modifier.width(12.dp))
				Text(
					text = if (held) "Filtro activo" else "Filtro inactivo",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					color = OnSurface
				)
			}
			Spacer(Modifier.height(8.dp))
			Text(
				text = if (held) {
					"ColombiaGuard analiza tus llamadas entrantes antes de que suenen."
				} else {
					"Android no entrega las llamadas: falta autorizar ColombiaGuard como app de filtrado."
				},
				style = MaterialTheme.typography.bodySmall,
				color = OnSurfaceVariant
			)
			if (!held && roleManager != null) {
				Spacer(Modifier.height(12.dp))
				Button(
					onClick = {
						runCatching {
							roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
						}.onSuccess { launcher.launch(it) }
					},
					colors = ButtonDefaults.buttonColors(containerColor = PrimaryMuted, contentColor = OnPrimary)
				) {
					Text("Activar filtro")
				}
			}
		}
	}
}

@Composable
fun StatusCard(config: FilterConfig) {
	val (bgColor, iconColor, icon) = when (config.level) {
		FilterLevel.STRICT -> Triple(StatusStrict, IconStrict, Icons.Default.Shield)
		FilterLevel.NORMAL -> Triple(StatusNormal, IconNormal, Icons.Default.Security)
		FilterLevel.PERMISSIVE -> Triple(StatusPermissive, IconPermissive, Icons.Default.Warning)
	}

	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(containerColor = bgColor)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = iconColor)
			Spacer(Modifier.width(16.dp))
			Column {
				Text(
					text = "Filtro: ${config.level.name}",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					color = OnSurface
				)
				Text(
					text = when (config.level) {
						FilterLevel.STRICT -> "Solo números colombianos válidos"
						FilterLevel.NORMAL -> "Colombianos permitidos, extranjeros bloqueados"
						FilterLevel.PERMISSIVE -> "Solo spam confirmado bloqueado"
					},
					style = MaterialTheme.typography.bodySmall,
					color = OnSurfaceVariant
				)
			}
		}
	}
}

@Composable
fun StatCard(
	modifier: Modifier = Modifier,
	title: String,
	value: String,
	icon: ImageVector,
	color: Color
) {
	Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Surface)) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
			Spacer(Modifier.height(8.dp))
			Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = OnBackground)
			Text(title, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
		}
	}
}

@Composable
fun FilterLevelSelector(currentLevel: FilterLevel, onLevelChanged: (FilterLevel) -> Unit) {
	Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
		FilterLevel.entries.forEach { level ->
			val isSelected = currentLevel == level
			val (bgColor, labelColor) = when {
				!isSelected -> Surface to OnSurfaceVariant
				level == FilterLevel.STRICT -> ErrorMuted to OnPrimary
				level == FilterLevel.NORMAL -> SuccessMuted to OnPrimary
				else -> WarningMuted to OnPrimary
			}

			Button(
				onClick = { onLevelChanged(level) },
				modifier = Modifier.fillMaxWidth(),
				colors = ButtonDefaults.buttonColors(containerColor = bgColor, contentColor = labelColor)
			) {
				Icon(
					imageVector = when (level) {
						FilterLevel.STRICT -> Icons.Default.Shield
						FilterLevel.NORMAL -> Icons.Default.Security
						FilterLevel.PERMISSIVE -> Icons.Default.Warning
					},
					contentDescription = null,
					modifier = Modifier.size(24.dp)
				)
				Spacer(Modifier.width(12.dp))
				Text(
					text = when (level) {
						FilterLevel.STRICT -> "Estricto - Solo colombianos válidos"
						FilterLevel.NORMAL -> "Normal - Balance inteligente"
						FilterLevel.PERMISSIVE -> "Permisivo - Solo bloquear spam"
					},
					style = MaterialTheme.typography.bodyLarge,
					fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
				)
			}
		}
	}
}
