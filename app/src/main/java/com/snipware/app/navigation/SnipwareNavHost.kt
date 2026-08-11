package com.snipware.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.snipware.app.data.repository.SnippetRepository
import com.snipware.app.ui.ViewModelFactory
import com.snipware.app.ui.editor.EditorScreen
import com.snipware.app.ui.editor.EditorViewModel
import com.snipware.app.ui.home.HomeScreen
import com.snipware.app.ui.home.HomeViewModel
import com.snipware.app.ui.navshell.BottomNavTab
import com.snipware.app.ui.navshell.ComingSoonScreen
import com.snipware.app.ui.navshell.SnipBottomNav
import com.snipware.app.ui.theme.SnipBg
import com.snipware.app.ui.viewer.CodeViewerScreen
import com.snipware.app.ui.viewer.CodeViewerViewModel

private object Routes {
    const val HOME = "home"
    const val PINNED = "pinned"
    const val ASSISTANT = "assistant"
    const val ACCOUNT = "account"
    const val EDITOR = "editor?snippetId={snippetId}"
    const val VIEWER = "viewer/{snippetId}"

    val BOTTOM_NAV_ROUTES = setOf(HOME, PINNED, ASSISTANT, ACCOUNT)

    fun editorNew() = "editor"
    fun editorEdit(id: String) = "editor?snippetId=$id"
    fun viewer(id: String) = "viewer/$id"
}

private fun routeToTab(route: String?): BottomNavTab? = when (route) {
    Routes.HOME -> BottomNavTab.ALL
    Routes.PINNED -> BottomNavTab.PINNED
    Routes.ASSISTANT -> BottomNavTab.ASSISTANT
    Routes.ACCOUNT -> BottomNavTab.ACCOUNT
    else -> null
}

@Composable
fun SnipwareNavHost(
    repository: SnippetRepository,
    navController: NavHostController = rememberNavController()
) {
    val factory = ViewModelFactory(repository)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomNav = currentRoute in Routes.BOTTOM_NAV_ROUTES

    Scaffold(
        containerColor = SnipBg,
        bottomBar = {
            if (showBottomNav) {
                SnipBottomNav(
                    currentTab = routeToTab(currentRoute),
                    onAllClick = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onPinnedClick = {
                        navController.navigate(Routes.PINNED) {
                            popUpTo(Routes.HOME)
                        }
                    },
                    onNewClick = { navController.navigate(Routes.editorNew()) },
                    onAssistantClick = {
                        navController.navigate(Routes.ASSISTANT) { popUpTo(Routes.HOME) }
                    },
                    onAccountClick = {
                        navController.navigate(Routes.ACCOUNT) { popUpTo(Routes.HOME) }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                val vm: HomeViewModel = viewModel(factory = factory)
                HomeScreen(
                    viewModel = vm,
                    pinnedOnly = false,
                    onViewFull = { navController.navigate(Routes.viewer(it.id)) },
                    onEdit = { navController.navigate(Routes.editorEdit(it.id)) }
                )
            }

            composable(Routes.PINNED) {
                val vm: HomeViewModel = viewModel(factory = factory)
                HomeScreen(
                    viewModel = vm,
                    pinnedOnly = true,
                    onViewFull = { navController.navigate(Routes.viewer(it.id)) },
                    onEdit = { navController.navigate(Routes.editorEdit(it.id)) }
                )
            }

            composable(Routes.ASSISTANT) {
                ComingSoonScreen(
                    icon = Icons.Outlined.HelpOutline,
                    title = "Assistant",
                    description = "The AI assistant isn't wired up in this build yet."
                )
            }

            composable(Routes.ACCOUNT) {
                ComingSoonScreen(
                    icon = Icons.Outlined.Person,
                    title = "Account",
                    description = "Sign-in and sync aren't wired up in this build yet -- everything stays local for now."
                )
            }

            composable(
                route = Routes.EDITOR,
                arguments = listOf(
                    navArgument("snippetId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry2 ->
                val vm: EditorViewModel = viewModel(factory = factory)
                val snippetId = backStackEntry2.arguments?.getString("snippetId")
                EditorScreen(
                    viewModel = vm,
                    editingId = snippetId,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.VIEWER,
                arguments = listOf(navArgument("snippetId") { type = NavType.StringType })
            ) { backStackEntry2 ->
                val vm: CodeViewerViewModel = viewModel(factory = factory)
                val snippetId = requireNotNull(backStackEntry2.arguments?.getString("snippetId"))
                CodeViewerScreen(
                    viewModel = vm,
                    snippetId = snippetId,
                    onBack = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() }
                )
            }
        }
    }
}
