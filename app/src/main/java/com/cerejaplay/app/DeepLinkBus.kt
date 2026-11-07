package com.cerejaplay.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ponte entre o Intent que abriu o app (deep link) e a navegação Compose.
 * A rota fica pendente até a UI consumi-la, evitando perder o evento caso a
 * navegação ainda não esteja pronta.
 */
object DeepLinkBus {

    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun emit(route: String) {
        _pending.value = route
    }

    fun consume() {
        _pending.value = null
    }
}
