package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainNavigationScreen()
            }
        }
    }
}

@Composable
fun AppHomeScreenContent(modifier: Modifier = Modifier) { 
    Card(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo), 
                contentDescription = "Logo do aplicativo Civis+",
                modifier = Modifier.size(100.dp)
            )
            Text(
                text = "Bem-vindo ao seu guia da cidade!",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Explore informações detalhadas sobre sua localização atual com apenas um toque.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScreen() {
    val navController: NavHostController = rememberNavController()
    val navItems = listOf(NavigationItem.Home, NavigationItem.Cidades, NavigationItem.Nome)
    var currentIbgeCode by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                navItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { navDest -> 
                            val compareRoute = if (screen is NavigationItem.Nome) screen.baseRoute else screen.route
                            navDest.route?.startsWith(compareRoute) == true
                        } == true,
                        onClick = {
                            val routeToNavigate = if (screen is NavigationItem.Nome) {
                                NavigationItem.Nome.createRoute(currentIbgeCode)
                            } else {
                                screen.route
                            }
                            navController.navigate(routeToNavigate) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = screen !is NavigationItem.Nome 
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavigationItem.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(NavigationItem.Home.route) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppHomeScreenContent()
                }
            }
            composable(NavigationItem.Cidades.route) {
                CidadesScreenContent(
                    modifier = Modifier.fillMaxSize(),
                    onIbgeFetched = { ibge -> 
                        currentIbgeCode = ibge 
                    }
                )
            }
            composable(
                route = NavigationItem.Nome.route, 
                arguments = listOf(navArgument(ARG_CODIGO_IBGE) { 
                    type = NavType.StringType 
                    nullable = true 
                })
            ) { backStackEntry ->
                val ibgeCodeFromNav = backStackEntry.arguments?.getString(ARG_CODIGO_IBGE)
                NomeScreen(
                    modifier = Modifier.fillMaxSize(), 
                    codigoIbge = ibgeCodeFromNav
                )
            }
        }
    }
}
