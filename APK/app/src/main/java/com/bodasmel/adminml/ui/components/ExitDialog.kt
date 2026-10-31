package com.bodasmel.adminml.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

/** Diálogo de confirmação reutilizado nas saídas do app e da conta. */
@Composable
fun ExitDialog(
  title: String,
  text: String,
  confirmText: String = "Confirmar",
  cancelText: String = "Cancelar",
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title, fontWeight = FontWeight.SemiBold) },
    text = { Text(text) },
    confirmButton = {
      TextButton(onClick = onConfirm) { Text(confirmText) }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(cancelText) }
    },
  )
}
