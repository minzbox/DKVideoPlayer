package com.dueeeke.videoplayer.player

import android.content.Context

/**
 * How to use this interface:
 * 1. Inherit [AbstractPlayer] to extend your own player.
 * 2. Inherit this interface and implement [.createPlayer], and return to the player in step 1.
 * Refer to the implementation of [AndroidMediaPlayer] and [AndroidMediaPlayerFactory].
 */
abstract class PlayerFactory<P : AbstractPlayer?> {
    abstract fun createPlayer(context: Context?): P
}