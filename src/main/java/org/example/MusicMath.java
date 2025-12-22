package org.example;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicMath {

    // Helper to clean up artist names
    public static String sanitize(String input) {
        if (input == null) return "unknown";

        // Lowercase and trim whitespace
        String clean = input.toLowerCase().trim();

        // Simple cleaners for common Youtube junk
        clean = clean.replace(" - topic", "");
        clean = clean.replace("official video", "");
        clean = clean.replace("official audio", "");

        // Account for ft. or feat
        String[] seperators = {" ft", " feat", " featuring"};

        for (String sep : seperators) {
            int index = clean.indexOf(sep);
            if (index != -1) {
                // Cut the string right before the seperator
                clean = clean.substring(0, index);
            }
        }

        return clean.trim();
    }

    /** The math engine
     * Calculates cosine similarity using CBS inequality ( (U * V) / (||U|| * ||V||)
     * @return double between 0.0 (no similarity) and 1.0 (identical taste)
     * Angles beyond 90 degrees are not possible because count >= 0 (so disregard -1 to 0)
     */
    public static double calculateCosineSimilarity(Map<String, Integer> userA, Map<String, Integer> userB) {
        double dotProduct = 0.0;

        // Iterate over the smaller map for performance
        // Uses Sparse Vector Product using the logic that a small map with 5 entries to a larger one with 10,000 only
        // the 5 matter since the other 9,995 are automatically 0 (anything * 0 = 0)
        Map<String, Integer> smallerMap = (userA.size() < userB.size()) ? userA : userB;
        Map<String, Integer> largerMap = (userA.size() < userB.size()) ? userB : userA;

        for (Map.Entry<String, Integer> entry : smallerMap.entrySet()) {
            String artist = entry.getKey();
            int countA = entry.getValue();

            // Check if the other user (user with more entries) also listened to this artist
            if (largerMap.containsKey(artist)) {
                int countB = largerMap.get(artist);
                dotProduct += (countA * countB);
            }
        }

        // Calculate magnitudes (||U|| * ||V||)
        double magnitudeA = getMagnitude(userA);
        double magnitudeB = getMagnitude(userB);

        // If someone played 0 songs, their magnitude is 0, and we cannot divide by 0
        if (magnitudeA == 0 || magnitudeB == 0) {
            return 0.0;
        }

        return dotProduct / (magnitudeA * magnitudeB);
    }

    private static double getMagnitude(Map<String, Integer> vector) {
        double sumOfSquares = 0.0;

        for (int count : vector.values()) {
            sumOfSquares += (count * count);
        }

        return Math.sqrt(sumOfSquares);
    }
}
