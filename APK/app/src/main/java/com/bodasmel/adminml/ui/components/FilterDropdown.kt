package com.bodasmel.adminml.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Filtro suspenso com contadores — paridade com os `<select>` do portal
 * (rótulo exibido como "Pendentes (3)").
 */
@OptIn(ExperimentalMaterial3Api::class)
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

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    modifier = modifier,
  ) {
    OutlinedTextField(
      value = display,
      onValueChange = {},
      readOnly = true,
      label = { Text(label) },
      modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      // Ergonomia de toque igual à web no mobile (16px evita auto-zoom, 44px+ de altura)
      textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
      shape = RoundedCornerShape(6.dp),
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { (value, text) ->
        val count = counts[value]
        DropdownMenuItem(
          text = { Text(if (count != null && value != "all") "$text ($count)" else text) },
          onClick = {
            onSelect(value)
            expanded = false
          },
        )
      }
    }
  }
}
