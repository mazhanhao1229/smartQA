package com.example.smartqa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartqa.data.ChatMessage
import com.example.smartqa.data.MessageType
import com.example.smartqa.ui.theme.Accent
import com.example.smartqa.ui.theme.Border
import com.example.smartqa.ui.theme.Fg
import com.example.smartqa.ui.theme.Muted
import com.example.smartqa.ui.theme.Surface

@Composable
fun ChatBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (message.type == MessageType.USER) Alignment.End else Alignment.Start
    ) {
        if (message.type == MessageType.AI) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Box(
            modifier = Modifier
                .clip(
                    if (message.type == MessageType.AI) {
                        RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
                    } else {
                        RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)
                    }
                )
                .background(if (message.type == MessageType.AI) Surface else Accent)
                .padding(12.dp, 16.dp)
                .then(
                    if (message.type == MessageType.AI) {
                        Modifier.background(Surface)
                    } else {
                        Modifier
                    }
                ),
        ) {
            Text(
                text = message.content,
                color = if (message.type == MessageType.USER) androidx.compose.ui.graphics.Color.White else Fg,
                fontSize = 15.sp,
                letterSpacing = (-0.01).sp
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = message.timestamp,
            color = Muted,
            fontSize = 11.sp,
            modifier = Modifier.padding(8.dp, 0.dp)
        )
    }
}

@Preview
@Composable
fun ChatBubblePreview() {
    MaterialTheme {
        Column {
            ChatBubble(
                message = ChatMessage(
                    id = "1",
                    content = "你好！我是 AI 助手，可以帮你解答 Android 开发相关的问题。",
                    type = MessageType.AI,
                    timestamp = "10:32"
                )
            )
            ChatBubble(
                message = ChatMessage(
                    id = "2",
                    content = "Jetpack Compose 中怎么实现一个带动画的 LazyColumn 列表项？",
                    type = MessageType.USER,
                    timestamp = "10:33"
                )
            )
        }
    }
}