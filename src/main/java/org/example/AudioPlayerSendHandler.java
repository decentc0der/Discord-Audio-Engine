package org.example;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import java.nio.ByteBuffer;

public class AudioPlayerSendHandler implements AudioSendHandler{
    // Contains the actual song data
    // AudioPlayer plays audio tracks and can provide frame data from the current track
    private final AudioPlayer audioPlayer;
    // Stores the most recent 20ms byte of the song data to be sent to Discord
    private AudioFrame lastFrame;

    public AudioPlayerSendHandler(AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
    }

    @Override
    public boolean canProvide() {
        // Asks user: Do you have a new chunk of audio ready
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        // If yes, flip the data to the format Discord wants
        return ByteBuffer.wrap(lastFrame.getData());
    }

    @Override
    public boolean isOpus() {
        // Tell discord we are sending Opus format audio (Standard for voice chat)
        return true;
    }
}
