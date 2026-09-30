package ir.bedehyar.app.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ir.bedehyar.app.TxViewModel
import ir.bedehyar.app.ui.screen.EditScreen
import ir.bedehyar.app.ui.screen.HomeScreen
import ir.bedehyar.app.ui.screen.PermissionsScreen
import ir.bedehyar.app.ui.screen.PersonScreen

@Composable
fun AppRoot(vm: TxViewModel) {
    val nav = rememberNavController()
    val start = remember { if (vm.onboardingDone) "home" else "perms" }

    NavHost(navController = nav, startDestination = start) {
        composable("home") {
            HomeScreen(
                vm = vm,
                onOpenPerson = { name -> nav.navigate("person/${Uri.encode(name)}") },
                onAdd = { nav.navigate("edit/-1") },
                onOpenPerms = { nav.navigate("perms") }
            )
        }
        composable("perms") {
            PermissionsScreen(
                vm = vm,
                onDone = {
                    nav.navigate("home") {
                        popUpTo("perms") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("person/{name}") { entry ->
            val name = entry.arguments?.getString("name") ?: ""
            PersonScreen(
                vm = vm,
                personName = name,
                onBack = { nav.popBackStack() },
                onEdit = { id -> nav.navigate("edit/$id") }
            )
        }
        composable("edit/{id}") { entry ->
            val id = entry.arguments?.getString("id")?.toLongOrNull() ?: -1L
            EditScreen(vm = vm, txId = id, onBack = { nav.popBackStack() })
        }
    }
}
