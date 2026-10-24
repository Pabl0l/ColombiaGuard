package com.colombiaguard.ui.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.colombiaguard.ui.appViewModel
import com.colombiaguard.ui.theme.Error
import com.colombiaguard.ui.theme.OnBackground
import com.colombiaguard.ui.theme.OnSurface
import com.colombiaguard.ui.theme.OnSurfaceVariant
import com.colombiaguard.ui.theme.Primary
import com.colombiaguard.ui.theme.Surface
import com.colombiaguard.ui.theme.SurfaceBorder

@Composable
fun ConfigScreen(viewModel: ConfigViewModel = appViewModel { ConfigViewModel(it) }) {
	val config by viewModel.config.collectAsState()

	Column(
		modifier = Modifier
			.fillMaxSize()
			.verticalScroll(rememberScrollState())
			.padding(16.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp)
	) {
		Text(
			text = "Configuración",
			style = MaterialTheme.typography.headlineMedium,
			fontWeight = FontWeight.Bold,
			color = OnBackground
		)
		Text(
			text = "El nivel de filtro se elige en la pantalla de inicio.",
			style = MaterialTheme.typography.bodySmall,
			color = OnSurfaceVariant
		)

		SettingsCard(
			title = "Bloquear llamadas extranjeras",
			subtitle = "Rechaza números que no pertenecen al plan de numeración colombiano",
			checked = config.blockForeignCalls,
			onCheckedChange = viewModel::setBlockForeign
		)

		SettingsCard(
			title = "Silenciar colombianos desconocidos",
			subtitle = "La llamada entra pero no suena si el número no es un contacto",
			checked = config.silenceUnknownColombian,
			onCheckedChange = viewModel::setSilenceUnknown
		)

		SettingsCard(
			title = "Detección de patrones",
			subtitle = "Analiza llamadas repetidas, horarios y números sospechosos",
			checked = config.enablePatternDetection,
			onCheckedChange = viewModel::setPatternDetection
		)

		SettingsCard(
			title = "Permitir siempre a mis contactos",
			subtitle = "Un número guardado en la agenda nunca se bloquea",
			checked = config.whitelistAllContacts,
			onCheckedChange = viewModel::setWhitelistContacts
		)

		Spacer(Modifier.height(8.dp))

		TextButton(
			onClick = viewModel::reset,
			modifier = Modifier.fillMaxWidth(),
			colors = ButtonDefaults.textButtonColors(contentColor = Error)
		) {
			Text("Restablecer valores por defecto")
		}

		Text(
			text = "ColombiaGuard analiza todo en el dispositivo. No se envía ninguna llamada ni contacto a Internet.",
			style = MaterialTheme.typography.bodySmall,
			color = OnSurfaceVariant
		)
	}
}

@Composable
private fun SettingsCard(
	title: String,
	subtitle: String,
	checked: Boolean,
	onCheckedChange: (Boolean) -> Unit
) {
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
			Column(modifier = Modifier.weight(1f)) {
				Text(title, style = MaterialTheme.typography.titleMedium, color = OnSurface)
				Spacer(Modifier.height(4.dp))
				Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
			}
			Switch(
				checked = checked,
				onCheckedChange = onCheckedChange,
				colors = SwitchDefaults.colors(
					checkedThumbColor = OnSurface,
					checkedTrackColor = Primary,
					uncheckedThumbColor = OnSurfaceVariant,
					uncheckedTrackColor = SurfaceBorder
				)
			)
		}
	}
}
