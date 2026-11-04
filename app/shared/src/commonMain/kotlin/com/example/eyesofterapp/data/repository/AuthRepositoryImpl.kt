package com.example.eyesofterapp.data.repository

import com.example.eyesofterapp.data.model.LoginRequestDto
import com.example.eyesofterapp.data.model.toDomain
import com.example.eyesofterapp.data.network.AuthApi
import com.example.eyesofterapp.data.network.ResultadoRed
import com.example.eyesofterapp.domain.model.InvalidCredentialsException
import com.example.eyesofterapp.domain.model.Session
import com.example.eyesofterapp.domain.repository.AuthRepository

class AuthRepositoryImpl(private val api: AuthApi) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<Session> =
        loginConTimeout(username, password, 3000L)

    override suspend fun loginConTimeout(username: String, password: String, timeoutMs: Long): Result<Session> {
        return when (val resultado = api.login(LoginRequestDto(username, password), timeoutMs)) {
            is ResultadoRed.Exito -> Result.success(resultado.data.toDomain())
            // 401 = usuario o contraseña incorrectos: se usa la excepción del dominio para que
            // quien llame pueda distinguir este caso por su tipo (su mensaje es el mismo de antes).
            is ResultadoRed.ErrorCliente ->
                if (resultado.codigo == 401) Result.failure(InvalidCredentialsException())
                else Result.failure(Exception(resultado.mensaje))
            is ResultadoRed.ErrorServidor -> Result.failure(Exception(resultado.mensaje))
            is ResultadoRed.SinConexion -> Result.failure(Exception("Sin conexión a internet. Revisa tu red."))
            is ResultadoRed.ServidorInalcanzable -> Result.failure(Exception("No se puede conectar al servidor (${resultado.url})."))
            is ResultadoRed.Timeout -> Result.failure(Exception("Timeout al conectar con el servidor (${resultado.url})."))
            is ResultadoRed.Desconocido -> Result.failure(resultado.excepcion)
        }
    }
}
