package com.akky.mediaplayer.player

import com.google.android.exoplayer2.ExoPlayer

data class VideoPlayerUiState(
    val isLoading: Boolean = true,
    val isPlaying: Boolean = false,
    val isFullscreen: Boolean = false,
    val errorMessage: String? = null,
    val player: ExoPlayer? = null,
    val availableQualities: List<VideoQuality> = emptyList(),
    val selectedQuality: VideoQuality = VideoQuality.auto(),
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L
) {

    val hasError: Boolean get() = errorMessage != null

    val isReady: Boolean get() = !isLoading && !hasError && player != null

    val progress: Float get() = if (duration > 0) currentPosition.toFloat() / duration else 0f

    val bufferedProgress: Float get() = if (duration > 0) bufferedPosition.toFloat() / duration else 0f
}

/**
 * Video Quality data class with smart constructors
 */
data class VideoQuality(
    val label: String,
    val height: Int,
    val isAuto: Boolean = false
) {
    companion object {
        fun auto() = VideoQuality("Auto", 0, true)
        fun fromHeight(height: Int): VideoQuality {
            val label = when {
                height >= 2160 -> "4K (${height}p)"
                height >= 1080 -> "1080p"
                height >= 720 -> "720p"
                height >= 480 -> "480p"
                height >= 360 -> "360p"
                else -> "${height}p"
            }
            return VideoQuality(label, height)
        }
        fun getDefaultQualities(): List<VideoQuality> = listOf(
            auto(),
            VideoQuality("1080p", 1080),
            VideoQuality("720p", 720),
            VideoQuality("480p", 480),
            VideoQuality("360p", 360)
        )
    }
    val isHighQuality: Boolean get() = height >= 720
    
    /**
     * Check if this is a low quality option
     */
    val isLowQuality: Boolean get() = height > 0 && height < 480
}
