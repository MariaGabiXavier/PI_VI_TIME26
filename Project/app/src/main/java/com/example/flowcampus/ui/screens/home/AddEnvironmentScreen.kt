
package com.example.flowcampus.ui.screens.home

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
import kotlinx.coroutines.launch

@Composable
fun AddEnvironmentScreen(
    onBack: () -> Unit,
    onSave: suspend (
        String,
        PlaceIcon,
        String,
        Int,
        String,
        String
    ) -> Result<Unit>
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<PlaceIcon?>(null) }
    var building by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("40") }
    var operatingHours by remember { mutableStateOf("07:00 - 22:00") }
    var description by remember { mutableStateOf("") }

    var error by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    FormScreen(
        title = "Cadastrar Ambiente",
        subtitle = "Adicione um novo ambiente monitorado ao sistema.",
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
                    val parsedCapacity = capacity.toIntOrNull()

                    when {
                        name.isBlank() ->
                            error = "Informe o nome do ambiente."

                        category == null ->
                            error = "Selecione uma categoria."

                        building.isBlank() ->
                            error = "Informe o prédio."

                        parsedCapacity == null ||
                            parsedCapacity <= 0 ->
                            error = "Informe uma capacidade maior que zero."

                        operatingHours.isBlank() ->
                            error = "Informe o horário de funcionamento."

                        else -> {
                            scope.launch {
                                isSaving = true
                                error = null

                                try {
                                    val result = onSave(
                                        name.trim(),
                                        category!!,
                                        building.trim(),
                                        parsedCapacity,
                                        operatingHours.trim(),
                                        description.trim()
                                    )

                                    result.fold(
                                        onSuccess = {
                                            onBack()
                                        },
                                        onFailure = {
                                            error = it.message
                                                ?: "Não foi possível salvar o ambiente."
                                        }
                                    )
                                } catch (e: Exception) {
                                    error = e.message
                                        ?: "Não foi possível salvar o ambiente."
                                } finally {
                                    isSaving = false
                                }
                            }
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
