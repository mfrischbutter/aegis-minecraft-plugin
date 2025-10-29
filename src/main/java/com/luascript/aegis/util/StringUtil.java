package com.luascript.aegis.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for string operations.
 */
public class StringUtil {

    /**
     * Join an array of strings from a starting index.
     *
     * @param args Array of strings
     * @param startIndex Starting index (inclusive)
     * @param delimiter Delimiter to use
     * @return Joined string
     */
    public static String joinFrom(String[] args, int startIndex, String delimiter) {
        if (args == null || startIndex >= args.length) {
            return "";
        }

        return String.join(delimiter, Arrays.copyOfRange(args, startIndex, args.length));
    }

    /**
     * Join an array of strings from a starting index with space delimiter.
     *
     * @param args Array of strings
     * @param startIndex Starting index (inclusive)
     * @return Joined string
     */
    public static String joinFrom(String[] args, int startIndex) {
        return joinFrom(args, startIndex, " ");
    }

    /**
     * Truncate a string to a maximum length, adding ellipsis if truncated.
     *
     * @param text Text to truncate
     * @param maxLength Maximum length
     * @return Truncated string
     */
    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Capitalize the first letter of a string.
     *
     * @param text Text to capitalize
     * @return Capitalized string
     */
    public static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    /**
     * Convert a string to title case (first letter of each word capitalized).
     *
     * @param text Text to convert
     * @return Title case string
     */
    public static String toTitleCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String[] words = text.split("\\s+");
        return Arrays.stream(words)
                .map(StringUtil::capitalize)
                .collect(Collectors.joining(" "));
    }

    /**
     * Get tab completion matches for a partial input.
     *
     * @param input Partial input
     * @param options Available options
     * @return List of matching options
     */
    public static List<String> getMatches(String input, List<String> options) {
        if (input == null || input.isEmpty()) {
            return options;
        }

        String lowerInput = input.toLowerCase();
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(lowerInput))
                .collect(Collectors.toList());
    }

    /**
     * Check if a string is null or empty.
     *
     * @param text Text to check
     * @return true if null or empty
     */
    public static boolean isEmpty(String text) {
        return text == null || text.isEmpty();
    }

    /**
     * Check if a string is null, empty, or contains only whitespace.
     *
     * @param text Text to check
     * @return true if blank
     */
    public static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    /**
     * Repeat a string n times.
     *
     * @param text Text to repeat
     * @param times Number of times to repeat
     * @return Repeated string
     */
    public static String repeat(String text, int times) {
        if (text == null || times <= 0) {
            return "";
        }

        return text.repeat(times);
    }

    /**
     * Pluralize a word based on count.
     *
     * @param count Count
     * @param singular Singular form
     * @param plural Plural form
     * @return Appropriate form based on count
     */
    public static String pluralize(long count, String singular, String plural) {
        return count == 1 ? singular : plural;
    }

    /**
     * Pluralize a word by adding 's' if count is not 1.
     *
     * @param count Count
     * @param word Word to pluralize
     * @return Pluralized word
     */
    public static String pluralize(long count, String word) {
        return pluralize(count, word, word + "s");
    }
}
