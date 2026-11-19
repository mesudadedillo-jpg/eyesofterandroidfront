package com.example.eyesofterapp

import com.example.eyesofterapp.data.network.AuthApi
import com.example.eyesofterapp.data.network.EyesApi
import com.example.eyesofterapp.data.network.HttpClientFactory
import com.example.eyesofterapp.data.network.OpticasApi
import com.example.eyesofterapp.data.repository.AuthRepositoryImpl
import com.example.eyesofterapp.data.session.SesionGuardada
import com.example.eyesofterapp.domain.model.Role
import com.example.eyesofterapp.domain.repository.AuthRepository
import com.example.eyesofterapp.domain.usecase.LoginUseCase
import com.example.eyesofterapp.location.LocationProvider
import com.example.eyesofterapp.ui.OpticasViewModel
import com.example.eyesofterapp.ui.PanelViewModel
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
    val opticasApi = OpticasApi(httpClient)

    fun crearOpticasViewModel(): OpticasViewModel =
        OpticasViewModel(opticasApi, locationProvider)

    /** Un panel por sesión: se crea al entrar y se cierra al salir. */
    fun crearPanel(rol: Role, token: () -> String?, alVencerSesion: () -> Unit): PanelViewModel =
        PanelViewModel(
            api = EyesApi(httpClient, token, alVencerSesion),
            rol = rol,
            viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
        )
}
