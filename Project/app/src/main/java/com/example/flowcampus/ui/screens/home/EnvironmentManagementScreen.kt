package com.example.flowcampus.ui.screens.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.flowcampus.ui.components.FormScreen
import kotlinx.coroutines.launch

enum class EnvironmentAction {
    ADD,
    EDIT,
    DELETE
}

@Composable
fun EnvironmentManagementScreen(
    action: EnvironmentAction,
    places: List<CampusPlace>,
    onBack: () -> Unit,
    onSave: suspend (String?, EnvironmentFormData) -> Result<Unit>,
    onDelete: suspend (String) -> Result<Unit>
) {
    if (action == EnvironmentAction.ADD) {
        EnvironmentFormScreen(
            onBack = onBack,
            onSave = { onSave(null, it) }
        )
        return
    }

    var selectedPlace by remember(action) { mutableStateOf<CampusPlace?>(null) }
    val selected = selectedPlace
    val isEditing = action == EnvironmentAction.EDIT

    if (isEditing && selected != null) {
        EnvironmentFormScreen(
            environment = selected,
            onBack = { selectedPlace = null },
            onSave = { onSave(selected.firestoreId, it) }
        )
        return
    }

    FormScreen(
        title = if (isEditing) "Editar Ambiente" else "Excluir Ambiente",
        subtitle = if (isEditing) "Selecione o ambiente que deseja editar."
            else "Selecione o ambiente que deseja excluir.",
        onBack = onBack
    ) {
        if (places.isEmpty()) {
            Text("Nenhum ambiente disponível.")
        }

        places.sortedBy { it.name.lowercase() }.forEach { place ->
            OutlinedButton(
                onClick = { selectedPlace = place },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (place.building.isBlank()) place.name else "${place.name} · ${place.building}")
            }
        }
    }

    if (selected != null) {
        DeleteEnvironmentDialog(
            place = selected,
            onDismiss = { selectedPlace = null },
            onDelete = { onDelete(selected.firestoreId) }
        )
    }
}

@Composable
private fun DeleteEnvironmentDialog(
    place: CampusPlace,
    onDismiss: () -> Unit,
    onDelete: suspend () -> Result<Unit>
) {
    var isDeleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { Text("Excluir ambiente?") },
        text = {
            Text(
                error ?: "O ambiente “${place.name}” será removido da lista. " +
                    "O histórico de ocupação será mantido."
            )
        },
        confirmButton = {
            TextButton(
                enabled = !isDeleting,
                onClick = {
                    if (!isDeleting) {
                        isDeleting = true
                        error = null
                        scope.launch {
                            onDelete()
                                .onSuccess { onDismiss() }
                                .onFailure { error = it.message ?: "Não foi possível excluir o ambiente." }
                            isDeleting = false
                        }
                    }
                }
            ) {
                Text(if (isDeleting) "Excluindo…" else "Excluir", color = HighRedText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text("Cancelar")
            }
        }
    )
}
