package com.example.flowcampus.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.example.flowcampus.ui.components.FormPrimaryButton
import com.example.flowcampus.ui.components.FormScreen
import com.example.flowcampus.ui.components.FormSecondaryButton
import com.example.flowcampus.ui.components.FormTextField
import com.example.flowcampus.ui.components.LabeledField
import com.example.flowcampus.ui.screens.auth.AuthValidator
import com.example.flowcampus.ui.screens.auth.UserProfile
import com.example.flowcampus.ui.screens.auth.UserRole
import com.example.flowcampus.ui.screens.home.HighRedText
import com.example.flowcampus.ui.theme.AppGreen
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(
    user: UserProfile,
    onBack: () -> Unit,
    onSave: suspend (name: String, ra: String) -> Result<Unit>
) {

    var name by remember { mutableStateOf(user.name) }
    var ra by remember { mutableStateOf(user.ra) }

    var error by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // RA é só de estudante
    val isStudent = user.role == UserRole.STUDENT

    FormScreen(
        title = "Editar Perfil",
        subtitle = "Atualize as informações que aparecem no seu perfil.",
        onBack = onBack,
        backEnabled = !isSaving
    ) {

        LabeledField(label = "Nome completo") {

            FormTextField(
                value = name,
                onValueChange = {
                    name = it
                    error = null
                },
                placeholder = "Seu nome",
                enabled = !isSaving
            )
        }

        if (isStudent) {

            LabeledField(label = "RA") {

                FormTextField(
                    value = ra,
                    onValueChange = {
                        ra = it.filter(Char::isDigit)
                        error = null
                    },
                    placeholder = "Ex: 26175800",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    enabled = !isSaving
                )
            }
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
                text = "Salvar Alterações",
                containerColor = AppGreen,
                loading = isSaving,
                onClick = {
                    val nameError = AuthValidator.name(name)

                    if (nameError != null) {
                        error = nameError
                    } else {
                        scope.launch {
                            isSaving = true
                            error = null

                            onSave(name.trim(), ra.trim())
                                .onSuccess {
                                    onBack()
                                }
                                .onFailure {
                                    error = "Não foi possível salvar as alterações. Tente novamente."
                                }

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
