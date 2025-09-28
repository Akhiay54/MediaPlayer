package com.akky.mediaplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akky.mediaplayer.ui.theme.MediaPlayerTheme
import com.akky.mediaplayer.player.VideoPlayerScreen
import com.akky.mediaplayer.player.VideoPlayerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediaPlayerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val videoPlayerViewModel: VideoPlayerViewModel = viewModel()
    val uiState by videoPlayerViewModel.uiState.collectAsState()

    if (uiState.isFullscreen) {
        VideoPlayerScreenTab(viewModel = videoPlayerViewModel)
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("DRM Video Player") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Metadata Viewer") }
                )
            }

            when (selectedTab) {
                0 -> VideoPlayerScreenTab(viewModel = videoPlayerViewModel)
                1 -> MetadataViewerScreen()
            }
        }
    }
}

@Composable
fun VideoPlayerScreenTab(viewModel: VideoPlayerViewModel = viewModel()) {
    VideoPlayerScreen(viewModel = viewModel)
}

@Composable
fun MetadataViewerScreen() {
    InstagramAPI()
}