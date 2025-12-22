package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

/**
 * Creates two tables: Songs and Plays
 * Songs is a master list of every UNIQUE song that the bot has ever heard, with a unique ID number for each song
 * Plays is a list of "receipts" of songs played, it says [User A] listened to [Song ID] at [timestamp]
 * For !compare we ask the database give me all the song IDs for a specific user
 */
public class DatabaseManager {
    // The connection string "jdbc:sqlite:" tells Java to use the SQLite driver.
    // "musicbot.db" is the filename
    private static final String URL = "jdbc:sqlite:musicbot.db";

    public DatabaseManager() {
        // Upon initialization, make sure tables exist
        createTables();
    }

    private void createTables() {
        // We use try-with-resources (the inside of the parenthesis)
        // to ensure the connection closes automatically.
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {

            // Table 1: Songs (The library)
            String sqlSongs = "CREATE TABLE IF NOT EXISTS songs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT NOT NULL, " +
                    "artist TEXT NOT NULL, " +
                    "url TEXT NOT NULL UNIQUE" + // UNIQUE prevents duplicate URLs
                    ");";
            stmt.execute(sqlSongs);

            // Table 2: Plays (The History)
            String sqlPlays = "CREATE TABLE IF NOT EXISTS plays (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER NOT NULL, " +
                    "song_id INTEGER NOT NULL, " +
                    "timestamp INTEGER NOT NULL, " +
                    "FOREIGN KEY(song_id) REFERENCES songs(id)" +
                    ");";
            stmt.execute(sqlPlays);

            System.out.println("Database intialized successfully.");
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper to get a connection
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public void addPlay(long userId, AudioTrack track) {
        String title = track.getInfo().title;
        String artist = track.getInfo().author;
        String url = track.getInfo().uri;
        long timestamp = System.currentTimeMillis();

        // Asks do we already have this url in the library
        String sqlCheckSong = "SELECT id FROM songs WHERE url = ?";

        // Creates a new entry for this song (only used later on if new)
        String sqlInsertSong = "INSERT INTO songs(title, artist, url) VALUES(?, ?, ?)";

        // Writes the log to record who played a song, the song id, and when
        String sqlInsertPlay = "INSERT INTO plays(user_id, song_id, timestamp) VALUES(?, ?, ?)";

        try (Connection conn = getConnection()) {
            int songId = -1;

            // Step 1: Check if song exists
            try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sqlCheckSong)) {
                pstmt.setString(1, url);
                java.sql.ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    songId = rs.getInt("id");
                }
            }

            // If song is new, insert it
            if (songId == -1) {
                // ReturnGeneratedKeys lets us grab the ID (e.g., 101) immediately after inserting
                try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sqlInsertSong, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, title);
                    pstmt.setString(2, artist);
                    pstmt.setString(3, url);
                    pstmt.executeUpdate();

                    java.sql.ResultSet rs = pstmt.getGeneratedKeys();
                    if (rs.next()) {
                        songId = rs.getInt(1);
                    }
                }
            }

            // STEP 3: Record the play (History) and create the receipt
            if (songId != -1) {
                try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sqlInsertPlay)) {
                    pstmt.setLong(1, userId);
                    pstmt.setInt(2, songId);
                    pstmt.setLong(3, timestamp);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saving play to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Retrieval used for !compare
    public Map<String, Integer> getTopArtists(long userId) {
        Map<String, Integer> artistCount = new HashMap<>();

        // The SQL: Joins plays with songs, and groups by artist
        // Shows what artists got played and how many times
        String sql = "SELECT s.artist, COUNT(*) as play_count " +
                "FROM plays p " +
                "JOIN songs s ON p.song_id = s.id " +
                "WHERE p.user_id = ? " +
                "GROUP BY s.artist";

        try (Connection conn = getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            java.sql.ResultSet rs = pstmt.executeQuery();

            // Reads each row, moves on to the next row each iteration and checks if it exists
            while (rs.next()) {
                String rawArtist = rs.getString("artist");
                int count = rs.getInt("play_count");

                // We sanitize here to ensure "Drake" and "Drake - Topic"
                // form a single mathematical vector.
                String cleanArtist = MusicMath.sanitize(rawArtist);

                // Merge: If "drake" already exists, add the new count to it
                // Drake has 5 plays, DrakeVEVO has 3, now they merge so that drake has 8 plays
                artistCount.merge(cleanArtist, count, Integer::sum);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching top artist: " + e.getMessage());
            e.printStackTrace();
        }

        return artistCount;
    }
}
