package com.bodasmel.adminml.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.onFocusChanged
import com.bodasmel.adminml.theme.InterFamily
import com.bodasmel.adminml.theme.PortalColors

/**
 * Campo de texto no mesmo estilo do portal web
 * (`.login-page .form-input` / `.form-input`): fundo translúcido, borda de 1px,
 * raio de 10px, altura compacta e anel de foco terracota.
 */
@Composable
fun PortalInput(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  placeholder: String = "",
  singleLine: Boolean = true,
  enabled: Boolean = true,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  textSize: TextUnit = 14.sp,
  trailing: (@Composable RowScope.() -> Unit)? = null,
) {
  var focused by remember { mutableStateOf(false) }
  val shape = RoundedCornerShape(10.dp)
  val borderColor = if (focused) PortalColors.Terracotta else PortalColors.LoginInputBorder
  val ringWidth = 3.dp

  Box(
    modifier =
      modifier
        .drawBehind {
          if (focused) {
            val w = ringWidth.toPx()
            drawRoundRect(
              color = PortalColors.LoginFocusRing,
              topLeft = Offset(-w, -w),
              size = Size(size.width + 2 * w, size.height + 2 * w),
              cornerRadius = CornerRadius(13.dp.toPx()),
            )
          }
        }
        .background(PortalColors.LoginInputBg, shape)
        .border(1.dp, borderColor, shape)
  ) {
    Row(
      modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 11.dp, bottom = 11.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(modifier = Modifier.weight(1f)) {
        if (value.isEmpty()) {
          Text(
            text = placeholder,
            style =
              LocalTextStyle.current.merge(
                TextStyle(
                  fontFamily = InterFamily,
                  fontSize = textSize,
                  color = PortalColors.LoginPlaceholder,
                )
              ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        BasicTextField(
          value = value,
          onValueChange = onValueChange,
          enabled = enabled,
          singleLine = singleLine,
          textStyle =
            TextStyle(
              fontFamily = InterFamily,
              fontSize = textSize,
              // O card do login é sempre claro (areia), então o texto é SEMPRE
              // escuro — independente do tema do app (o portal é o mesmo).
              color = PortalColors.LightText,
            ),
          keyboardOptions = keyboardOptions,
          visualTransformation = visualTransformation,
          cursorBrush = SolidColor(PortalColors.Terracotta),
          modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        )
      }
      trailing?.invoke(this)
    }
  }
}

/**
 * Campo de pesquisa compacto — paridade com `.search-wrapper` / `.search-input`
 * do portal (ícone de 16px à esquerda, altura de 44px, borda de 1px).
 */
@Composable
fun PortalSearchField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  placeholder: String = "Buscar…",
) {
  val shape = RoundedCornerShape(8.dp)
  var focused by remember { mutableStateOf(false) }
  val borderColor = if (focused) PortalColors.Terracotta else MaterialTheme.colorScheme.outline

  Box(
    modifier =
      modifier
        .height(44.dp)
        .background(MaterialTheme.colorScheme.surface, shape)
        .border(1.dp, borderColor, shape)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().height(44.dp).padding(start = 12.dp, end = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Filled.Search,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp),
      )
      Box(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
        if (value.isEmpty()) {
          Text(
            text = placeholder,
            fontFamily = InterFamily,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        BasicTextField(
          value = value,
          onValueChange = onValueChange,
          singleLine = true,
          textStyle =
            TextStyle(
              fontFamily = InterFamily,
              fontSize = 15.sp,
              color = MaterialTheme.colorScheme.onSurface,
            ),
          cursorBrush = SolidColor(PortalColors.LightPrimary),
          modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        )
      }
      if (value.isNotEmpty()) {
        Icon(
          imageVector = Icons.Filled.Close,
          contentDescription = "Limpar busca",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier =
            Modifier
              .size(28.dp)
              .clickable { onValueChange("") }
              .padding(6.dp),
        )
      }
    }
  }
}
