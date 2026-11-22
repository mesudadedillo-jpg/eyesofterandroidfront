package com.example.eyesofterapp.composeApp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.eyesofterapp.composeApp.theme.EyesBarraPantalla
import com.example.eyesofterapp.composeApp.theme.EyesBotonPrincipal
import com.example.eyesofterapp.composeApp.theme.EyesBotonTexto
import com.example.eyesofterapp.composeApp.theme.EyesColors
import com.example.eyesofterapp.composeApp.theme.EyesShapes
import com.example.eyesofterapp.composeApp.theme.EyesTarjeta
import com.example.eyesofterapp.ui.PasoRecuperacion
import com.example.eyesofterapp.ui.RecuperacionUiState

/**
 * "¿Olvidaste tu contraseña?" (C-RFNN-02). Dos pasos: correo o celular → código + contraseña nueva.
 * No sabe nada de red: muestra el estado y avisa con los callbacks.
 */
@Composable
fun RecuperarPasswordScreen(
    state: RecuperacionUiState,
    onPedirCodigo: (cuenta: String) -> Unit,
    onReenviar: () -> Unit,
    onRestablecer: (codigo: String, password: String, confirmar: String) -> Unit,
    onCambiarCuenta: () -> Unit,
    onVolver: () -> Unit,
) {
    val colores = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = EyesColors.Azul,
        unfocusedBorderColor = EyesColors.Borde,
        focusedLabelColor = EyesColors.Azul,
        unfocusedLabelColor = EyesColors.TextoSecundario,
        cursorColor = EyesColors.Azul,
        focusedContainerColor = EyesColors.Blanco,
        unfocusedContainerColor = EyesColors.Blanco
    )
    val foco = LocalFocusManager.current

    Column(Modifier.fillMaxSize().background(EyesColors.Fondo)) {
        EyesBarraPantalla("Recuperar contraseña", onVolver)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EyesTarjeta(Modifier.fillMaxWidth().widthIn(max = 420.dp)) {
                when (state.paso) {
                    PasoRecuperacion.Cuenta -> {
                        var cuenta by rememberSaveable { mutableStateOf(state.cuenta) }
                        Text("¿Olvidaste tu contraseña?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EyesColors.Marino)
                        Text(
                            "Escribe el correo de tu cuenta o tu celular y te enviaremos un código de 6 dígitos para crear una contraseña nueva.",
                            style = MaterialTheme.typography.bodyMedium, color = EyesColors.TextoSecundario
                        )
                        OutlinedTextField(
                            value = cuenta,
                            onValueChange = { cuenta = it.take(120) },
                            label = { Text("Correo electrónico o celular") },
                            supportingText = { Text("El celular es el que la escuela tiene registrado del tutor.") },
                            singleLine = true,
                            shape = EyesShapes.Boton,
                            colors = colores,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { foco.clearFocus(); onPedirCodigo(cuenta) }),
                            modifier = Modifier.fillMaxWidth()
                        )
                        MensajeError(state.error)
                        EyesBotonPrincipal(
                            "Enviar código", onClick = { foco.clearFocus(); onPedirCodigo(cuenta) },
                            enabled = cuenta.isNotBlank(), cargando = state.enviando, modifier = Modifier.fillMaxWidth()
                        )
                    }

                    PasoRecuperacion.Codigo -> {
                        var codigo by remember { mutableStateOf("") }
                        var password by remember { mutableStateOf("") }
                        var confirmar by remember { mutableStateOf("") }
                        var ver by remember { mutableStateOf(false) }
                        val transformacion = if (ver) VisualTransformation.None else PasswordVisualTransformation()
                        Text("Escribe tu código", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EyesColors.Marino)
                        state.aviso?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = EyesColors.TextoSecundario)
                        }
                        Text("Si no llega, revisa la carpeta de spam.", style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoTenue)
                        OutlinedTextField(
                            value = codigo,
                            onValueChange = { codigo = it.filter(Char::isDigit).take(6) },
                            label = { Text("Código de 6 dígitos") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.3.em, textAlign = TextAlign.Center),
                            shape = EyesShapes.Boton,
                            colors = colores,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { foco.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it.take(64) },
                            label = { Text("Nueva contraseña") },
                            supportingText = { Text("Mínimo 8 caracteres, con mayúscula, minúscula y número.") },
                            singleLine = true,
                            visualTransformation = transformacion,
                            trailingIcon = { EyesBotonTexto(if (ver) "Ocultar" else "Mostrar", onClick = { ver = !ver }, color = EyesColors.Azul) },
                            shape = EyesShapes.Boton,
                            colors = colores,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { foco.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = confirmar,
                            onValueChange = { confirmar = it.take(64) },
                            label = { Text("Confirmar contraseña") },
                            singleLine = true,
                            visualTransformation = transformacion,
                            shape = EyesShapes.Boton,
                            colors = colores,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { foco.clearFocus(); onRestablecer(codigo, password, confirmar) }),
                            modifier = Modifier.fillMaxWidth()
                        )
                        MensajeError(state.error)
                        EyesBotonPrincipal(
                            "Guardar contraseña", onClick = { foco.clearFocus(); onRestablecer(codigo, password, confirmar) },
                            enabled = codigo.length == 6 && password.isNotEmpty() && confirmar.isNotEmpty(),
                            cargando = state.enviando, modifier = Modifier.fillMaxWidth()
                        )
                        EyesBotonTexto(
                            if (state.reenviarEn > 0) "Enviar otro código en ${state.reenviarEn} s" else "No me llegó: enviar otro código",
                            onClick = onReenviar, enabled = state.reenviarEn == 0 && !state.enviando,
                            color = EyesColors.Azul, modifier = Modifier.fillMaxWidth()
                        )
                        EyesBotonTexto("Usar otro correo o celular", onClick = onCambiarCuenta, modifier = Modifier.fillMaxWidth())
                    }

                    PasoRecuperacion.Listo -> {
                        Text("¡Listo!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EyesColors.Marino)
                        Text(
                            "Tu contraseña se cambió. Ya puedes iniciar sesión con la nueva, en la app y en el portal. " +
                                "Por seguridad, se cerraron las sesiones que tenías abiertas.",
                            style = MaterialTheme.typography.bodyMedium, color = EyesColors.TextoSecundario
                        )
                        EyesBotonPrincipal("Iniciar sesión", onClick = onVolver, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun MensajeError(texto: String?) {
    if (texto == null) return
    Text(
        texto,
        color = EyesColors.RojoBg,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    )
}
