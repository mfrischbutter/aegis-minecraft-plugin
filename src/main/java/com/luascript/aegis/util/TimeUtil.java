package com.luascript.aegis.util;

import com.luascript.aegis.exception.ValidationException;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing and formatting time durations.
 * Supports formats like: "1d", "2h 30m", "1w 3d 12h", "permanent", "perm"
 */
public class TimeUtil {

    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhdwMy])");

    /**
     * Parse a duration string into a Duration object.
     * Supported units: s (seconds), m (minutes), h (hours), d (days), w (weeks), M (months), y (years)
     *
     * @param input Duration string (e.g., "1d", "2h 30m", "1w 3d")
     * @return Duration object
     * @throws ValidationException if the input is invalid
     */
    public static Duration parseDuration(String input) throws ValidationException {
        if (input == null || input.isBlank()) {
            throw new ValidationException("Duration cannot be empty");
        }

        input = input.toLowerCase().trim();

        // Check for permanent ban indicators
        if (input.equals("permanent") || input.equals("perm") || input.equals("forever")) {
            return null; // null indicates permanent
        }

        Matcher matcher = DURATION_PATTERN.matcher(input);
        long totalSeconds = 0;

        boolean foundMatch = false;
        while (matcher.find()) {
            foundMatch = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            totalSeconds += switch (unit) {
                case "s" -> value;
                case "m" -> value * 60;
                case "h" -> value * 3600;
                case "d" -> value * 86400;
                case "w" -> value * 604800;
                case "M" -> value * 2592000; // 30 days
                case "y" -> value * 31536000; // 365 days
                default -> throw new ValidationException("Unknown time unit: " + unit);
            };
        }

        if (!foundMatch) {
            throw new ValidationException("Invalid duration format. Use formats like: 1d, 2h 30m, 1w 3d");
        }

        if (totalSeconds <= 0) {
            throw new ValidationException("Duration must be positive");
        }

        return Duration.ofSeconds(totalSeconds);
    }

    /**
     * Calculate expiration instant from duration.
     *
     * @param duration Duration (null means permanent)
     * @return Instant when it expires, or null for permanent
     */
    public static Instant getExpirationInstant(Duration duration) {
        if (duration == null) {
            return null; // Permanent
        }
        return Instant.now().plus(duration);
    }

    /**
     * Format a duration into a human-readable string.
     *
     * @param duration Duration to format
     * @return Formatted string (e.g., "2 days 3 hours")
     */
    public static String formatDuration(Duration duration) {
        if (duration == null) {
            return "Permanent";
        }

        long seconds = duration.getSeconds();
        if (seconds == 0) {
            return "0 seconds";
        }

        StringBuilder result = new StringBuilder();

        long years = seconds / 31536000;
        if (years > 0) {
            result.append(years).append(" year").append(years > 1 ? "s" : "").append(" ");
            seconds %= 31536000;
        }

        long months = seconds / 2592000;
        if (months > 0) {
            result.append(months).append(" month").append(months > 1 ? "s" : "").append(" ");
            seconds %= 2592000;
        }

        long weeks = seconds / 604800;
        if (weeks > 0) {
            result.append(weeks).append(" week").append(weeks > 1 ? "s" : "").append(" ");
            seconds %= 604800;
        }

        long days = seconds / 86400;
        if (days > 0) {
            result.append(days).append(" day").append(days > 1 ? "s" : "").append(" ");
            seconds %= 86400;
        }

        long hours = seconds / 3600;
        if (hours > 0) {
            result.append(hours).append(" hour").append(hours > 1 ? "s" : "").append(" ");
            seconds %= 3600;
        }

        long minutes = seconds / 60;
        if (minutes > 0) {
            result.append(minutes).append(" minute").append(minutes > 1 ? "s" : "").append(" ");
            seconds %= 60;
        }

        if (seconds > 0) {
            result.append(seconds).append(" second").append(seconds > 1 ? "s" : "");
        }

        return result.toString().trim();
    }

    /**
     * Format an instant to a human-readable date/time string.
     *
     * @param instant Instant to format
     * @return Formatted string
     */
    public static String formatInstant(Instant instant) {
        if (instant == null) {
            return "Never";
        }

        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(java.time.ZoneId.systemDefault());

        return formatter.format(instant);
    }

    /**
     * Format time remaining until an instant.
     *
     * @param expiration Expiration instant
     * @return Formatted string (e.g., "2 days remaining")
     */
    public static String formatTimeRemaining(Instant expiration) {
        if (expiration == null) {
            return "Permanent";
        }

        Instant now = Instant.now();
        if (now.isAfter(expiration)) {
            return "Expired";
        }

        Duration remaining = Duration.between(now, expiration);
        return formatDuration(remaining) + " remaining";
    }

    /**
     * Check if an instant has expired.
     *
     * @param expiration Expiration instant (null means never expires)
     * @return true if expired, false otherwise
     */
    public static boolean isExpired(Instant expiration) {
        return expiration != null && Instant.now().isAfter(expiration);
    }
}
