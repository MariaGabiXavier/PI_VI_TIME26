
package com.example.flowcampus.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable

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
    var capacity by remember { mutableStateOf("") }
    var operatingHours by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var showCategories by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 20.dp,
                    top = 4.dp,
                    bottom = 4.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                enabled = !isSaving
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = DarkText
                )
            }

            Text(
                text = "Cadastrar ambiente",
                color = DarkText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 28.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Adicione um novo ambiente monitorado ao sistema.",
                color = GrayText,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Informações do ambiente",
                        color = DarkText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    FormLabel("Nome do ambiente")

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            error = null
                        },
                        placeholder = {
                            Text("Ex.: Lab A2", color = GrayText)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving
                    )

                    FormLabel("Categoria")

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = category?.toDisplayName()
                                ?: "",
                            onValueChange = {},
                            readOnly = true,
                            placeholder = {
                                Text(
                                    "Selecione uma categoria",
                                    color = GrayText
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = "Selecionar categoria",
                                    tint = GrayText
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSaving
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(end = 42.dp)
                                .clickable(enabled = !isSaving) {
                                    showCategories = true
                                }
                        )

                        DropdownMenu(
                            expanded = showCategories,
                            onDismissRequest = {
                                showCategories = false
                            },
                            modifier = Modifier.background(Color.White)
                        ) {
                            PlaceIcon.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(option.toDisplayName())
                                    },
                                    onClick = {
                                        category = option
                                        showCategories = false
                                        error = null
                                    }
                                )
                            }
                        }
                    }

                    FormLabel("Prédio")

                    OutlinedTextField(
                        value = building,
                        onValueChange = {
                            building = it
                            error = null
                        },
                        placeholder = {
                            Text("Ex.: H15", color = GrayText)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            FormLabel("Capacidade máxima")

                            OutlinedTextField(
                                value = capacity,
                                onValueChange = {
                                    capacity = it.filter(Char::isDigit)
                                    error = null
                                },
                                placeholder = {
                                    Text("Ex.: 40", color = GrayText)
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isSaving
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            FormLabel("Funcionamento")

                            OutlinedTextField(
                                value = operatingHours,
                                onValueChange = {
                                    operatingHours = it
                                    error = null
                                },
                                placeholder = {
                                    Text(
                                        "Ex.: 07:00 às 22:00",
                                        color = GrayText,
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isSaving
                            )
                        }
                    }

                    FormLabel("Descrição")

                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            error = null
                        },
                        placeholder = {
                            Text(
                                "Descreva brevemente o uso do ambiente, sensores instalados ou particularidades do local...",
                                color = GrayText,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(125.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving
                    )

                    if (error != null) {
                        Text(
                            text = error!!,
                            color = Color(0xFFCF3333),
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Button(
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
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                "Salvar ambiente",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF0F2F7),
                            contentColor = DarkText
                        )
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        color = DarkText,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )
}

private fun PlaceIcon.toDisplayName(): String = when (this) {
    PlaceIcon.RESTAURANT -> "Refeitório"
    PlaceIcon.LIBRARY -> "Biblioteca"
    PlaceIcon.BUS -> "Ponto de ônibus"
    PlaceIcon.LAB -> "Laboratório"
    PlaceIcon.STUDY -> "Sala de estudo"
    PlaceIcon.CLASSROOM -> "Sala de aula"
}
