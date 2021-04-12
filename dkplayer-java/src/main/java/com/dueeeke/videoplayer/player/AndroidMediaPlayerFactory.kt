package com.dueeeke.videoplayer.player

import android.content.Context

/**
 * Create a factory class of [AndroidMediaPlayer]. It is not recommended.
 * The system's MediaPlayer compatibility is poor. It is recommended to use IjkPlayer or ExoPlayer
 */
class AndroidMediaPlayerFactory : PlayerFactory<AndroidMediaPlayer?>() {
    override fun createPlayer(context: Context?): AndroidMediaPlayer {
        return AndroidMediaPlayer(context!!)
    }

    companion object {
        @JvmStatic
        fun create(): AndroidMediaPlayerFactory {
            return AndroidMediaPlayerFactory()
        }
    }
}