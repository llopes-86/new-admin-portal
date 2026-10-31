package com.bodasmel.adminml

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bodasmel.adminml.core.AuthApi
import com.bodasmel.adminml.core.AuthState
import com.bodasmel.adminml.core.ChangePasswordResult
import com.bodasmel.adminml.core.LoginResult
import com.bodasmel.adminml.core.SecureStore
import com.bodasmel.adminml.core.SessionManager
import com.bodasmel.adminml.core.UnlockResult
import com.bodasmel.adminml.data.ApiException
import com.bodasmel.adminml.data.AuthResponse
import com.bodasmel.adminml.data.UserRecord
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SessionManagerTest {

  private val context = ApplicationProvider.getApplicationContext<Application>()

  private val prefs
    get() = context.getSharedPreferences("adminml_secure", Context.MODE_PRIVATE)

  @Before
  fun setUp() {
    prefs.edit().clear().commit()
  }

  @Test
  fun `cold start fica SignedOut mesmo com token e usuario gravados`() {
    val store = SecureStore(context)
    store.pbToken = "token-antigo"
    store.pbUserJson = """{"id":"user-1","email":"admin@bodasmel.com.br"}"""

    val session = SessionManager(store, FakeAuthApi())

    assertEquals(AuthState.SignedOut, session.state.value)
    assertNull(session.currentUser)
  }

  @Test
  fun `login lembra apenas o e-mail e nunca a senha`() = runBlocking {
    val store = SecureStore(context)
    val session = SessionManager(store, FakeAuthApi())

    val result = session.login("admin@bodasmel.com.br", "senha-super-secreta", remember = true)

    assertTrue(result is LoginResult.Success)
    assertEquals("admin@bodasmel.com.br", store.rememberedEmail)
    assertEquals("token-1", store.pbToken)
    assertTrue(store.rememberCredentials)
    assertFalse(prefsContains("senha-super-secreta"))
    assertFalse(prefs.contains("remember_password"))
  }

  @Test
  fun `changePassword nao grava a nova senha`() = runBlocking {
    val store = SecureStore(context)
    val fake = FakeAuthApi()
    val session = SessionManager(store, fake)
    session.login(USER.email, "senha-antiga", remember = true)

    val result = session.changePassword("senha-antiga", "nova-super-secreta")

    assertTrue(result is ChangePasswordResult.Success)
    assertEquals(USER.email, store.rememberedEmail)
    assertFalse(prefsContains("nova-super-secreta"))
    assertFalse(prefs.contains("remember_password"))
  }

  @Test
  fun `lock mantem o token e bloqueia a sessao`() = runBlocking {
    val store = SecureStore(context)
    val session = SessionManager(store, FakeAuthApi())
    session.login(USER.email, "senha123", remember = true)
    val token = store.pbToken

    session.lock()

    val state = session.state.value
    assertTrue(state is AuthState.Locked)
    assertEquals(USER.email, (state as AuthState.Locked).user.email)
    assertEquals(token, store.pbToken)
  }

  @Test
  fun `lock so funciona a partir de SignedIn`() {
    val session = SessionManager(SecureStore(context), FakeAuthApi())

    session.lock()
    assertFalse(session.unlockWithBiometrics())

    assertEquals(AuthState.SignedOut, session.state.value)
  }

  @Test
  fun `unlockWithPassword valida a senha e renova o token`() = runBlocking {
    val store = SecureStore(context)
    val fake = FakeAuthApi()
    val session = SessionManager(store, fake)
    session.login(USER.email, "senha123", remember = true)
    session.lock()
    fake.response = AuthResponse("token-2", USER.copy(name = "Admin 2"))

    val result = session.unlockWithPassword("senha123")

    assertEquals(UnlockResult.Success, result)
    assertTrue(session.state.value is AuthState.SignedIn)
    assertEquals("token-2", store.pbToken)
    assertEquals(USER.email to "senha123", fake.calls.last())
  }

  @Test
  fun `unlockWithPassword com senha errada mantem a sessao bloqueada`() = runBlocking {
    val store = SecureStore(context)
    val fake = FakeAuthApi()
    val session = SessionManager(store, fake)
    session.login(USER.email, "senha123", remember = true)
    session.lock()
    val token = store.pbToken
    fake.failure = ApiException(400, "Failed to authenticate.")

    val result = session.unlockWithPassword("senha-errada")

    assertEquals("Senha incorreta. Tente novamente.", (result as UnlockResult.Error).message)
    assertTrue(session.state.value is AuthState.Locked)
    assertEquals(token, store.pbToken)
  }

  @Test
  fun `logout limpa token e usuario e preserva o e-mail lembrado`() = runBlocking {
    val store = SecureStore(context)
    val session = SessionManager(store, FakeAuthApi())
    session.login(USER.email, "senha123", remember = true)

    session.logout()

    assertEquals(AuthState.SignedOut, session.state.value)
    assertNull(store.pbToken)
    assertNull(store.pbUserJson)
    assertEquals(USER.email, store.rememberedEmail)
    assertTrue(store.rememberCredentials)
  }

  @Test
  fun `expireSession limpa a sessao e preserva o e-mail lembrado`() = runBlocking {
    val store = SecureStore(context)
    val session = SessionManager(store, FakeAuthApi())
    session.login(USER.email, "senha123", remember = true)

    session.expireSession()

    assertEquals(AuthState.SignedOut, session.state.value)
    assertNull(store.pbToken)
    assertNull(store.pbUserJson)
    assertEquals(USER.email, store.rememberedEmail)
  }

  @Test
  fun `clearLegacyRememberedPassword remove a chave antiga`() {
    prefs.edit().putString("remember_password", "senha-antiga").commit()
    val store = SecureStore(context)

    store.clearLegacyRememberedPassword()

    assertFalse(prefs.contains("remember_password"))
  }

  private fun prefsContains(value: String): Boolean =
    prefs.all.values.any { it is String && it.contains(value) }

  private class FakeAuthApi : AuthApi {
    var response = AuthResponse("token-1", USER)
    var failure: Exception? = null
    val calls = mutableListOf<Pair<String, String>>()

    override suspend fun authWithPassword(email: String, password: String): AuthResponse {
      calls += email to password
      failure?.let { throw it }
      return response
    }

    override suspend fun changePassword(
      userId: String,
      oldPassword: String,
      newPassword: String,
    ): UserRecord = response.record
  }

  private companion object {
    val USER = UserRecord(id = "user-1", email = "admin@bodasmel.com.br", name = "Admin")
  }
}
