package org.example;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerManager {
    // The Singleton Instance (The only copy of this class that will ever exist)
    private static PlayerManager INSTANCE;

    // Storage
    private final Map<Long, GuildMusicManager> musicManagers; // Long is the server ID, mapped to a unique GuildMusicManager
    private final AudioPlayerManager audioPlayerManager; // Used for creating audio players and loading tracks + playlists
    private final DatabaseManager database;

    // The constructor (Private so no one else can make a new Player Manager)
    private PlayerManager() {
        this.musicManagers = new HashMap<>();
        this.audioPlayerManager = new DefaultAudioPlayerManager();
        this.database = new DatabaseManager();

        // Tells Lavaplayer to learn to play from Youtube, Soundcloud, e.t.c
        AudioSourceManagers.registerRemoteSources(this.audioPlayerManager);
        AudioSourceManagers.registerLocalSource(this.audioPlayerManager);
    }

    // The Accessor (how other classes get this instance)
    public static PlayerManager get() {
        if (INSTANCE == null) {
            INSTANCE = new PlayerManager();
        }
        return INSTANCE;
    }

    // THe Retrieval Method: Gets the specific box for the specific server
    public GuildMusicManager getGuildMusicManager(Guild guild) {
        return this.musicManagers.computeIfAbsent(guild.getIdLong(), (guildId) -> {
            // If this server doesn't have a manager, create one
            GuildMusicManager musicManager = new GuildMusicManager(this.audioPlayerManager, database);

            // Connect the SendHandler (audio cable) to the guild's audio manager
            // Audio manager deals with creating, managing and severing audio connections to VoiceChannels and dealing with audio handlers
            // Connects the server audio "port" with the allocated GuildMusicManager instance
            guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

            return musicManager;
        });
    }

    // The Play Method (Handles loading the track)
    public void loadAndPlay(TextChannel channel, Member member, String trackUrl) {
        // Returns the music manager for the server based off of the channel (channel.getGuild())
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());

        // Smart search logic
        // If it starts with http, it's a direct link
        // If not, treat it as a search query for SoundCloud (scsearch:)
        String lookupUrl;
        if (trackUrl.startsWith("http")) {
            lookupUrl = trackUrl;
        } else {
            lookupUrl = "scsearch:" + trackUrl;
        }


        // Ask the audioPlayerManager to decode and fetch the URL
        this.audioPlayerManager.loadItemOrdered(musicManager, lookupUrl, new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(AudioTrack track) {
                // Case 1: A simple song link
                channel.sendMessage("Adding to queue: " + track.getInfo().title).queue();
                musicManager.scheduler.queue(member, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                // Case 2: A Playlist link or a Youtube search
                // Just pick the first song for now
                AudioTrack firstTrack = playlist.getTracks().get(0);
                channel.sendMessage("Adding to queue: " + firstTrack.getInfo().title).queue();
                musicManager.scheduler.queue(member, firstTrack);
            }

            @Override
            public void noMatches() {
                // Case 3: The link was broken or found nothing
                channel.sendMessage("Nothing found by " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                // Case 4: Something broke (Age restricted, region locked, e.t.c)
                channel.sendMessage("Could not play " + exception.getMessage()).queue();
            }
        });
    }

    public void skipTrack(TextChannel channel) {
        // Now we have the music manager for the server we want
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());

        // Force a call to nextTrack() through the scheduler
        musicManager.scheduler.nextTrack();

        channel.sendMessage("Skipped current track").queue();
    }

    public void clearTrack(TextChannel channel) {
        // Now we have the music manager for the server we want
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());

        // Force a call to nextTrack() through the scheduler
        musicManager.scheduler.clear();

        channel.sendMessage("Cleared queue and player stopped").queue();
    }

    public void pauseTrack(TextChannel channel) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());

        // Check if we are playing anything
        if (musicManager.audioPlayer.getPlayingTrack() == null) {
             channel.sendMessage("Cannot pause: Player is currently empty").queue();
        }

        // Toggle logic, if paused resume. If playing, pause
        boolean currentPauseState = musicManager.scheduler.isPaused();
        boolean newPauseState = !currentPauseState;

        musicManager.scheduler.pause(newPauseState);

        String message = newPauseState ? "Passed the player." : "Resumed playback";
        channel.sendMessage(message).queue();
    }

    public DatabaseManager getDatabase() {
        return database;
    }
}
