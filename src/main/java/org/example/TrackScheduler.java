package org.example;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.entities.Member;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

public class TrackScheduler extends AudioEventAdapter{
    private final AudioPlayer player;

    // The heap, automatically sorts songs based off of how many songs that user has queued
    private final PriorityBlockingQueue<AudioRequest> queue;

    // The tracker, tracks how many songs each user has queued
    private final Map<Long, Integer> userTicketCounts;

    // History, stores songs in a library database (Two columns, Plays and Songs)
    private final DatabaseManager db;

    public TrackScheduler(AudioPlayer player, DatabaseManager db) {
        this.player = player;
        this.queue = new PriorityBlockingQueue<>();
        this.userTicketCounts = new HashMap<>();
        this.db = db;
    }

    public void queue(Member member, AudioTrack track) {
        long userId = member.getIdLong();

        // Tag the track so we know who to credit it to when it ends
        track.setUserData(userId);

        // Check the tracker, how many songs does this user have queued right now
        // getOrDefault returns 0 if they aren't in the map yet (haven't queued any songs)
        int ticketNumber = userTicketCounts.getOrDefault(userId, 0);

        // Update the ticket count for the user
        userTicketCounts.put(userId, ticketNumber + 1);

        // Create the request and push onto the heap
        AudioRequest request = new AudioRequest(track, userId, ticketNumber);
        queue.offer(request);

        // If the bot is silent, start the track immediately
        if (player.getPlayingTrack() == null) {
            nextTrack();
        }
    }

    public void nextTrack() {
        // Pop the highest priority song
        AudioRequest request = queue.poll();

        // If the queue was already empty, request is null, stop playing
        if (request == null) {
            player.stopTrack();
            // If the queue is totally empty, reset song counts for each user
            // This ensures the next session starts fresh
            if (queue.isEmpty()) { // This step is redundant, but keep anyway for readability
                userTicketCounts.clear();
            }
            return;
        }

        AudioTrack track = request.getTrack();

        // Play the song
        player.startTrack(track, false);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        // Now a song has ended, so retrieve the user ID from the tag we set in queue for the song that just ended
        if (track.getUserData() instanceof Long) {
            long userId = (Long) track.getUserData();

            // We check "LOAD_FAILED" because we don't want to save broken songs.
            if (endReason != AudioTrackEndReason.LOAD_FAILED) {
                // Run in a new Thread so we don't lag the music player
                new Thread(() -> {
                    // Save the song and the user who played it into the database
                    db.addPlay(userId, track);
                }).start();
            }

            // Now we decrement the count
            // Since this song is leaving the queue, we lower the user's count
            int currentCount = userTicketCounts.getOrDefault(userId, 0);
            if (currentCount > 0) {
                userTicketCounts.put(userId, currentCount - 1);
            }
        }

        // Only start the next song if the previous one finished naturally
        // This prevents skipped songs from triggering double-plays
        if (endReason.mayStartNext) {
            nextTrack();
        }
    }

    public void clear() {
        // Wipe the queue
        queue.clear();

        // Wipe the fairness data
        userTicketCounts.clear();

        // Stop the current song
        // This triggers onTrackEnd, but since queue is empty it just stops
        player.stopTrack();
    }

    public void pause(boolean pause) { player.setPaused(pause); }

    public boolean isPaused() { return player.isPaused(); }
}
