package org.example;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main{
    public static void main(String[] args) throws InterruptedException {
        //Paste discord music token here
        String token = System.getenv("DISCORD_TOKEN");

        if (token == null) {
            System.err.println("ERROR: No Token Found!");
            System.err.println("1. Set the DISCORD_TOKEN environment variable in IntelliJ.");
            System.err.println("2. Or in your Run Configuration.");
            return;
        }

        //Configure the bot
        JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                // Plug in BotListener
                .addEventListeners(new BotListener())

                .build()
                .awaitReady(); // Blocks until the bot is fully online

        System.out.println("Bot is online and listening!");
    }
}