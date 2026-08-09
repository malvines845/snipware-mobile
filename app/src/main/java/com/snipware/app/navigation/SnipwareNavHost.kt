package com.snipware.app.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.snipware.app.data.repository.SnippetRepository
import com.snipware.app.ui.ViewModelFactory
import com.snipware.app.ui.editor.EditorScreen
import com.snipware.app.ui.editor.EditorViewModel
import com.snipware.app.ui.home.HomeScreen
import com.snipware.app.ui.home.HomeViewModel
import com.snipware.app.ui.viewer.CodeViewerScreen
import com.snipware.app.ui.viewer.CodeViewerViewModel

private object Routes {
    const val HOME = "home"
    const val EDITOR = "editor?snippetId={snippetId}"
    const val VIEWER = "viewer/{snippetId}"

    fun editorNew() = "editor"
    fun editorEdit(id: String) = "editor?snippetId=$id"
    fun viewer(id: String) = "viewer/$id"
}

@Composable
fun SnipwareNavHost(
    repository: SnippetRepository,
    navController: NavHostController = rememberNavController()
) {
    val factory = ViewModelFactory(repository)

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                viewModel = vm,
                onViewFull = { navController.navigate(Routes.viewer(it.id)) },
                onEdit = { navController.navigate(Routes.editorEdit(it.id)) },
                onAddClick = { navController.navigate(Routes.editorNew()) }
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
        ) { backStackEntry ->
            val vm: EditorViewModel = viewModel(factory = factory)
            val snippetId = backStackEntry.arguments?.getString("snippetId")
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
        ) { backStackEntry ->
            val vm: CodeViewerViewModel = viewModel(factory = factory)
            val snippetId = requireNotNull(backStackEntry.arguments?.getString("snippetId"))
            CodeViewerScreen(
                viewModel = vm,
                snippetId = snippetId,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() }
            )
        }
    }
}
