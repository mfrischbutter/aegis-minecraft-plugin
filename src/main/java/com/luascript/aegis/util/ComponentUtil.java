package com.luascript.aegis.util;

import com.luascript.aegis.service.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Map;

/**
 * Utility class for working with Kyori Adventure Components.
 */
public class ComponentUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String PREFIX = "<dark_red>[<red>Aegis</red>]</dark_red> ";

    private static MessageManager messageManager;

    /**
     * Set the MessageManager instance for localized messages.
     *
     * @param manager MessageManager instance
     */
    public static void setMessageManager(MessageManager manager) {
        messageManager = manager;
    }

    /**
     * Parse a MiniMessage string to a Component.
     *
     * @param message MiniMessage formatted string
     * @return Component
     */
    public static Component parse(String message) {
        return MINI_MESSAGE.deserialize(message);
    }

    /**
     * Create a success message with prefix.
     *
     * @param message Message text
     * @return Component
     */
    public static Component success(String message) {
        return parse(PREFIX + "<green>" + message + "</green>");
    }

    /**
     * Create an error message with prefix.
     *
     * @param message Message text
     * @return Component
     */
    public static Component error(String message) {
        return parse(PREFIX + "<red>" + message + "</red>");
    }

    /**
     * Create a warning message with prefix.
     *
     * @param message Message text
     * @return Component
     */
    public static Component warning(String message) {
        return parse(PREFIX + "<yellow>" + message + "</yellow>");
    }

    /**
     * Create an info message with prefix.
     *
     * @param message Message text
     * @return Component
     */
    public static Component info(String message) {
        return parse(PREFIX + "<gray>" + message + "</gray>");
    }

    /**
     * Create a plain message with prefix.
     *
     * @param message Message text
     * @return Component
     */
    public static Component message(String message) {
        return parse(PREFIX + "<white>" + message + "</white>");
    }

    /**
     * Create a ban message component.
     *
     * @param reason Ban reason
     * @param expiration Expiration string (or "Permanent")
     * @param banId Ban ID
     * @return Component
     */
    public static Component banMessage(String reason, String expiration, Long banId) {
        String headerText = messageManager != null ?
                messageManager.getMessage("disconnect.ban_header") :
                "Du bist von diesem Server gebannt!";
        String reasonLabel = messageManager != null ?
                messageManager.getMessage("disconnect.ban_reason") :
                "Grund: ";
        String expiresLabel = messageManager != null ?
                messageManager.getMessage("disconnect.ban_expires") :
                "Läuft ab: ";
        String banIdLabel = messageManager != null ?
                messageManager.getMessage("disconnect.ban_id") :
                "Ban-ID: #";

        return Component.text()
                .append(Component.newline())
                .append(Component.text(headerText, NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text(reasonLabel, NamedTextColor.GRAY))
                .append(Component.text(reason, NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text(expiresLabel, NamedTextColor.GRAY))
                .append(Component.text(expiration, NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text(banIdLabel + banId, NamedTextColor.DARK_GRAY))
                .append(Component.newline())
                .build();
    }

    /**
     * Create a kick message component.
     *
     * @param reason Kick reason
     * @param issuer Staff member who issued the kick
     * @return Component
     */
    public static Component kickMessage(String reason, String issuer) {
        String headerText = messageManager != null ?
                messageManager.getMessage("kick.message_header") :
                "Du wurdest gekickt!";
        String reasonLabel = messageManager != null ?
                messageManager.getMessage("kick.message_reason") :
                "Grund: ";
        String kickedByLabel = messageManager != null ?
                messageManager.getMessage("kick.message_kicked_by") :
                "Gekickt von: ";

        return Component.text()
                .append(Component.newline())
                .append(Component.text(headerText, NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text(reasonLabel, NamedTextColor.GRAY))
                .append(Component.text(reason, NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text(kickedByLabel, NamedTextColor.GRAY))
                .append(Component.text(issuer, NamedTextColor.YELLOW))
                .append(Component.newline())
                .build();
    }

    /**
     * Create an IP ban message component.
     *
     * @return Component
     */
    public static Component ipBanMessage() {
        String message = messageManager != null ?
                messageManager.getMessage("disconnect.ip_banned") :
                "Deine IP-Adresse wurde von diesem Server gebannt.";
        return error(message);
    }

    /**
     * Create a hover text component with multiple lines.
     *
     * @param lines Lines of text
     * @return Component with hover text
     */
    public static Component createHoverText(String... lines) {
        var builder = Component.text();
        for (int i = 0; i < lines.length; i++) {
            builder.append(Component.text(lines[i]));
            if (i < lines.length - 1) {
                builder.append(Component.newline());
            }
        }
        return builder.build();
    }

    /**
     * Strip all formatting from a component and return plain text.
     *
     * @param component Component to strip
     * @return Plain text string
     */
    public static String toPlainText(Component component) {
        return MiniMessage.miniMessage().stripTags(
                MiniMessage.miniMessage().serialize(component)
        );
    }
}
