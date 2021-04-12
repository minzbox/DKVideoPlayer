package com.dueeeke.videoplayer.player

/**
 * Play progress manager, inherit this interface to implement your own progress manager.
 */
abstract class ProgressManager {
    /**
     * This method is used to implement the logic of saving progress
     * @param url playback address
     * @param progress Play progress
     */
    abstract fun saveProgress(url: String?, progress: Long)

    /**
     * This method is used to implement the logic of obtaining the saved progress
     * @param url playback address
     * @return saved playback progress
     */
    abstract fun getSavedProgress(url: String?): Long
}