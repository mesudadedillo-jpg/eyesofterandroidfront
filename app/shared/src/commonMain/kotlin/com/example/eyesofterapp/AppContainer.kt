package com.example.eyesofterapp

import com.example.eyesofterapp.data.network.AuthApi
import com.example.eyesofterapp.data.network.HttpClientFactory
import com.example.eyesofterapp.data.network.OpticasApi
import com.example.eyesofterapp.data.repository.AuthRepositoryImpl
import com.example.eyesofterapp.domain.repository.AuthRepository
import com.example.eyesofterapp.domain.usecase.LoginUseCase
import com.example.eyesofterapp.location.LocationProvider
import com.example.eyesofterapp.ui.OpticasViewModel

class AppContainer(
    val locationProvider: LocationProvider? = null
) {
    private val httpClient = HttpClientFactory.create()
    private val authRepository: AuthRepository = AuthRepositoryImpl(AuthApi(httpClient))

    val loginUseCase = LoginUseCase(authRepository)
    val opticasApi = OpticasApi(httpClient)

    fun crearOpticasViewModel(): OpticasViewModel =
        OpticasViewModel(opticasApi, locationProvider)
}
