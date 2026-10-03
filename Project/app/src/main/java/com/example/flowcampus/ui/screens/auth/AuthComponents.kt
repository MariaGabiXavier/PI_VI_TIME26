package com.example.flowcampus.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowcampus.ui.screens.home.Blue
import com.example.flowcampus.ui.screens.home.DarkText
import com.example.flowcampus.ui.screens.home.GrayText
import com.example.flowcampus.ui.screens.home.HighRed
import com.example.flowcampus.ui.screens.home.HighRedText

private val FieldBorder = Color(0xFFE3E6EE)
private val FieldIcon = Color(0xFF9DA5BA)
private val FieldPlaceholder = Color(0xFFA4AABD)

/** Logo + título + subtítulo, igual ao cabeçalho do onboarding. */
@Composable
fun AuthHeader(
    title: String,
    subtitle: String
) {

    Column {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Logo",
                tint = Blue,
                modifier = Modifier.size(28.dp)
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = "FlowCampus",
                color = DarkText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        Text(
            text = title,
            color = DarkText,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 32.sp
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = subtitle,
            color = GrayText,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    error: String? = null,
    onImeDone: () -> Unit = {}
) {

    val focusManager = LocalFocusManager.current

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {

        Text(
            text = label,
            color = DarkText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = error != null,
            shape = RoundedCornerShape(13.dp),

            textStyle = TextStyle(
                color = DarkText,
                fontSize = 14.sp
            ),

            placeholder = {
                Text(
                    text = placeholder,
                    color = FieldPlaceholder,
                    fontSize = 13.sp
                )
            },

            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = FieldIcon,
                    modifier = Modifier.size(20.dp)
                )
            },

            trailingIcon = if (isPassword) {
                {
                    IconButton(
                        onClick = {
                            passwordVisible = !passwordVisible
                        }
                    ) {

                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (passwordVisible) {
                                "Ocultar senha"
                            } else {
                                "Mostrar senha"
                            },
                            tint = FieldIcon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                null
            },

            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },

            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                imeAction = imeAction
            ),

            keyboardActions = KeyboardActions(
                onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                },
                onDone = {
                    onImeDone()
                }
            ),

            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                errorContainerColor = Color.White,
                focusedBorderColor = Blue,
                unfocusedBorderColor = FieldBorder,
                errorBorderColor = HighRedText,
                cursorColor = Blue
            )
        )

        if (error != null) {

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = error,
                color = HighRedText,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false
) {

    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),

        shape = RoundedCornerShape(13.dp),

        colors = ButtonDefaults.buttonColors(
            containerColor = Blue,
            disabledContainerColor = Blue.copy(alpha = 0.6f)
        )
    ) {

        if (loading) {

            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )

        } else {

            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Faixa de erro geral do formulário (ex.: falha de rede, credenciais inválidas). */
@Composable
fun AuthFormError(
    message: String,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HighRed)
            .padding(
                horizontal = 14.dp,
                vertical = 10.dp
            )
    ) {

        Text(
            text = message,
            color = HighRedText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Rodapé do tipo "Não tem conta? Cadastre-se". */
@Composable
fun AuthFooterLink(
    text: String,
    linkText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = "$text ",
            color = GrayText,
            fontSize = 13.sp
        )

        Text(
            text = linkText,
            color = Blue,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable {
                onClick()
            }
        )
    }
}
