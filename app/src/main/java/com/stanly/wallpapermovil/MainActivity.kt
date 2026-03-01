package com.stanly.wallpapermovil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stanly.wallpapermovil.ui.screens.AddWallpaperScreen
import com.stanly.wallpapermovil.ui.screens.AdjustScreen
import com.stanly.wallpapermovil.ui.screens.LibraryScreen
import com.stanly.wallpapermovil.ui.theme.WallpaperMovilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WallpaperMovilTheme {
                val navController = rememberNavController()
                val viewModel: WallpaperViewModel = viewModel()

                NavHost(
                    navController = navController,
                    startDestination = "library"
                ) {
                    // ── Screen 1: Wallpaper Library ───────────────────────────
                    composable("library") {
                        LibraryScreen(
                            viewModel = viewModel,
                            onAddNew  = { navController.navigate("add") }
                        )
                    }

                    // ── Screen 2: Add / Select a video ────────────────────────
                    composable("add") {
                        AddWallpaperScreen(
                            viewModel          = viewModel,
                            onBack             = { navController.popBackStack() },
                            onPreviewAndAdjust = { navController.navigate("adjust") }
                        )
                    }

                    // ── Screen 3: Crop / Zoom / Pan adjustment ────────────────
                    composable("adjust") {
                        AdjustScreen(
                            viewModel = viewModel,
                            onBack    = { navController.popBackStack() },
                            onSaved   = {
                                // Go back to library, clearing the add/adjust back stack
                                navController.popBackStack("library", inclusive = false)
                            }
                        )
                    }
                }
            }
        }
    }
}
