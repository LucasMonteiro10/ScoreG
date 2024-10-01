package com.example.scoreg.pages.gamenavbar

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.compose.ui.graphics.Color
import com.example.scoreg.R
import com.example.scoreg.components.ActionButtons
import com.example.scoreg.components.CustomTopAppBar
import com.example.scoreg.components.GameListSection
import com.example.scoreg.components.GameView
import com.example.scoreg.models.MainViewModel

@Composable
fun GameInfoPage(
    navController: NavController,
    mainViewModel: MainViewModel,
) {
    val activity = LocalContext.current as? Activity

    val gameId = mainViewModel.currentGame.id // Pegue o ID do jogo atual

    // Checa se o jogo está nas listas
    val isCompleted = mainViewModel.isGameInList("completedGames", gameId)
    val isPlayingNow = mainViewModel.isGameInList("playingNow", gameId)
    val isWishListed = mainViewModel.isGameInList("wishList", gameId)

    val completedTextColor = if (isCompleted) Color.Red else Color.Green
    val playingTextColor = if (isPlayingNow) Color.Red else Color.Green
    val wishListTextColor = if (isWishListed) Color.Red else Color.Green

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            CustomTopAppBar(
                showBackButton = true,
                onBackClick = {
                    navController.navigate("home")
                }
            )

            GameView(game = mainViewModel.currentGame)

            // Ícones de adicionar/remover para cada estado
            val completedGamesAddIcon = R.drawable.navbar_icon_completedgames_green
            val completedGamesRemoveIcon = R.drawable.navbar_icon_completedgames_red
            val playingNowAddIcon = R.drawable.navbar_icon_playingnow_green
            val playingNowRemoveIcon = R.drawable.navbar_icon_playingnow_red
            val wishListAddIcon = R.drawable.navbar_icon_wishlist_green
            val wishListRemoveIcon = R.drawable.navbar_icon_wishlist_red

            ActionButtons(
                onAddToCompleted = {
                    if (isCompleted) {
                        mainViewModel.removeGameToCurrentUserList("completedGames")
                        mainViewModel.removeGameFromList("completedGames", gameId)
                        Toast.makeText(activity,"Jogo removido da lista 'Jogos Completados'.", Toast.LENGTH_SHORT).show()
                    }
                    else if (isPlayingNow) {
                        Toast.makeText(activity,"Remova o jogo da lista 'Jogando Agora' antes.", Toast.LENGTH_SHORT).show()
                    }
                    else if (isWishListed) {
                        Toast.makeText(activity,"Remova o jogo da lista 'Lista de Compras' antes.", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        mainViewModel.addGameToCurrentUserList("completedGames")
                        mainViewModel.addGameToList("completedGames", gameId)
                        Toast.makeText(activity,"Jogo adicionado à lista 'Jogos Completados'.", Toast.LENGTH_SHORT).show()
                    }

                },
                onAddToPlaying = {
                    if (isPlayingNow) {
                        mainViewModel.removeGameToCurrentUserList("playingNow")
                        mainViewModel.removeGameFromList("playingNow", gameId)
                        Toast.makeText(activity,"Jogo removido da lista 'Jogando Agora'.", Toast.LENGTH_SHORT).show()
                    }
                    else if (isCompleted) {
                        Toast.makeText(activity,"Remova o jogo da lista 'Jogos Completados' antes.", Toast.LENGTH_SHORT).show()
                    }
                    else if (isWishListed) {
                        Toast.makeText(activity,"Remova o jogo da lista 'Lista de Compras' antes.", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        mainViewModel.addGameToCurrentUserList("playingNow")
                        mainViewModel.addGameToList("playingNow", gameId)
                        Toast.makeText(activity,"Jogo adicionado à lista 'Jogando Agora'.", Toast.LENGTH_SHORT).show()
                    }
                },
                onAddToWishlist = {
                    if (isWishListed) {
                        mainViewModel.removeGameToCurrentUserList("wishList")
                        mainViewModel.removeGameFromList("wishList", gameId)
                        Toast.makeText(activity,"Jogo removido da lista 'Lista de Compras'.", Toast.LENGTH_SHORT).show()
                    }
                    else if (isCompleted) {
                        Toast.makeText(activity,"Remova o jogo da lista 'Jogos Completados' antes.", Toast.LENGTH_SHORT).show()
                    }
                    else if (isPlayingNow) {
                        Toast.makeText(activity,"Remova o jogo da lista 'Jogando Agora' antes.", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        mainViewModel.addGameToCurrentUserList("wishList")
                        mainViewModel.addGameToList("wishList", gameId)
                        Toast.makeText(activity,"Jogo adicionado à lista 'Lista de compras'.", Toast.LENGTH_SHORT).show()
                    }
                },
                completedIcon = if (isCompleted) completedGamesRemoveIcon else completedGamesAddIcon,
                playingIcon = if (isPlayingNow) playingNowRemoveIcon else playingNowAddIcon,
                wishlistIcon = if (isWishListed) wishListRemoveIcon else wishListAddIcon,
                completedTextColor = completedTextColor,
                playingTextColor = playingTextColor,
                wishlistTextColor = wishListTextColor
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()

        ) {
            GameListSection(
                title = "Mais Populares",
                games = mainViewModel.gamesSortedByScore.value.toList(),
                mainViewModel = mainViewModel,
                navController = navController
            )
        }
    }
}
