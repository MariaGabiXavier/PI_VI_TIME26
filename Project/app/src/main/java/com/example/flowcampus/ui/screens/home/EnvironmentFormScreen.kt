
package com.example.flowcampus.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowcampus.ui.components.FormDropdownField
import com.example.flowcampus.ui.components.FormPrimaryButton
import com.example.flowcampus.ui.components.FormScreen
import com.example.flowcampus.ui.components.FormSecondaryButton
import com.example.flowcampus.ui.components.FormTextField
import com.example.flowcampus.ui.components.LabeledField
import com.example.flowcampus.ui.theme.AppGreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun EnvironmentFormScreen(
    environment: CampusPlace? = null,
    onBack: () -> Unit,
    onSave: suspend (EnvironmentFormData) -> Result<Unit>
) {
    val isEditing = environment != null
    val environmentId = environment?.firestoreId
    var name by rememberSaveable(environmentId) { mutableStateOf(environment?.name.orEmpty()) }
    var category by rememberSaveable(environmentId) { mutableStateOf(environment?.iconType) }
    var building by rememberSaveable(environmentId) { mutableStateOf(environment?.building.orEmpty()) }
    var capacity by rememberSaveable(environmentId) { mutableStateOf((environment?.capacity ?: 40).toString()) }
    var operatingHours by rememberSaveable(environmentId) {
        mutableStateOf(environment?.operatingHours ?: "07:00 - 22:00")
    }
    var description by rememberSaveable(environmentId) { mutableStateOf(environment?.description.orEmpty()) }

    var error by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    BackHandler(enabled = isSaving) {}

    FormScreen(
        title = if (isEditing) "Editar Ambiente" else "Cadastrar Ambiente",
        subtitle = if (isEditing) "Atualize os dados do ambiente." else "Adicione um novo ambiente monitorado ao sistema.",
        onBack = onBack,
        backEnabled = !isSaving
    ) {

        LabeledField(label = "Nome") {

            FormTextField(
                value = name,
                onValueChange = {
                    name = it
                    error = null
                },
                placeholder = "Ex: Lab A2",
                enabled = !isSaving
            )
        }

        LabeledField(label = "Categoria") {

            FormDropdownField(
                selected = category,
                options = PlaceIcon.entries,
                optionLabel = { it.toDisplayName() },
                onSelect = {
                    category = it
                    error = null
                },
                placeholder = "Selecione uma categoria",
                enabled = !isSaving
            )
        }

        LabeledField(label = "Prédio") {

            FormTextField(
                value = building,
                onValueChange = {
                    building = it
                    error = null
                },
                placeholder = "Ex: H15",
                enabled = !isSaving
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            LabeledField(
                label = "Capacidade Máxima",
                modifier = Modifier.weight(1f)
            ) {

                FormTextField(
                    value = capacity,
                    onValueChange = {
                        capacity = it.filter(Char::isDigit)
                        error = null
                    },
                    placeholder = "Ex: 40",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    enabled = !isSaving
                )
            }

            LabeledField(
                label = "Funcionamento",
                modifier = Modifier.weight(1f)
            ) {

                FormTextField(
                    value = operatingHours,
                    onValueChange = {
                        operatingHours = it
                        error = null
                    },
                    placeholder = "07:00 - 22:00",
                    enabled = !isSaving
                )
            }
        }

        LabeledField(label = "Descrição") {

            FormTextField(
                value = description,
                onValueChange = {
                    description = it
                    error = null
                },
                placeholder = "Descreva brevemente o uso do ambiente, sensores instalados ou particularidades do local...",
                singleLine = false,
                height = 96.dp,
                enabled = !isSaving
            )
        }

        if (error != null) {
            Text(
                text = error!!,
                color = HighRedText,
                fontSize = 13.sp
            )
        }

        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            FormPrimaryButton(
                text = "Salvar Ambiente",
                containerColor = AppGreen,
                loading = isSaving,
                onClick = {
                    if (isSaving) return@FormPrimaryButton
                    val data = EnvironmentFormData(
                        name = name.trim(),
                        category = category,
                        building = building.trim(),
                        capacity = capacity.toIntOrNull() ?: 0,
                        operatingHours = operatingHours.trim(),
                        description = description.trim()
                    )
                    error = data.validationError(
                        requireBuilding = environment?.building?.isBlank() != true,
                        requireHours = environment?.operatingHours?.isBlank() != true
                    )
                    if (error != null) return@FormPrimaryButton

                    isSaving = true
                    scope.launch {
                        try {
                            onSave(data).getOrThrow()
                            onBack()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            error = e.message ?: "Não foi possível salvar o ambiente."
                        } finally {
                            isSaving = false
                        }
                    }
                }
            )

            FormSecondaryButton(
                text = "Cancelar",
                onClick = onBack,
                enabled = !isSaving
            )
        }
    }
}

private fun PlaceIcon.toDisplayName(): String = when (this) {
    PlaceIcon.RESTAURANT -> "Refeitório"
    PlaceIcon.LIBRARY -> "Biblioteca"
    PlaceIcon.BUS -> "Ponto de ônibus"
    PlaceIcon.LAB -> "Laboratório"
    PlaceIcon.STUDY -> "Sala de estudo"
    PlaceIcon.CLASSROOM -> "Sala de aula"
}
