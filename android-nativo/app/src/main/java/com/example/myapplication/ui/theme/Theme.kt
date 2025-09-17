package com.example.myapplication.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color // Adicionado para Color.White/Black
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CivisVerdeAguaForte,
    onPrimary = CivisFundoClaro,
    secondary = CivisVerdeAzuladoMedio,
    onSecondary = CivisFundoClaro,
    tertiary = CivisVerdeLimas,
    onTertiary = CivisVerdePetroleoEscuro, // Texto escuro em verde limão
    background = Color(0xFF121212), // Fundo escuro padrão
    onBackground = CivisFundoClaro,
    surface = CivisVerdePetroleoEscuro, // Superfície com cor da marca
    onSurface = CivisFundoClaro,
    error = Color(0xFFCF6679), // Vermelho de erro para tema escuro
    onError = Color.Black,
    surfaceVariant = Color(0xFF252525), // Um cinza escuro para variantes de superfície
    onSurfaceVariant = CivisVerdeAcinzentadoClaro,
    outline = CivisVerdeAzuladoMedio
)

private val LightColorScheme = lightColorScheme(
    primary = CivisVerdePetroleoEscuro,
    onPrimary = CivisFundoClaro,
    secondary = CivisVerdeAguaForte,
    onSecondary = CivisFundoClaro,
    tertiary = CivisVerdeAzuladoMedio,
    onTertiary = CivisFundoClaro,
    background = CivisFundoClaro,
    onBackground = CivisVerdePetroleoEscuro,
    surface = CivisFundoClaro,
    onSurface = CivisVerdePetroleoEscuro,
    error = Color(0xFFB00020), // Vermelho de erro padrão
    onError = Color.White,
    surfaceVariant = CivisVerdeAcinzentadoClaro,
    onSurfaceVariant = CivisVerdePetroleoEscuro,
    outline = CivisVerdeAzuladoMedio
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Manter a opção de cor dinâmica
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb() // Pode ajustar para background se preferir
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Seu arquivo Typography.kt existente
        content = content
    )
}