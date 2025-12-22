package org.example;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.jetbrains.annotations.NotNull;

public class AudioRequest implements Comparable<AudioRequest> {
    private final AudioTrack track;
    private final long userId;
    private final int userIndex; // Tracks how many songs a user already has queued
    private final long timestamp; // When a song was added

    public AudioRequest(AudioTrack track, long userId, int userIndex) {
        this.track = track;
        this.userId = userId;
        this.userIndex = userIndex;
        this.timestamp = System.currentTimeMillis();
    }

    public AudioTrack getTrack() { return track; }
    public long getUserId() { return userId; }

    @Override
    public int compareTo(@NotNull AudioRequest other) {
        // Check first who has the lower amount of songs queued
        if (this.userIndex < other.userIndex) {
            return -1; // "this" user goes first
        } else if (this.userIndex > other.userIndex) {
            return 1; // "other" user goes first
        } else {
            // Tie breaker, now check who queued first
            return Long.compare(this.timestamp, other.timestamp);
        }
    }
}
