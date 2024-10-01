package com.example.scoreg.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.scoreg.database.entities.Game
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainViewModel : ViewModel() {

    // V Auth State  V ---------------------------------------------------------------------------
    private var _loggedIn = mutableStateOf(false)
    val loggedIn: Boolean get() = _loggedIn.value
    private val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _loggedIn.value = firebaseAuth.currentUser != null
    }

    init {
        listener.onAuthStateChanged(Firebase.auth)
    }

    // V FB Utils V ---------------------------------------------------------------------------

    // Instância do FirebaseDatabase
    private val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance()
    }

    // Retorna a referência para um caminho específico no Realtime Database
    private fun getDatabaseReference(path: String): DatabaseReference {
        return database.getReference(path)
    }

    private fun getCurrentUserId(): String? {
        val firebaseAuth = FirebaseAuth.getInstance()
        return firebaseAuth.currentUser?.uid
    }

    // V Games Table V ---------------------------------------------------------------------------

    fun updateGameScore(gameId: String, newScore: Int) {
        val gameRef = getDatabaseReference("games/$gameId")
        gameRef.child("score").setValue(newScore)
    }

    // Listas observáveis para armazenar os jogos ordenados por score e releaseYear
    var gamesSortedByScore = mutableStateOf<List<Game>>(listOf())
        private set

    var gamesSortedByReleaseYear = mutableStateOf<List<Game>>(listOf())
        private set

    fun fetchAndSortGames() {
        val gamesRef = getDatabaseReference("games")

        // Listener para escutar mudanças no nó "games"
        gamesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val gamesList = mutableListOf<Game>()

                // Itera sobre todos os jogos no banco de dados
                for (gameSnapshot in dataSnapshot.children) {
                    val gameId = gameSnapshot.key // Obtém o ID único do jogo
                    val game = gameSnapshot.getValue(Game::class.java)

                    if (game != null && gameId != null) {
                        // Define o ID do jogo
                        val gameWithId = game.copy(id = gameId)
                        gamesList.add(gameWithId)
                    }
                }

                // Ordena as listas
                val sortedByScore = gamesList.sortedByDescending { it.score }
                val sortedByReleaseYear = gamesList.sortedByDescending { it.releaseYear }

                // Atualiza as listas observáveis
                gamesSortedByScore.value = sortedByScore
                gamesSortedByReleaseYear.value = sortedByReleaseYear
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Lida com erros, se necessário
            }
        })
    }

    fun searchGamesByTitle(query: String, callback: (List<Game>) -> Unit) {
        val gamesRef = getDatabaseReference("games")

        gamesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val matchingGames = mutableListOf<Game>()

                // Itera sobre todos os jogos no banco de dados
                for (gameSnapshot in dataSnapshot.children) {
                    val game = gameSnapshot.getValue(Game::class.java)
                    val gameId = gameSnapshot.key

                    if (game != null && gameId != null) {
                        // Verifica se o título do jogo contém a string passada na query (ignorando maiúsculas/minúsculas)
                        if (game.title.contains(query, ignoreCase = true)) {
                            val gameWithId = game.copy(id = gameId)
                            matchingGames.add(gameWithId)
                        }
                    }
                }

                // Chama o callback com a lista de jogos encontrados
                callback(matchingGames)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Lida com erros, se necessário
                callback(emptyList()) // Retorna uma lista vazia em caso de erro
            }
        })
    }

    // V Users Table V ---------------------------------------------------------------------------

    var currentUserCompletedGamesList = mutableStateOf<List<Game>>(listOf())
        private set

    var currentUserPlayingNowList = mutableStateOf<List<Game>>(listOf())
        private set

    var currentUserWishList = mutableStateOf<List<Game>>(listOf())
        private set

    // Função para atualizar a Game list de jogos de acordo com o nome da lista do usuário logado
    fun fetchCurrentUserGameList(listName: String) {
        val currentUserId = getCurrentUserId()
        val userRef = getDatabaseReference("users/$currentUserId/$listName")
        val gamesRef = getDatabaseReference("games")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnapshot: DataSnapshot) {
                val completedGameIds = userSnapshot.children.mapNotNull { it.key }

                val completedGamesListTemp = mutableListOf<Game>()

                gamesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(gamesSnapshot: DataSnapshot) {
                        for (gameSnapshot in gamesSnapshot.children) {
                            val gameId = gameSnapshot.key
                            val game = gameSnapshot.getValue(Game::class.java)

                            if (gameId != null && gameId in completedGameIds && game != null) {
                                val gameWithId = game.copy(id = gameId)
                                completedGamesListTemp.add(gameWithId)
                            }
                        }

                        when (listName) {
                            "completedGames" -> {
                                currentUserCompletedGamesList.value = completedGamesListTemp
                            }

                            "playingNow" -> {
                                currentUserPlayingNowList.value = completedGamesListTemp
                            }

                            "wishList" -> {
                                currentUserWishList.value = completedGamesListTemp
                            }
                        }

                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Lida com erros, se necessário
                    }
                })
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Lida com erros, se necessário
            }
        })
    }

    var completedGames = mutableStateOf<List<String>>(emptyList())
        private set
    var playingNow = mutableStateOf<List<String>>(emptyList())
        private set
    var wishList = mutableStateOf<List<String>>(emptyList())
        private set

    lateinit var currentGame: Game
        private set

    // Atualiza o currentGame e busca a lista do usuário logado onde o game está
    fun setCurrentGame(currentGameTemp: Game) {
        currentGame = currentGameTemp
        observeUserGameLists() // Observa as listas do usuário ao alterar o currentGame
    }

    // Função para observar as listas do usuário logado e verificar se o jogo atual está em alguma delas
    private fun observeUserGameLists() {
        val currentUserId = getCurrentUserId()
        if (currentUserId != null) {
            val userRef = getDatabaseReference("users/$currentUserId")

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Atualiza as listas do usuário logado
                    val completedGamesList =
                        dataSnapshot.child("completedGames").children.mapNotNull { it.key }
                    val playingNowList =
                        dataSnapshot.child("playingNow").children.mapNotNull { it.key }
                    val wishListGamesList =
                        dataSnapshot.child("wishList").children.mapNotNull { it.key }

                    // Atualiza as variáveis observáveis
                    completedGames.value = completedGamesList
                    playingNow.value = playingNowList
                    wishList.value = wishListGamesList

                    // Verifica se o currentGame está em alguma dessas listas
                    updateCurrentGameListBasedOnUserLists(currentGame.id)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Lida com erros
                }
            })
        }
    }

    // Função para verificar em qual lista o currentGame está e atualizar o estado apropriado
    private fun updateCurrentGameListBasedOnUserLists(gameId: String) {
        when (gameId) {
            in completedGames.value -> {
                currentGameList.value = "completedGames"
            }

            in playingNow.value -> {
                currentGameList.value = "playingNow"
            }

            in wishList.value -> {
                currentGameList.value = "wishList"
            }

            else -> {
                currentGameList.value = ""
            }
        }
    }

    // Variável que representa a lista atual onde o jogo está
    var currentGameList = mutableStateOf("")

    fun addGameToCurrentUserList(listName: String) {
        val currentUserId = getCurrentUserId()
        if (currentUserId != null) {
            // Referência ao caminho da lista específica no banco de dados (completedGames, playingNow ou wishList)
            val listRef = getDatabaseReference("users/$currentUserId/$listName/${currentGame.id}")

            // Define o valor como 'true' para indicar que o game está na lista
            listRef.setValue(true).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("Jogo adicionado à lista $listName com sucesso!")
                    currentGameList.value = listName
                } else {
                    println("Falha ao adicionar jogo à lista: ${task.exception?.message}")
                }
            }
        } else {
            println("Usuário não está logado!")
        }
    }

    fun removeGameToCurrentUserList(listName: String) {
        val currentUserId = getCurrentUserId()
        if (currentUserId != null) {
            // Referência ao caminho da lista específica no banco de dados
            val listRef = getDatabaseReference("users/$currentUserId/$listName/${currentGame.id}")

            // Remove o jogo da lista
            listRef.removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("Jogo removido da lista $listName com sucesso!")
                    currentGameList.value = ""
                } else {
                    println("Falha ao remover jogo da lista: ${task.exception?.message}")
                }
            }
        } else {
            println("Usuário não está logado!")
        }
    }


    // Funções para adicionar e remover jogos
    fun addGameToList(listType: String, gameId: String) {
        when (listType) {
            "completedGames" -> {
                completedGames.value = completedGames.value + gameId
            }

            "playingNow" -> {
                playingNow.value = playingNow.value + gameId
            }

            "wishList" -> {
                wishList.value = wishList.value + gameId
            }
        }
    }

    fun removeGameFromList(listType: String, gameId: String) {
        when (listType) {
            "completedGames" -> {
                completedGames.value = completedGames.value.filter { it != gameId }
            }

            "playingNow" -> {
                playingNow.value = playingNow.value.filter { it != gameId }
            }

            "wishList" -> {
                wishList.value = wishList.value.filter { it != gameId }
            }
        }
    }

    // Função para verificar se um jogo está na lista
    fun isGameInList(listType: String, gameId: String): Boolean {
        return when (listType) {
            "completedGames" -> completedGames.value.contains(gameId)
            "playingNow" -> playingNow.value.contains(gameId)
            "wishList" -> wishList.value.contains(gameId)
            else -> false
        }
    }

}
