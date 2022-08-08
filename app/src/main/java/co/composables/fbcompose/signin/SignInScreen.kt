package co.composables.fbcompose.signin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Facebook
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import co.composables.fbcompose.R
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun SignInScreen(
    navigateToHome: () -> Unit,
) {
    val context = LocalContext.current
    Box(Modifier
        .background(MaterialTheme.colors.surface)
        .fillMaxSize()) {

        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Facebook, contentDescription = null,
                modifier = Modifier.size(90.dp),
                tint = MaterialTheme.colors.primary
            )
            Spacer(Modifier.height(20.dp))
            SigninButton(
                onSignedIn = navigateToHome,
                onSignInFailed = {
                    Toast.makeText(context,
                        context.getString(R.string.try_again),
                        Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun SigninButton(
    onSignInFailed: (Exception) -> Unit,
    onSignedIn: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    AndroidView({ context ->
        LoginButton(context).apply {
            val callbackManager = CallbackManager.Factory.create()
            setPermissions("email", "public_profile")
            registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onCancel() {
                    // do nothing
                }

                override fun onError(error: FacebookException) {
                    onSignInFailed(error)
                }

                override fun onSuccess(result: LoginResult) {
                    scope.launch {
                        val token = result.accessToken.token
                        val credential = FacebookAuthProvider.getCredential(token)
                        val authResult = Firebase.auth.signInWithCredential(credential).await()
                        if (authResult.user != null) {
                            onSignedIn()
                        } else {
                            onSignInFailed(RuntimeException("Could not sign in with Firebase"))
                        }
                    }
                }
            })
        }
    })
}
