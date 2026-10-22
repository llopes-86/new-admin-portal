package com.bodasmel.adminml

import android.app.Application
import androidx.test.core.app.ApplicationProvider

/** Atalho para o contexto da aplicação nos testes de captura de tela. */
object ApplicationProviderHolder {
  fun app(): AdminMlApp = ApplicationProvider.getApplicationContext<Application>() as AdminMlApp
}
