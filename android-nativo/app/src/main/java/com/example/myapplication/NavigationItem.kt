package com.example.myapplication

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.ui.graphics.vector.ImageVector

const val ARG_CODIGO_IBGE = "codigoIbge"

sealed class NavigationItem(val baseRoute: String, val title: String, val icon: ImageVector) {
    object Home : NavigationItem("home", "Home", Icons.Filled.Home)
    object Cidades : NavigationItem("cidades", "Cidades", Icons.Filled.LocationCity)
    object Nome : NavigationItem("nome", "Nome", Icons.Filled.Person) {
        
        val routeWithArg = "$baseRoute?$ARG_CODIGO_IBGE={$ARG_CODIGO_IBGE}" 
        
        fun createRoute(codigoIbge: String?) = "$baseRoute?$ARG_CODIGO_IBGE=${codigoIbge ?: ""}"
    }

    val route: String
        get() = if (this is Nome) this.routeWithArg else this.baseRoute
}
