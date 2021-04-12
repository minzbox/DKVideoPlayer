package com.dueeeke.videoplayer.render

import android.graphics.Bitmap
import android.view.View
import com.dueeeke.videoplayer.player.AbstractPlayer

interface IRenderView {
    /**
     * Associate AbstractPlayer
     */
    fun attachToPlayer(player: AbstractPlayer)

    /**
     * Set the video width and height
     * @param videoWidth width
     * @param videoHeight high
     */
    fun setVideoSize(videoWidth: Int, videoHeight: Int)

    /**
     * Set the video rotation angle
     * @param degree angle value
     */
    fun setVideoRotation(degree: Int)

    /**
     * Set screen scale type
     * @param scaleType type
     */
    fun setScaleType(scaleType: Int)

    /**
     * Get the real RenderView
     */
    val view: View?

    /**
     * Screenshot
     */
    fun doScreenShot(): Bitmap?

    /**
     * Release
     */
    fun release()
}