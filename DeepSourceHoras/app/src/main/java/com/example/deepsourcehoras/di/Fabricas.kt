package com.example.deepsourcehoras.di

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.deepsourcehoras.ui.viewmodel.HistorialViewModel
import com.example.deepsourcehoras.ui.viewmodel.HomeViewModel
import com.example.deepsourcehoras.ui.viewmodel.LoginViewModel
import com.example.deepsourcehoras.ui.viewmodel.MisProyectosViewModel
import com.example.deepsourcehoras.ui.viewmodel.RegistroHorasViewModel

// Fábricas que crean cada ViewModel con sus dependencias reales
object Fabricas {

    val login = viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY]!!
            LoginViewModel(Dependencias.authRepository, Dependencias.sesion(app), Dependencias.recursos(app))
        }
    }

    val home = viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY]!!
            HomeViewModel(Dependencias.horasRepository(app), Dependencias.sesion(app))
        }
    }

    val misProyectos = viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY]!!
            val sesion = Dependencias.sesion(app)
            MisProyectosViewModel(Dependencias.horasRepository(app), Dependencias.recursos(app)) { sesion.usuarioId() }
        }
    }

    val historial = viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY]!!
            HistorialViewModel(Dependencias.horasRepository(app), Dependencias.recursos(app))
        }
    }

    val registro = viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY]!!
            // El id llega como argumento de navegación ("registro?id=5"); -1 = registro nuevo
            val id = createSavedStateHandle().get<Long>("id")?.takeIf { it > 0 }
            RegistroHorasViewModel(
                registroId = id,
                repositorio = Dependencias.horasRepository(app),
                feriados = Dependencias.feriadosRepository,
                recursos = Dependencias.recursos(app),
                cronometro = Dependencias.cronometro(app)
            )
        }
    }
}
