package com.dueeeke.dkplayer.util

import android.view.View
import android.widget.FrameLayout
import com.dueeeke.videoplayer.player.VideoView
import com.dueeeke.videoplayer.player.VideoViewManager.Companion.config
import java.lang.reflect.Field

object Utils {
    /**
     * Get the current playback core
     */
    @JvmStatic
    val currentPlayerFactory: Any?
        get() {
            val config = config
            var playerFactory: Any? = null
            try {
                val mPlayerFactoryField: Field
                if (config != null) {
                    mPlayerFactoryField = config.javaClass.getDeclaredField("mPlayerFactory")
                    mPlayerFactoryField.isAccessible = true
                    playerFactory = mPlayerFactoryField[config]
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return playerFactory
        }

    /**
     * Remove View from the parent control
     */
    @JvmStatic
    fun removeViewFromParent(v: View?) {
        if (v == null) return
        val parent = v.parent
        if (parent is FrameLayout) {
            parent.removeView(v)
        }
    }

    /**
     * Returns a string containing player state debugging information.
     */
    @JvmStatic
    fun playState2str(state: Int): String {
        val playStateString: String = when (state) {
            VideoView.STATE_IDLE -> "idle"
            VideoView.STATE_PREPARING -> "preparing"
            VideoView.STATE_PREPARED -> "prepared"
            VideoView.STATE_PLAYING -> "playing"
            VideoView.STATE_PAUSED -> "pause"
            VideoView.STATE_BUFFERING -> "buffering"
            VideoView.STATE_BUFFERED -> "buffered"
            VideoView.STATE_PLAYBACK_COMPLETED -> "playback completed"
            VideoView.STATE_ERROR -> "error"
            else -> "idle"
        }
        return String.format("playState: %s", playStateString)
    }

    /**
     * Returns a string containing player state debugging information.
     */
    @JvmStatic
    fun playerState2str(state: Int): String {
        val playerStateString: String = when (state) {
            VideoView.PLAYER_NORMAL -> "normal"
            VideoView.PLAYER_FULL_SCREEN -> "full screen"
            VideoView.PLAYER_TINY_SCREEN -> "tiny screen"
            else -> "normal"
        }
        return String.format("playerState: %s", playerStateString)
    }
}