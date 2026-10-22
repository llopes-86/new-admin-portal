package com.bodasmel.adminml.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.bodasmel.adminml.R

/**
 * Fontes do portal web (`public/css/fonts.css`):
 * - **Inter** → `--font-sans`, usada em todo o texto do portal
 * - **Playfair Display** → `--font-display`, usada no título do login
 *
 * Os arquivos TTF foram gerados a partir dos mesmos woff2 servidos pelo portal
 * (subconjuntos latin e latin-ext do Inter + Playfair Display), instanciados nos
 * pesos 400/500/600/700.
 */
val InterFamily =
  FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    // latin-ext (nomes com acentos da Europa central) — usado como fallback
    Font(R.font.inter_ext_regular, FontWeight.Normal),
    Font(R.font.inter_ext_medium, FontWeight.Medium),
    Font(R.font.inter_ext_semibold, FontWeight.SemiBold),
    Font(R.font.inter_ext_bold, FontWeight.Bold),
  )

val PlayfairFamily =
  FontFamily(
    Font(R.font.playfair_display_semibold, FontWeight.SemiBold),
    Font(R.font.playfair_display_bold, FontWeight.Bold),
  )
