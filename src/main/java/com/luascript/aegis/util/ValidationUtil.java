package com.luascript.aegis.util;

import com.luascript.aegis.exception.ValidationException;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Utility class for input validation.
 */
public class ValidationUtil {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    private static final int MIN_REASON_LENGTH = 3;
    private static final int MAX_REASON_LENGTH = 500;

    /**
     * Validate a Minecraft username.
     *
     * @param username Username to validate
     * @throws ValidationException if invalid
     */
    public static void validateUsername(String username) throws ValidationException {
        if (username == null || username.isBlank()) {
            throw new ValidationException("Username cannot be empty");
        }

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new ValidationException(
                    "Invalid username. Must be 3-16 characters and contain only letters, numbers, and underscores"
            );
        }
    }

    /**
     * Validate a UUID string.
     *
     * @param uuidString UUID string to validate
     * @return Parsed UUID
     * @throws ValidationException if invalid
     */
    public static UUID validateUuid(String uuidString) throws ValidationException {
        if (uuidString == null || uuidString.isBlank()) {
            throw new ValidationException("UUID cannot be empty");
        }

        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid UUID format: " + uuidString);
        }
    }

    /**
     * Validate a reason string.
     *
     * @param reason Reason to validate
     * @throws ValidationException if invalid
     */
    public static void validateReason(String reason) throws ValidationException {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Reason cannot be empty");
        }

        if (reason.length() < MIN_REASON_LENGTH) {
            throw new ValidationException(
                    "Reason must be at least " + MIN_REASON_LENGTH + " characters long"
            );
        }

        if (reason.length() > MAX_REASON_LENGTH) {
            throw new ValidationException(
                    "Reason cannot exceed " + MAX_REASON_LENGTH + " characters"
            );
        }
    }

    /**
     * Validate an IP address.
     *
     * @param ipAddress IP address to validate
     * @throws ValidationException if invalid
     */
    public static void validateIpAddress(String ipAddress) throws ValidationException {
        if (ipAddress == null || ipAddress.isBlank()) {
            throw new ValidationException("IP address cannot be empty");
        }

        if (!IP_ADDRESS_PATTERN.matcher(ipAddress).matches()) {
            throw new ValidationException("Invalid IP address format: " + ipAddress);
        }
    }

    /**
     * Validate a page number.
     *
     * @param page Page number (0-indexed)
     * @throws ValidationException if invalid
     */
    public static void validatePage(int page) throws ValidationException {
        if (page < 0) {
            throw new ValidationException("Page number must be non-negative");
        }
    }

    /**
     * Validate a page size.
     *
     * @param pageSize Page size
     * @throws ValidationException if invalid
     */
    public static void validatePageSize(int pageSize) throws ValidationException {
        if (pageSize <= 0) {
            throw new ValidationException("Page size must be positive");
        }

        if (pageSize > 100) {
            throw new ValidationException("Page size cannot exceed 100");
        }
    }

    /**
     * Validate that a string is not null or blank.
     *
     * @param value String to validate
     * @param fieldName Field name for error message
     * @throws ValidationException if invalid
     */
    public static void validateNotEmpty(String value, String fieldName) throws ValidationException {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " cannot be empty");
        }
    }

    /**
     * Validate that an object is not null.
     *
     * @param value Object to validate
     * @param fieldName Field name for error message
     * @throws ValidationException if null
     */
    public static void validateNotNull(Object value, String fieldName) throws ValidationException {
        if (value == null) {
            throw new ValidationException(fieldName + " cannot be null");
        }
    }
}
