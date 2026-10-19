package com.example.flowcampus.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowcampus.ui.screens.home.BackgroundColor
import com.example.flowcampus.ui.screens.home.Blue
import com.example.flowcampus.ui.screens.home.DarkText
import com.example.flowcampus.ui.screens.home.GrayText

private val FieldShape = RoundedCornerShape(12.dp)
private val FieldBorder = Color(0xFFE3E6EE)
private val FieldPlaceholder = Color(0xFFA4AABD)
private val FieldIcon = Color(0xFF9DA5BA)

/** Base das telas de cadastro/edição: voltar + título, subtítulo e o card branco com o formulário. */
@Composable
fun FormScreen(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    backEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {

    BackHandler(enabled = backEnabled) {
        onBack()
    }

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
                enabled = backEnabled
            ) {

                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = DarkText
                )
            }

            Text(
                text = title,
                color = DarkText,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = 4.dp,
                    bottom = 24.dp
                )
        ) {

            Text(
                text = subtitle,
                color = GrayText,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = content
                )
            }
        }
    }
}

/** Rótulo em cima e o campo logo abaixo. */
@Composable
fun LabeledField(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        Text(
            text = label,
            color = DarkText,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        content()
    }
}

/** Campo de texto mais baixo que o OutlinedTextField, como no protótipo. */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    height: Dp = 44.dp,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {

    var isFocused by remember {
        mutableStateOf(false)
    }

    val textStyle = LocalTextStyle.current.merge(
        TextStyle(
            color = DarkText,
            fontSize = 15.sp,
            lineHeight = 21.sp
        )
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        cursorBrush = SolidColor(Blue),

        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(FieldShape)
            .background(Color.White)
            .border(
                width = 1.dp,
                color = if (isFocused) Blue else FieldBorder,
                shape = FieldShape
            )
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .padding(
                horizontal = 16.dp,
                vertical = if (singleLine) 0.dp else 12.dp
            ),

        decorationBox = { innerTextField ->

            Box(
                contentAlignment = if (singleLine) {
                    Alignment.CenterStart
                } else {
                    Alignment.TopStart
                }
            ) {

                if (value.isEmpty()) {

                    Text(
                        text = placeholder,
                        style = textStyle.copy(color = FieldPlaceholder)
                    )
                }

                innerTextField()
            }
        }
    )
}

/** Campo de seleção com a mesma aparência do [FormTextField]. */
@Composable
fun <T> FormDropdownField(
    selected: T?,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    // O menu abre com a mesma largura do campo
    var fieldWidth by remember {
        mutableStateOf(0.dp)
    }

    val density = LocalDensity.current

    Box(
        modifier = modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(FieldShape)
                .background(Color.White)
                .border(
                    width = 1.dp,
                    color = if (expanded) Blue else FieldBorder,
                    shape = FieldShape
                )
                .clickable(enabled = enabled) {
                    expanded = true
                }
                .onSizeChanged {
                    fieldWidth = with(density) { it.width.toDp() }
                }
                .padding(
                    start = 16.dp,
                    end = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = selected?.let(optionLabel) ?: placeholder,
                color = if (selected != null) DarkText else FieldPlaceholder,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = FieldIcon,
                modifier = Modifier.size(22.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            containerColor = Color.White,
            modifier = Modifier.width(fieldWidth)
        ) {

            options.forEach { option ->

                DropdownMenuItem(
                    text = {
                        Text(
                            text = optionLabel(option),
                            color = DarkText,
                            fontSize = 14.sp
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun FormPrimaryButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp),

        shape = RoundedCornerShape(12.dp),

        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor.copy(alpha = 0.6f)
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

@Composable
fun FormSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp),

        shape = RoundedCornerShape(12.dp),

        border = BorderStroke(1.dp, FieldBorder),

        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = GrayText
        )
    ) {

        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
