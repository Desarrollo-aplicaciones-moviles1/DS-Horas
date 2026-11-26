package com.example.deepsourcehoras.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.deepsourcehoras.data.preferences.SesionStorage
import com.example.deepsourcehoras.ui.screen.ConfirmacionScreen
import com.example.deepsourcehoras.ui.screen.HistorialScreen
import com.example.deepsourcehoras.ui.screen.HomeScreen
import com.example.deepsourcehoras.ui.screen.LoginScreen
import com.example.deepsourcehoras.ui.screen.MisProyectosScreen
import com.example.deepsourcehoras.ui.screen.RegistroHorasScreen
import com.example.deepsourcehoras.ui.screen.SplashScreen

@Composable
fun AppNavigation(abrirRegistro: Boolean = false, finalizarCronometro: Boolean = false) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Si se abre desde el widget y ya hay sesión, se salta el splash y el login
    val directoAlHome = remember { abrirRegistro && SesionStorage.haySesion(context) }
    var registroAbierto by rememberSaveable { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = if (directoAlHome) AppRoutes.HOME else AppRoutes.SPLASH,
        // Al avanzar la pantalla entra deslizando; al volver, sale hacia el otro lado
        enterTransition = { slideInHorizontally(tween(350)) { it / 4 } + fadeIn(tween(350)) },
        exitTransition = { slideOutHorizontally(tween(350)) { -it / 4 } + fadeOut(tween(350)) },
        popEnterTransition = { slideInHorizontally(tween(350)) { -it / 4 } + fadeIn(tween(350)) },
        popExitTransition = { slideOutHorizontally(tween(350)) { it / 4 } + fadeOut(tween(350)) }
    ) {
        composable(AppRoutes.SPLASH) {
            SplashScreen(
                onTerminado = {
                    // Con sesión guardada pasa directo al Home
                    val destino = if (SesionStorage.haySesion(context)) AppRoutes.HOME else AppRoutes.LOGIN
                    navController.navigate(destino) {
                        popUpTo(AppRoutes.SPLASH) { inclusive = true }
                    }
                    if (destino == AppRoutes.HOME && abrirRegistro) {
                        registroAbierto = true
                        navController.navigate(AppRoutes.registro(finalizarCronometro))
                    }
                }
            )
        }

        composable(AppRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                    // Si venía del widget, abre el formulario encima del Home
                    if (abrirRegistro && !registroAbierto) {
                        registroAbierto = true
                        navController.navigate(AppRoutes.registro(finalizarCronometro))
                    }
                }
            )
        }

        composable(AppRoutes.HOME) {
            HomeScreen(
                onRegistrarHoras = { navController.navigate(AppRoutes.registro()) },
                onVerProyectos = { navController.navigate(AppRoutes.MIS_PROYECTOS) },
                onVerRegistros = { navController.navigate(AppRoutes.HISTORIAL) },
                onCerrarSesion = {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = AppRoutes.REGISTRO,
            arguments = listOf(
                navArgument("finalizar") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("id") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            RegistroHorasScreen(
                finalizarAlAbrir = backStackEntry.arguments?.getBoolean("finalizar") ?: false,
                onBack = { navController.popBackStack() },
                onRegistrado = { resumen ->
                    if (resumen.editado) {
                        // Al editar se vuelve a "Mis registros", que ya muestra el cambio
                        navController.popBackStack()
                    } else {
                        // Va a la confirmación y saca el formulario de la pila
                        navController.navigate(
                            AppRoutes.confirmacion(
                                resumen.proyecto,
                                resumen.horas,
                                resumen.minutos,
                                resumen.dia,
                                resumen.foto
                            )
                        ) {
                            popUpTo(AppRoutes.REGISTRO) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(AppRoutes.MIS_PROYECTOS) {
            MisProyectosScreen(onBack = { navController.popBackStack() })
        }

        composable(AppRoutes.HISTORIAL) {
            HistorialScreen(
                onBack = { navController.popBackStack() },
                onEditar = { id -> navController.navigate(AppRoutes.editarRegistro(id)) }
            )
        }

        composable(
            route = AppRoutes.CONFIRMACION,
            arguments = listOf(
                navArgument("proyecto") { type = NavType.StringType },
                navArgument("horas") { type = NavType.IntType },
                navArgument("minutos") { type = NavType.IntType },
                navArgument("dia") { type = NavType.StringType },
                navArgument("foto") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            ConfirmacionScreen(
                proyecto = args?.getString("proyecto") ?: "",
                horas = args?.getInt("horas") ?: 0,
                minutos = args?.getInt("minutos") ?: 0,
                dia = args?.getString("dia") ?: "",
                foto = args?.getString("foto"),
                onVolverInicio = { navController.popBackStack() }
            )
        }
    }

    // Si se partió directo en el Home, el formulario se abre encima una sola vez
    LaunchedEffect(Unit) {
        if (directoAlHome && !registroAbierto) {
            registroAbierto = true
            navController.navigate(AppRoutes.registro(finalizarCronometro))
        }
    }
}
