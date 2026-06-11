package com.example.smartqa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartqa.R
import com.example.smartqa.ui.theme.Accent
import com.example.smartqa.ui.theme.Border
import com.example.smartqa.ui.theme.Bg
import com.example.smartqa.ui.theme.Muted
import com.example.smartqa.ui.theme.Surface

@Composable
fun ChatInputBar(onSendMessage: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(10.dp, 10.dp, 10.dp, 14.dp)
        ,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Bg)
                .border(
                    width = 1.dp,
                    color = if (isFocused) Accent else Border,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(8.dp, 8.dp, 8.dp, 18.dp),
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth().padding(18.dp, 8.dp)
                    .onFocusChanged { isFocused = it.isFocused },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (text.trim().isNotEmpty()) {
                            onSendMessage(text.trim())
                            text = ""
                        }
                    }
                ),
                decorationBox = { innerTextField ->
                    if (text.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.placeholder_input),
                            color = Muted,
                            fontSize = 15.sp,

                        )
                    }
                    innerTextField()
                },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                ),
                maxLines = 4
            )
        }

        IconButton(
            onClick = {
                if (text.trim().isNotEmpty()) {
                    onSendMessage(text.trim())
                    text = ""
                }
            },
            enabled = text.trim().isNotEmpty(),
            modifier = Modifier
                .padding(start = 8.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Accent)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = stringResource(id = R.string.label_send),
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Preview
@Composable
fun ChatInputBarPreview() {
    MaterialTheme {
        ChatInputBar(onSendMessage = {})
    }
}