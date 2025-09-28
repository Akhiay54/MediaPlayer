package com.akky.mediaplayer.player

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback
import com.google.android.exoplayer2.drm.UnsupportedDrmException
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoPlayerViewModel : ViewModel(), DefaultLifecycleObserver {

    private val _uiState = MutableStateFlow(VideoPlayerUiState())
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()
    
    private var player: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var isPlayerInitialized = false

    companion object {
        private const val TAG = "VideoPlayerViewModel"
        private const val MANIFEST_URL = "https://bitmovin-a.akamaihd.net/content/art-of-motion_drm/mpds/11331.mpd"
        private const val LICENSE_URL = "https://cwip-shaka-proxy.appspot.com/no_auth"
    }

    fun initializePlayer(context: Context) {
        if (isPlayerInitialized) {
            Log.d(TAG, "Player already initialized, skipping")
            return
        }
        
        viewModelScope.launch(Dispatchers.Main) {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                val (createdPlayer, createdTrackSelector) = createExoPlayer(context)
                player = createdPlayer
                trackSelector = createdTrackSelector
                isPlayerInitialized = true
                
                val qualities = parseAvailableQualities()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    player = createdPlayer,
                    availableQualities = qualities,
                    selectedQuality = qualities.firstOrNull() ?: VideoQuality.auto()
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing player", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to initialize player: ${e.message}"
                )
            }
        }
    }

    fun changeQuality(quality: VideoQuality) {
        trackSelector?.let { selector ->
            applyQualitySelection(selector, quality)
            _uiState.value = _uiState.value.copy(selectedQuality = quality)
        }
    }

    fun toggleFullscreen() {
        _uiState.value = _uiState.value.copy(
            isFullscreen = !_uiState.value.isFullscreen
        )
    }

    fun togglePlayPause() {
        player?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
            _uiState.value = _uiState.value.copy(isPlaying = player.isPlaying)
        }
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }
    

    fun onConfigurationChanged() {
        Log.d(TAG, "Configuration changed - maintaining player state")
        // Player instance is preserved, just update UI state if needed
        player?.let { player ->
            _uiState.value = _uiState.value.copy(
                isPlaying = player.isPlaying,
                player = player
            )
        }
    }
    

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        player?.pause()
        Log.d(TAG, "Player paused due to lifecycle")
    }
    
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Log.d(TAG, "Lifecycle resumed")
    }
    
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        player?.pause()
        Log.d(TAG, "Player stopped due to lifecycle")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "Releasing player resources")
        player?.release()
        player = null
        trackSelector = null
        isPlayerInitialized = false
    }

    private suspend fun createExoPlayer(context: Context): Pair<ExoPlayer, DefaultTrackSelector> = 
        withContext(Dispatchers.Main) {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            val trackSelector = DefaultTrackSelector(context)
            
            val drmSessionManager = createDrmSessionManager(httpDataSourceFactory)
            
            val player = ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .build()
            
            val mediaItem = MediaItem.Builder()
                .setUri(MANIFEST_URL)
                .setDrmConfiguration(
                    MediaItem.DrmConfiguration.Builder(Util.getDrmUuid("widevine")!!)
                        .setLicenseUri(LICENSE_URL)
                        .build()
                )
                .build()
            
            val mediaSource = DashMediaSource.Factory(httpDataSourceFactory)
                .setDrmSessionManagerProvider { drmSessionManager }
                .createMediaSource(mediaItem)
            
            player.setMediaSource(mediaSource)
            player.prepare()
            player.playWhenReady = false
            
            Pair(player, trackSelector)
        }
    
    private fun createDrmSessionManager(httpDataSourceFactory: DefaultHttpDataSource.Factory): DrmSessionManager {
        return try {
            val drmCallback = HttpMediaDrmCallback(LICENSE_URL, httpDataSourceFactory)
            DefaultDrmSessionManager.Builder()
                .setUuidAndExoMediaDrmProvider(
                    Util.getDrmUuid("widevine")!!,
                    com.google.android.exoplayer2.drm.FrameworkMediaDrm.DEFAULT_PROVIDER
                )
                .build(drmCallback)
        } catch (e: UnsupportedDrmException) {
            Log.e(TAG, "DRM not supported", e)
            DrmSessionManager.DRM_UNSUPPORTED
        }
    }
    
    private suspend fun parseAvailableQualities(): List<VideoQuality> = withContext(Dispatchers.IO) {
        try {
            val manifestContent = fetchManifestContent()
            parseQualitiesFromManifest(manifestContent)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing qualities", e)
            VideoQuality.getDefaultQualities()
        }
    }
    
    private fun fetchManifestContent(): String {
        val connection = java.net.URL(MANIFEST_URL).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        return connection.inputStream.bufferedReader().readText()
    }
    
    private fun parseQualitiesFromManifest(manifestContent: String): List<VideoQuality> {
        val qualities = mutableListOf<VideoQuality>()
        qualities.add(VideoQuality.auto())
        
        val heightRegex = """height="(\d+)"""".toRegex()
        val heights = heightRegex.findAll(manifestContent)
            .map { it.groupValues[1].toInt() }
            .distinct()
            .sortedDescending()
        
        heights.forEach { height ->
            qualities.add(VideoQuality.fromHeight(height))
        }
        
        return qualities
    }
    
    private fun applyQualitySelection(trackSelector: DefaultTrackSelector, quality: VideoQuality) {
        val parametersBuilder = trackSelector.buildUponParameters()
        
        if (quality.isAuto) {
            parametersBuilder.clearVideoSizeConstraints()
            parametersBuilder.setMaxVideoSizeSd()
        } else {
            parametersBuilder.setMaxVideoSize(Int.MAX_VALUE, quality.height)
            parametersBuilder.setMinVideoSize(0, 0)
        }
        
        trackSelector.setParameters(parametersBuilder)
    }
}
