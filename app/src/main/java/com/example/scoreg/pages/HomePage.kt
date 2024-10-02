package com.example.scoreg.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.scoreg.components.CustomTopAppBar
import com.example.scoreg.components.GameListSection
import com.example.scoreg.components.Navbar
import com.example.scoreg.components.SearchGameBar
import com.example.scoreg.models.MainViewModel
import com.example.scoreg.utils.IntentUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    navController: NavController,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    mainViewModel.fetchAndSortGames()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        CustomTopAppBar(
            showLogoutButton = true,
            onLogoutClick = {
                Firebase.auth.signOut()
                val intent = IntentUtils.createLoginActivityIntent(context)
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SearchGameBar(mainViewModel = mainViewModel, navController = navController)

        Navbar(navController = navController)

        Spacer(modifier = Modifier.height(16.dp))

        Column {
            GameListSection(
                title = "Mais Populares",
                games = mainViewModel.gamesSortedByScore.value.toList(),
                mainViewModel = mainViewModel,
                navController = navController
            )

            GameListSection(
                title = "Lançamentos",
                games = mainViewModel.gamesSortedByReleaseYear.value.toList(),
                mainViewModel = mainViewModel,
                navController = navController
            )
        }



    }
}