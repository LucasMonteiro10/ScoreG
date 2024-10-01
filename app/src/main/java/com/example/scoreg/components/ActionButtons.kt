package com.example.scoreg.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ActionButtons(
    onAddToCompleted: () -> Unit,
    onAddToPlaying: () -> Unit,
    onAddToWishlist: () -> Unit,
    completedIcon: Int, // Ícone para "Jogos Completados"
    playingIcon: Int, // Ícone para "Jogando Agora"
    wishlistIcon: Int, // Ícone para "Lista de Compras"
    completedTextColor: Color, // Cor para "Jogos Completados"
    playingTextColor: Color, // Cor para "Jogando Agora"
    wishlistTextColor: Color, // Cor para "Lista de Compras"
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ActionButton(
            text = "Jogos Completados",
            icon = completedIcon,
            onClick = onAddToCompleted,
            textColor = completedTextColor // Passando a cor do texto
        )

        Spacer(modifier = Modifier.width(8.dp))

        ActionButton(
            text = "Jogando Agora",
            icon = playingIcon,
            onClick = onAddToPlaying,
            textColor = playingTextColor // Passando a cor do texto
        )

        Spacer(modifier = Modifier.width(8.dp))

        ActionButton(
            text = "Lista de Compras",
            icon = wishlistIcon,
            onClick = onAddToWishlist,
            textColor = wishlistTextColor // Passando a cor do texto
        )
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: Int,
    onClick: () -> Unit,
    textColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = "$icon",
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontSize = 12.sp
        )
    }
}
