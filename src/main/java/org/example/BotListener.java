package org.example;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.EmbedBuilder;
import java.awt.Color;
import java.util.List;
import java.util.Map;

// "extends ListenerAdapter" gives our class the ability to hear Discord events
public class BotListener extends ListenerAdapter{

    // We "Override" the specific function that runs whenever ANY message is sent
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        // Safety Check 1: Ignore messages from other bots (Prevent infinite loops)
        if (event.getAuthor().isBot()) return;

        // Safety Check 2: We only want to read TEXT messages
        String message = event.getMessage().getContentRaw();

        // Parser Logic, split string to be checked later
        String[] args = message.split(" ", 2);

        // Check the command (case insensitive, so !sHout or !SHOUT works too)
        if (args[0].equalsIgnoreCase("!shout")) {

            // Handle the missing argument case (only !shout was typed)
            if (args.length < 2) {
                event.getChannel().sendMessage("Ryan's a bitch!").queue();
                return;
            }

            //Now handle content
            String content = args[1];
            event.getChannel().sendMessage(content.toUpperCase()).queue();
        } // Check for !join
        else if (args[0].equalsIgnoreCase("!join")) {
            // Get the voice state of the user who typed the command
            if (!event.getMember().getVoiceState().inAudioChannel()) {
                event.getChannel().sendMessage("You need to be in a voice channel").queue();
                return;
            }

            // Get the voice manager for the server
            net.dv8tion.jda.api.managers.AudioManager audioManager = event.getGuild().getAudioManager();

            // Connect to the user's channel
            net.dv8tion.jda.api.entities.channel.middleman.AudioChannel userChannel = event.getMember().getVoiceState().getChannel();

            audioManager.openAudioConnection(userChannel);

            event.getChannel().sendMessage("Connected to your voice channel").queue();
        } // Check for !leave
        else if (args[0].equalsIgnoreCase("!leave")) {
            net.dv8tion.jda.api.managers.AudioManager audioManager = event.getGuild().getAudioManager();

            // Checks if bot is already in a voice channel
            if (!audioManager.isConnected()) {
                event.getChannel().sendMessage("I'm not connected to a voice channel.").queue();
                return;
            }

            // Forces bot out of voice channel
            audioManager.closeAudioConnection();
            event.getChannel().sendMessage("Disconnected!").queue();
        } // Check for !play
        else if (args[0].equalsIgnoreCase("!play")) {
            // Get the member (the user who sent the message)
            Member member = event.getMember();

            // Get their voice state (Are they in a channel? Muted? Deafened?)
            GuildVoiceState memberVoiceState = member.getVoiceState();

            // Get the text channel the user called the bot in
            TextChannel channel = event.getChannel().asTextChannel();

            // Validation check
            if (!memberVoiceState.inAudioChannel()) {
                channel.sendMessage("You need to be in a voice channel").queue();
                return;
            }

            // Get the voice manager for this server
            AudioManager audioManager = event.getGuild().getAudioManager();

            // Get the specific channel the user is in
            AudioChannel voiceChannel = memberVoiceState.getChannel();

            // Move the bot into that channel
            audioManager.openAudioConnection(voiceChannel);

            // Now we play the actual music
            // Check if the user provided a link
            if (args.length < 2) {
                channel.sendMessage("Please provide a link! Usage: !play [song name or url]").queue();
                return;
            }

            // Find the link passed in
            String link = args[1];

            // Call the Singleton (PlayManager) to download and play the song
            PlayerManager.get().loadAndPlay(channel, member, link);
        } // Check for !skip
        else if (args[0].equalsIgnoreCase("!skip")) {
            // Get the channel the user made the request in
            TextChannel channel = event.getChannel().asTextChannel();

            PlayerManager.get().skipTrack(channel);
        } // Checks for !clear
        else if (args[0].equalsIgnoreCase("!clear")) {
            // Get the channel the user made the request in
            TextChannel channel = event.getChannel().asTextChannel();

            PlayerManager.get().clearTrack(channel);
        } // Checks for !pause
        else if (args[0].equalsIgnoreCase("!pause")){
            // Get the channel the user made the request in
            TextChannel channel = event.getChannel().asTextChannel();

            PlayerManager.get().pauseTrack(channel);
        } // Checks for !resume
        else if (args[0].equalsIgnoreCase("!resume")){
            // Get the channel the user made the request in
            TextChannel channel = event.getChannel().asTextChannel();

            PlayerManager.get().pauseTrack(channel);
        } // Checks for !compare
        else if (args[0].equalsIgnoreCase("!compare")) {
            // Get the channel the user made the request in
            TextChannel channel = event.getChannel().asTextChannel();

            // Validate that someone was mentioned after !compare
            if (event.getMessage().getMentions().getMembers().isEmpty()) {
                channel.sendMessage("Please mention a user to compare with!").queue();
                return;
            }

            // Gets the two users
            Member user1 = event.getMember(); // User who called
            Member user2 = event.getMessage().getMentions().getMembers().get(0); // The other user

            // Checks to see if user compared against themselves
            if (user1.getIdLong() == user2.getIdLong()) {
                channel.sendMessage("Don't compare yourself with others twin 🥹").queue();
                return;
            }

            // Get the database manager
            DatabaseManager db = PlayerManager.get().getDatabase();

            // Transform each user's list of songs to a map (User ID -> Artist count) through database
            Map<String, Integer> vector1 = db.getTopArtists(user1.getIdLong());
            Map<String, Integer> vector2 = db.getTopArtists(user2.getIdLong());

            // Calculate similarity
            Double score = MusicMath.calculateCosineSimilarity(vector1, vector2);

            // Convert decimal to percentage
            int percentage = (int) (score * 100);

            // Reply
            String msg = String.format("🎵 Musical Compatibility: **%d%%**\n%s vs %s",
                    percentage, user1.getEffectiveName(), user2.getEffectiveName());

            channel.sendMessage(msg).queue();
        } // Checks for !help
        else if (args[0].equalsIgnoreCase("!help")){
            // Get the channel the user made the request in
            TextChannel channel = event.getChannel().asTextChannel();

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("🎵 30Pineapples Music Bot Help");
            eb.setColor(Color.CYAN);
            eb.setDescription("Here are the commands you can use: ");

            // Field format: (Title, Content, Inline?)
            eb.addField("!shout", "Returns the words after shout in caps.", false);
            eb.addField("!join", "Bot joins the channel.", true);
            eb.addField("!leave", "Bot leaves the channel", true);
            eb.addField("!play [song name]", "Searches and queues a song (e.g., `!play drake`).", false);
            eb.addField("!skip", "Skips the current song.", true);
            eb.addField("!pause / !resume", "Toggles playback.", true);
            eb.addField("!clear", "Clears the entire queue and disconnects.", false);
            eb.addField("!compare", "Compares you and one other @mention user.", false);

            // Send the embed
            channel.sendMessageEmbeds(eb.build()).queue();
        }
    }
}
