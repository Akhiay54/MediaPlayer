package com.akky.mediaplayer.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.exoplayer2.ui.StyledPlayerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    viewModel: VideoPlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Register lifecycle observer to prevent memory leaks
    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(viewModel)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(viewModel)
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.initializePlayer(context)
    }
    
    LaunchedEffect(uiState.isFullscreen) {
        val activity = context as? Activity
        activity?.requestedOrientation = if (uiState.isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
    
    BackHandler(enabled = uiState.isFullscreen) {
        viewModel.toggleFullscreen()
    }
    
    if (uiState.isFullscreen) {
        FullscreenPlayerView(
            uiState = uiState,
            onToggleFullscreen = viewModel::toggleFullscreen
        )
    } else {
        NormalPlayerView(
            uiState = uiState,
            onToggleFullscreen = viewModel::toggleFullscreen,
            onQualityChange = viewModel::changeQuality
        )
    }
}

/**
 * Normal (non-fullscreen) player view
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NormalPlayerView(
    uiState: VideoPlayerUiState,
    onToggleFullscreen: () -> Unit,
    onQualityChange: (VideoQuality) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "DRM Video Player",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        VideoPlayerView(
            uiState = uiState,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp)),
            showFullscreenButton = true,
            onToggleFullscreen = onToggleFullscreen
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        QualitySelectionCard(
            uiState = uiState,
            onQualityChange = onQualityChange
        )
    }
}

@Composable
private fun FullscreenPlayerView(
    uiState: VideoPlayerUiState,
    onToggleFullscreen: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VideoPlayerView(
            uiState = uiState,
            modifier = Modifier.fillMaxSize(),
            showFullscreenButton = true,
            onToggleFullscreen = onToggleFullscreen
        )
    }
}

@Composable
private fun VideoPlayerView(
    uiState: VideoPlayerUiState,
    modifier: Modifier = Modifier,
    showFullscreenButton: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier.background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(12.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Loading video...")
                }
            }
        }
        
        uiState.hasError -> {
            Card(
                modifier = modifier,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.errorMessage ?: "Unknown error",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        uiState.player != null -> {
            AndroidView(
                factory = { context ->
                    StyledPlayerView(context).apply {
                        player = uiState.player
                        useController = true
                        if (showFullscreenButton && onToggleFullscreen != null) {
                            setFullscreenButtonClickListener { onToggleFullscreen() }
                        }
                    }
                },
                modifier = modifier,
                update = { view ->
                    if (view.player != uiState.player) {
                        view.player = uiState.player
                    }
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualitySelectionCard(
    uiState: VideoPlayerUiState,
    onQualityChange: (VideoQuality) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Video Quality",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            QualityDropdown(
                uiState = uiState,
                onQualityChange = onQualityChange
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualityDropdown(
    uiState: VideoPlayerUiState,
    onQualityChange: (VideoQuality) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = uiState.selectedQuality.label,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { 
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) 
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            uiState.availableQualities.forEach { quality ->
                DropdownMenuItem(
                    text = { 
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(quality.label)
                            if (quality.isHighQuality) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "High Quality",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onQualityChange(quality)
                        expanded = false
                    },
                    leadingIcon = if (quality == uiState.selectedQuality) {
                        { Icon(Icons.Default.Check, contentDescription = "Selected") }
                    } else null
                )
            }
        }
    }
}