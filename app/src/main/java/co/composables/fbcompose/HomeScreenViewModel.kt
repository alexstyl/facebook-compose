package co.composables.fbcompose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.AccessToken
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

sealed class HomeScreenState {
    object Loading : HomeScreenState()
    data class Loaded(
        val avatarUrl: String,
        val posts: List<Post>,
    ) : HomeScreenState()

    object SignInRequired : HomeScreenState()
}

class HomeScreenViewModel : ViewModel() {
    private val mutableState = MutableStateFlow<HomeScreenState>(
        HomeScreenState.Loading
    )
    val state = mutableState.asStateFlow()

    val textState = MutableStateFlow("")

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUser = Firebase.auth.currentUser
            if (currentUser != null) {
                observePosts(currentUser)
            } else {
                mutableState.emit(
                    HomeScreenState.SignInRequired
                )
            }
        }
    }

    private suspend fun observePosts(currentUser: FirebaseUser) {
        observePosts().map { posts ->
            HomeScreenState.Loaded(
                avatarUrl = getAvatar(currentUser),
                posts = posts
            )
        }.collect {
            mutableState.emit(it)
        }
    }

    private fun observePosts(): Flow<List<Post>> {
        return callbackFlow {
            val listener = Firebase.firestore
                .collection("posts").addSnapshotListener { value, error ->
                    if (error != null) {
                        close(error)
                    } else if (value != null) {
                        val posts = value.map { doc ->
                            Post(
                                text = doc.getString("text").orEmpty(),
                                timestamp = doc.getDate("date_posted") ?: Date(),
                                authorName = doc.getString("author_name").orEmpty(),
                                authorAvatarUrl = doc.getString("author_avatar_url").orEmpty()
                            )
                        }.sortedByDescending { it.timestamp }
                        trySend(posts)
                    }
                }
            awaitClose {
                listener.remove()
            }
        }
    }

    private fun getAvatar(currentUser: FirebaseUser): String {
        val accessToken = AccessToken.getCurrentAccessToken()?.token
        return "${requireNotNull(currentUser.photoUrl)}?access_token=$accessToken&type=large"
    }

    fun onTextChanged(text: String) {
        viewModelScope.launch {
            textState.emit(text)
        }
    }

    fun onSendClick() {
        viewModelScope.launch(Dispatchers.IO) {
            val postText = textState.value
            val currentUser = requireNotNull(Firebase.auth.currentUser) {
                "Tried to create post without a signed in user"
            }
            Firebase.firestore.collection("posts")
                .add(
                    hashMapOf(
                        "text" to postText,
                        "date_posted" to Date(),
                        "author_name" to currentUser.displayName.orEmpty(),
                        "author_avatar_url" to getAvatar(currentUser)
                    ))
        }
    }
}
