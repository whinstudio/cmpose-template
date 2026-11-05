package com.kturker.multiplatform.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kturker.multiplatform.core.designsystem.icon.IcSearch
import com.kturker.multiplatform.core.designsystem.theme.LocalColorsPalette

@Composable
fun KtSearchBar(
    modifier: Modifier = Modifier,
    text: String,
    placeHolderText: String,
    onValueChange: (newValue: String) -> Unit,
    maxLines: Int = 1,
    singleLine: Boolean = true,
    onSearch: () -> Unit = {}
) {
    val color = LocalColorsPalette.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.backgroundColor)
            .padding(all = 8.dp)
    ) {
        OutlinedTextField(
            modifier = modifier.fillMaxWidth(),
            value = text,
            onValueChange = onValueChange,
            maxLines = maxLines,
            singleLine = singleLine,
            placeholder = {
                KtText(
                    text = placeHolderText,
                    color = color.textFieldLabelColor,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
                focusManager.clearFocus()
                onSearch.invoke()
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = color.textFieldFocusedBorder,
                unfocusedBorderColor = color.textFieldUnFocusedBorder,
                disabledBorderColor = color.textFieldFocusedBorder,
                focusedTextColor = color.textFieldTextColor,
                unfocusedTextColor = color.textFieldTextColor,
                disabledTextColor = color.textFieldTextColor,
                unfocusedSupportingTextColor = color.textFieldTextColor,
                focusedSupportingTextColor = color.textFieldTextColor,
                disabledSupportingTextColor = color.textFieldTextColor,
                errorSupportingTextColor = color.textFieldTextColor,
                cursorColor = color.textFieldCursorColor

            ),
            leadingIcon = {
                KtIcon(
                    modifier = Modifier.size(24.dp),
                    imageVector = IcSearch,
                    // Decorative: the field's placeholder says what it is for
                    contentDescription = null,
                    tint = color.tintGrey
                )
            }
        )
    }
}
