package com.example.eyesofterapp

import com.example.eyesofterapp.data.cache.AlmacenLocal
import com.example.eyesofterapp.data.network.AuthApi
import com.example.eyesofterapp.data.network.EyesApi
import com.example.eyesofterapp.data.network.HttpClientFactory
import com.example.eyesofterapp.data.network.OpticasApi
import com.example.eyesofterapp.data.repository.AuthRepositoryImpl
import com.example.eyesofterapp.data.session.SesionGuardada
import com.example.eyesofterapp.domain.model.User
import com.example.eyesofterapp.domain.repository.AuthRepository
import com.example.eyesofterapp.domain.usecase.LoginUseCase
import com.example.eyesofterapp.location.LocationProvider
import com.example.eyesofterapp.ui.OpticasViewModel
import com.example.eyesofterapp.ui.PanelViewModel
import com.example.eyesofterapp.ui.RecuperacionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Crea y conecta los objetos de la app (cliente HTTP, APIs, ViewModels). */
class AppContainer(
    val locationProvider: LocationProvider? = null
) {
    private val httpClient = HttpClientFactory.create()
    private val authRepository: AuthRepository = AuthRepositoryImpl(AuthApi(httpClient))

    val sesionGuardada = SesionGuardada()
    val loginUseCase = LoginUseCase(authRepository)
    val opticasApi = OpticasApi(httpClient) { sesionGuardada.leer()?.token }
    val almacen = AlmacenLocal()

    /** Sin sesión: para reportar una falla desde el login (C-RS-04). */
    val apiSinSesion = EyesApi(httpClient, token = { null }, alVencerSesion = {})

    /** "¿Olvidaste tu contraseña?" (sin sesión). Se cierra al salir de la pantalla. */
    fun crearRecuperacion(): RecuperacionViewModel =
        RecuperacionViewModel(apiSinSesion, CoroutineScope(SupervisorJob() + Dispatchers.Main))

    fun crearOpticasViewModel(): OpticasViewModel =
        OpticasViewModel(opticasApi, locationProvider)

    /** Un panel por sesión: se crea al entrar y se cierra al salir. */
    fun crearPanel(usuario: User, token: () -> String?, alVencerSesion: () -> Unit): PanelViewModel {
        var panel: PanelViewModel? = null
        val api = EyesApi(httpClient, token, alVencerSesion, alPedirConsentimiento = { panel?.pedirConsentimiento() })
        return PanelViewModel(
            api = api,
            rol = usuario.role,
            usuarioId = usuario.id.ifBlank { usuario.username },
            almacen = almacen,
            viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
            requiereConsentimiento = usuario.requiereConsentimiento,
        ).also { panel = it }
    }
}
