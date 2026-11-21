package com.example.eyesofterapp.ui

import com.example.eyesofterapp.domain.model.Session

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val session: Session? = null,
    /** La cuenta tiene verificación en dos pasos: se muestra el campo del código. */
    val pideCodigo: Boolean = false,
)