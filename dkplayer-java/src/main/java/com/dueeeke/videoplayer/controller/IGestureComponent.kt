package com.dueeeke.videoplayer.controller

interface IGestureComponent : IControlComponent {
    /**
     * Start sliding
     */
    fun onStartSlide()

    /**
     * Stop sliding
     */
    fun onStopSlide()

    /**
     * Slide to adjust progress
     * @param slidePosition sliding progress
     * @param currentPosition current playback progress
     * @param duration total video length
     */
    fun onPositionChange(slidePosition: Int, currentPosition: Int, duration: Int)

    /**
     * Slide to adjust brightness
     * @param percent brightness percentage
     */
    fun onBrightnessChange(percent: Int)

    /**
     * Slide to adjust the volume
     * @param percent volume percentage
     */
    fun onVolumeChange(percent: Int)
}