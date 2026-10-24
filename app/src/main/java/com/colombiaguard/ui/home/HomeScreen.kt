package com.colombiaguard.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.colombiaguard.domain.entity.FilterLevel
import com.colombiaguard.service.ColombiaGuardApp
import com.colombiaguard.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as ColombiaGuardApp
    val scope = rememberCoroutineScope()

    var stats by remember { mutableStateOf<com.colombiaguard.domain.repository.CallStats?>(null) }
    var filterConfig by remember { mutableStateOf<com.colombiaguard.domain.entity.FilterConfig?>(null) }

    LaunchedEffect(Unit) {
        stats = app.manageCallHistoryUseCase.getStats()
        filterConfig = app.manageFilterConfigUseCase.getCurrentConfig()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card
        item {
            StatusCard(filterConfig)
        }

        // Stats Cards
        item {
            Text(
                text = "Estadisticas",
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
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total",
                    value = "${stats?.totalCalls ?: 0}",
                    icon = Icons.Default.Phone,
                    color = Primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Bloqueadas",
                    value = "${stats?.blockedCalls ?: 0}",
                    icon = Icons.Default.Block,
                    color = Error
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Permitidas",
                    value = "${stats?.allowedCalls ?: 0}",
                    icon = Icons.Default.Check,
                    color = Success
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Extranjeras",
                    value = "${stats?.foreignBlocked ?: 0}",
                    icon = Icons.Default.Public,
                    color = Warning
                )
            }
        }

        // Filter Level Selector
        item {
            Text(
                text = "Nivel de Filtro",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
                color = OnBackground
            )
        }

        item {
            FilterLevelSelector(
                currentLevel = filterConfig?.level ?: FilterLevel.NORMAL,
                onLevelChanged = { level ->
                    scope.launch {
                        app.manageFilterConfigUseCase.setFilterLevel(level)
                        filterConfig = app.manageFilterConfigUseCase.getCurrentConfig()
                    }
                }
            )
        }

        // Quick Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "ColombiaGuard Activo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "El servicio de filtrado esta activo. Las llamadas seran analizadas automaticamente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatusCard(config: com.colombiaguard.domain.entity.FilterConfig?) {
    val (bgColor, iconColor, icon) = when (config?.level) {
        FilterLevel.STRICT -> Triple(StatusStrict, IconStrict, Icons.Default.Shield)
        FilterLevel.NORMAL -> Triple(StatusNormal, IconNormal, Icons.Default.Security)
        FilterLevel.PERMISSIVE -> Triple(StatusPermissive, IconPermissive, Icons.Default.Warning)
        null -> Triple(SurfaceVariant, OnSurfaceVariant, Icons.Default.Info)
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = iconColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Filtro: ${config?.level?.name ?: "Cargando..."}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                Text(
                    text = when (config?.level) {
                        FilterLevel.STRICT -> "Solo numeros colombianos permitidos"
                        FilterLevel.NORMAL -> "Colombianos permitidos, extranjeros silenciados"
                        FilterLevel.PERMISSIVE -> "Solo spam confirmado bloqueado"
                        null -> "Cargando..."
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
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = OnBackground
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }
    }
}

@Composable
fun FilterLevelSelector(
    currentLevel: FilterLevel,
    onLevelChanged: (FilterLevel) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterLevel.entries.forEach { level ->
            val isSelected = currentLevel == level
            val (bgColor, labelColor) = when {
                !isSelected -> Surface to OnSurfaceVariant
                level == FilterLevel.STRICT -> ErrorMuted to OnPrimary
                level == FilterLevel.NORMAL -> SuccessMuted to OnPrimary
                level == FilterLevel.PERMISSIVE -> WarningMuted to OnPrimary
                else -> PrimaryMuted to OnPrimary
            }

            Button(
                onClick = { onLevelChanged(level) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = bgColor,
                    contentColor = labelColor
                )
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
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = when (level) {
                        FilterLevel.STRICT -> "Estricto - Solo colombianos validos"
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
