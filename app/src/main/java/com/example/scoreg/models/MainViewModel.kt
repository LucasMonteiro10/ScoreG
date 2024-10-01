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

    lateinit var currentGame: Game
        private set

    // Atualiza o currentGame e busca a lista do usuário logado onde o game está
    fun setCurrentGame(currentGameTemp: Game) {
        currentGame = currentGameTemp
        observeGameUpdates(currentGame.id)    // Continua observando mudanças no jogo
    }

    // Variável que representa a lista atual onde o jogo está
    var currentGameList = mutableStateOf<String>("")

    // Função para atualizar currentGameList baseado no currentGame.id
    private fun updateCurrentGameListBasedOnCurrentGame() {
        if (::currentGame.isInitialized) {
            val gameId = currentGame.id // Assume que currentGame tem uma propriedade 'id'
            if (gameId != null) {
                updateCurrentGameList(gameId)
            } else {
                currentGameList.value = ""
            }
        } else {
            currentGameList.value = ""
        }
    }

    // Função que verifica em qual lista do usuário o currentGame está e atualiza currentGameList
    private fun updateCurrentGameList(gameId: String) {
        val currentUserId = getCurrentUserId()
        if (currentUserId != null) {
            val userRef = getDatabaseReference("users/$currentUserId")

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Listas de IDs dos jogos do usuário logado
                    val completedGames = dataSnapshot.child("completedGames").children.mapNotNull { it.key }
                    val playingNowGames = dataSnapshot.child("playingNow").children.mapNotNull { it.key }
                    val wishListGames = dataSnapshot.child("wishList").children.mapNotNull { it.key }

                    // Atualiza o currentGameList com o nome da lista onde o jogo foi encontrado
                    when (gameId) {
                        in completedGames -> {
                            currentGameList.value = "completedGames"
                        }
                        in playingNowGames -> {
                            currentGameList.value = "playingNow"
                        }
                        in wishListGames -> {
                            currentGameList.value = "wishList"
                        }
                        else -> {
                            // O jogo não está em nenhuma lista do usuário
                            currentGameList.value = ""
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Lida com erros, se necessário
                }
            })
        }
    }

    fun observeGameUpdates(gameId: String) {
        val gameRef = getDatabaseReference("games/$gameId")

        gameRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val game = dataSnapshot.getValue(Game::class.java)
                if (game != null) {
                    // Atualiza o currentGame com o jogo atualizado do Firebase
                    currentGame = game.copy(id = gameId)
                }
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

                        if (listName == "completedGames") {
                            currentUserCompletedGamesList.value = completedGamesListTemp
                        }
                        else if (listName == "playingNow") {
                            currentUserPlayingNowList.value = completedGamesListTemp
                        }
                        else if (listName == "wishList") {
                            currentUserWishList.value = completedGamesListTemp
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

    private fun convertListNameToFBRefName(listName: String): String {
        val listNamePretty = when (listName) {
            "completedGames" -> "Jogos Completados"
            "playingNow" -> "Jogando Agora"
            "wishList" -> "Lista de Compras"
            else -> "Tem isso não!"
        }

        return listNamePretty

    }

    // Função conferir se o jogo faz parte de alguma lista e adiciona o ID do jogo na lista de jogos do usuário logado
    fun addGameToCurrentUserListWithCheck(listName: String, callback: (String) -> Unit) {
        val userId = getCurrentUserId()
        val userRef = getDatabaseReference("users/$userId")
        val listNamePretty = convertListNameToFBRefName(listName)

        // Listener para pegar todas as listas de jogos do usuário
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val completedGames = dataSnapshot.child("completedGames").children.mapNotNull { it.key }
                val playingNow = dataSnapshot.child("playingNow").children.mapNotNull { it.key }
                val wishList = dataSnapshot.child("wishList").children.mapNotNull { it.key }

                // Verifica se o jogo já pertence a alguma lista
                when (currentGame.id) {
                    in completedGames -> {
                        callback("O jogo já está na lista 'Jogos Completados'")
                    }
                    in playingNow -> {
                        callback("O jogo já está na lista 'Jogando Agora'")
                    }
                    in wishList -> {
                        callback("O jogo já está na 'Lista de Compras'")
                    }
                    else -> {
                        // Adiciona o jogo à lista especificada
                        val userGameListRef: DatabaseReference = userRef.child(listName).child(currentGame.id)
                        userGameListRef.setValue(true).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                callback("Jogo adicionado com sucesso à lista $listNamePretty")
                            } else {
                                callback("Erro ao adicionar jogo à lista $listNamePretty")
                            }
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Retorna erro
                callback("Erro ao acessar o banco de dados")
            }
        })
    }

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




    var completedGames = mutableStateOf<List<String>>(emptyList())
    var playingNow = mutableStateOf<List<String>>(emptyList())
    var wishList = mutableStateOf<List<String>>(emptyList())

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
