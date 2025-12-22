package org.example;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

public class GuildMusicManager {
    public final AudioPlayer audioPlayer;
    public final TrackScheduler scheduler;
    private final AudioPlayerSendHandler sendHandler;

    public GuildMusicManager(AudioPlayerManager manager, DatabaseManager database) {
        // Create the "turntable" (Player)
        // We create ONE player. This is the turntable for each server instance
        this.audioPlayer = manager.createPlayer();

        // Create the DJ (Scheduler) and plug it into our Turntable
        // We pass in this.audioPlayer to the Scheduler
        // Now the scheduler knows which turntable it controls

        // Pass database down to scheduler
        // REMEMBER: The only reason we have database is to have a more advanced !compare, which is done in scheduler
        this.scheduler = new TrackScheduler(this.audioPlayer, database);

        // This finishes the feedback loop between the scheduler and the player
        // Now the player knows when it finishes a song, tell this scheduler
        this.audioPlayer.addListener(this.scheduler);

        // Create the Transmitter (SendHandler)
        // We pass this.audioPlayer to the SendHandler
        // Now the handler knows where to get its audio data from
        this.sendHandler = new AudioPlayerSendHandler(this.audioPlayer);
    }

    // Sends the actual song data out
    public AudioPlayerSendHandler getSendHandler() {
        return sendHandler;
    }
}
