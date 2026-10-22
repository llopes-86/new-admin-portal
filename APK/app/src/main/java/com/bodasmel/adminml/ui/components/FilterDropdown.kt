package com.bodasmel.adminml.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodasmel.adminml.theme.InterFamily

/**
 * Filtro suspenso compacto — paridade com os `<select>` do portal
 * (rótulo exibido como "Pendentes (3)", altura de 44px, borda de 1px).
 */
@Composable
fun FilterDropdown(
  label: String,
  options: List<Pair<String, String>>,
  selected: String,
  counts: Map<String, Int>,
  onSelect: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: label
  val selectedCount = counts[selected]
  val display =
    if (selectedCount != null && selected != "all") "$selectedLabel ($selectedCount)" else selectedLabel

  val shape = RoundedCornerShape(8.dp)

  Box(modifier) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(44.dp)
          .background(MaterialTheme.colorScheme.surface, shape)
          .border(1.dp, MaterialTheme.colorScheme.outline, shape)
          .clickable { expanded = true }
          .padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = label,
        fontFamily = InterFamily,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = " · ",
        fontFamily = InterFamily,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = display,
        fontFamily = InterFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(Modifier.weight(1f))
      Icon(
        imageVector = Icons.Filled.ArrowDropDown,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
      )
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { (value, text) ->
        val count = counts[value]
        DropdownMenuItem(
          text = {
            Text(
              text = if (count != null && value != "all") "$text ($count)" else text,
              fontFamily = InterFamily,
            )
          },
          onClick = {
            onSelect(value)
            expanded = false
          },
        )
      }
    }
  }
}
