package com.example.eyesofterapp.composeApp.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.eyesofterapp.composeApp.theme.EyesBandas
import com.example.eyesofterapp.composeApp.theme.EyesBotonPrincipal
import com.example.eyesofterapp.composeApp.theme.EyesBotonTexto
import com.example.eyesofterapp.composeApp.theme.EyesColors
import com.example.eyesofterapp.composeApp.theme.EyesLogo
import com.example.eyesofterapp.composeApp.theme.EyesMotion
import com.example.eyesofterapp.composeApp.theme.EyesShapes
import com.example.eyesofterapp.composeApp.theme.EyesTarjeta
import com.example.eyesofterapp.composeApp.theme.aparecer
import com.example.eyesofterapp.composeApp.theme.aparecerMarca
import com.example.eyesofterapp.ui.AuthUiState

/**
 * Pantalla de inicio de sesión. No sabe nada de red: solo muestra el estado
 * y avisa con onLogin.
 *
 * Diseño: arriba, un panel marino con las bandas diagonales de EYESOFTER Web
 * y, al centro, solo el logotipo y el nombre de la app. Abajo, el formulario
 * en una tarjeta blanca con borde fino.
 */
@Composable
fun LoginScreen(
    state: AuthUiState,
    onLogin: (username: String, password: String) -> Unit
) {
    // rememberSaveable: conserva lo escrito aunque se rote el teléfono
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Lo usan el botón y la tecla "Listo" del teclado
    fun submit() {
        focusManager.clearFocus()
        onLogin(username, password)
    }

    // Colores de los campos: borde fino que se vuelve azul al escribir.
    val coloresCampo = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = EyesColors.Azul,
        unfocusedBorderColor = EyesColors.Borde,
        focusedLabelColor = EyesColors.Azul,
        unfocusedLabelColor = EyesColors.TextoSecundario,
        cursorColor = EyesColors.Azul,
        focusedContainerColor = EyesColors.Blanco,
        unfocusedContainerColor = EyesColors.Blanco
    )

    // Los Modifier se aplican EN ORDEN, de arriba hacia abajo
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EyesColors.Fondo)
            .verticalScroll(rememberScrollState())
            .imePadding()               // sube el contenido cuando aparece el teclado
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Panel de arriba: las bandas diagonales de la web detrás del logo y el nombre.
        // Llega hasta el borde superior (detrás de la barra de estado).
        Box(
            modifier = Modifier.fillMaxWidth().height(340.dp),
            contentAlignment = Alignment.Center
        ) {
            // Animación de entrada (se ve una vez al abrir la app), en este orden:
            // bandas (fundido) → logo (rebote suave) → nombre → formulario.
            EyesBandas(Modifier.matchParentSize().aparecer(desplazamiento = 0.dp))
            Column(
                modifier = Modifier.statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Círculo blanco con anillo celeste, como el centro del anillo de la portada web.
                Box(
                    modifier = Modifier
                        .aparecerMarca(retrasoMs = 80)
                        .size(132.dp)
                        .clip(CircleShape)
                        .background(EyesColors.Blanco)
                        .border(7.dp, EyesColors.Celeste, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    EyesLogo(width = 84.dp)
                }
                // Nombre en mayúsculas con letras separadas, como la marca de la barra web.
                Text(
                    text = "EYESOFTER",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.18.em,
                    color = EyesColors.Blanco,
                    modifier = Modifier.aparecer(retrasoExtraMs = 180)
                )
            }
        }

        // El formulario va DEBAJO del panel azul, con aire entre los dos (no se encima).
        Spacer(Modifier.height(32.dp))
        EyesTarjeta(
            modifier = Modifier
                .aparecer(desplazamiento = 16.dp, retrasoExtraMs = 260)
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .widthIn(max = 420.dp)
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Usuario") },
                singleLine = true,
                isError = state.error != null,
                shape = EyesShapes.Boton,
                colors = coloresCampo,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                singleLine = true,
                isError = state.error != null,
                shape = EyesShapes.Boton,
                colors = coloresCampo,
                // Solo cambia CÓMO se ve (puntos); el texto real sigue en "password"
                visualTransformation = if (showPassword) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    EyesBotonTexto(
                        texto = if (showPassword) "Ocultar" else "Mostrar",
                        onClick = { showPassword = !showPassword },
                        color = EyesColors.Azul
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth()
            )

            // Solo aparece si el ViewModel puso un mensaje de error. Se despliega
            // (en vez de aparecer de golpe) para que el botón no "salte" hacia abajo.
            AnimatedVisibility(
                visible = state.error != null,
                enter = expandVertically(tween(EyesMotion.Entrada, easing = EyesMotion.EaseOut)) +
                    fadeIn(tween(EyesMotion.Entrada, easing = EyesMotion.EaseOut)),
                exit = shrinkVertically(tween(EyesMotion.Salida, easing = EyesMotion.EaseOut)) +
                    fadeOut(tween(EyesMotion.Salida))
            ) {
                Text(text = state.error ?: "", color = EyesColors.RojoBg, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(4.dp))

            // El único botón relleno de la pantalla. Mientras espera al
            // servidor muestra un indicador y no se puede volver a presionar.
            EyesBotonPrincipal(
                texto = "Iniciar sesión",
                onClick = { submit() },
                cargando = state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
