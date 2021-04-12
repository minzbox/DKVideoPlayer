package com.dueeeke.dkplayer.widget.player;


import android.content.Context;

import com.dueeeke.videoplayer.exo.ExoMediaPlayer;
import com.google.android.exoplayer2.source.MediaSource;

/**
 * Custom ExoMediaPlayer, currently expanded such as while playing and storing, and can directly set Exo's own MediaSource.
 */
public class CustomExoMediaPlayer extends ExoMediaPlayer {

    public CustomExoMediaPlayer(Context context) {
        super(context);
    }

    public void setDataSource(MediaSource dataSource) {
        mMediaSource = dataSource;
    }
}
