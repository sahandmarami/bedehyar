package ir.bedehyar.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.bedehyar.app.MainActivity
import ir.bedehyar.app.PermissionsAndGuide
import ir.bedehyar.app.data.AppSettings
import ir.bedehyar.app.ui.home.HomeScreen
import ir.bedehyar.app.ui.person.PersonScreen
import ir.bedehyar.app.ui.reports.ReportsScreen
import ir.bedehyar.app.ui.settings.SettingsScreen
import ir.bedehyar.app.ui.tx.EditScreen
import ir.bedehyar.app.ui.tx.TransactionDetailScreen

/** Application navigation graph. All screens are Persian/RTL. */
@Composable
fun AppRoot(startRoute: String?, settings: AppSettings) {
    val nav = rememberNavController()

    // Deep-entry (e.g. from the alarm screen): land on home first so Back works.
    LaunchedEffect(startRoute) {
        if (!startRoute.isNullOrBlank()) {
            val r = if (startRoute.startsWith("tx/") || startRoute.startsWith("person/")) {
                startRoute
            } else {
                when (startRoute) {
                    "reports", "settings" -> startRoute
                    else -> "home"
                }
            }
            if (r != "home") nav.navigate(r)
        }
    }

    NavHost(navController = nav, startDestination = "home") {

        composable("home") {
            HomeScreen(
                onOpenPerson = { id -> nav.navigate("person/$id") },
                onOpenTx = { id -> nav.navigate("tx/$id") },
                onOpenEdit = { personId, txId, type ->
                    nav.navigate("edit?txId=$txId&personId=$personId&type=$type")
                },
                onOpenReports = { nav.navigate("reports") },
                onOpenSettings = { nav.navigate("settings") }
            )
        }

        composable("reports") {
            ReportsScreen(
                onBack = { nav.popBackStack() },
                onOpenTx = { id -> nav.navigate("tx/$id") },
                onOpenPerson = { id -> nav.navigate("person/$id") }
            )
        }

        composable("settings") {
            SettingsScreen(onBack = { nav.popBackStack() })
        }

        composable(
            route = "person/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            PersonScreen(
                personId = entry.arguments?.getLong("id") ?: 0L,
                onBack = { nav.popBackStack() },
                onOpenTx = { id -> nav.navigate("tx/$id") },
                onOpenEdit = { personId, txId, type ->
                    nav.navigate("edit?txId=$txId&personId=$personId&type=$type")
                }
            )
        }

        composable(
            route = "tx/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            TransactionDetailScreen(
                txId = entry.arguments?.getLong("id") ?: 0L,
                onBack = { nav.popBackStack() },
                onOpenPerson = { id -> nav.navigate("person/$id") },
                onOpenEdit = { personId, txId, type ->
                    nav.navigate("edit?txId=$txId&personId=$personId&type=$type")
                }
            )
        }

        composable(
            route = "edit?txId={txId}&personId={personId}&type={type}",
            arguments = listOf(
                navArgument("txId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("personId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("type") { type = NavType.IntType; defaultValue = -1 }
            )
        ) { entry ->
            EditScreen(
                txId = entry.arguments?.getLong("txId") ?: 0L,
                presetPersonId = entry.arguments?.getLong("personId") ?: 0L,
                presetType = entry.arguments?.getInt("type") ?: -1,
                onBack = { nav.popBackStack() },
                onOpenPerson = { id -> nav.navigate("person/$id") }
            )
        }
    }

    PermissionsAndGuide(settings)
}
