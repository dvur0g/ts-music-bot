package com.tsmusicbot.backend.teamspeak;

/**
 * A chat message received over ServerQuery, already unwrapped from the library's event type so the
 * rest of the backend doesn't depend on {@code teamspeak3-api} event classes directly.
 *
 * @param senderName the display name of the TeamSpeak client who sent the message
 * @param senderUniqueId the TeamSpeak unique ID of the sender, used for the permission allowlist
 * @param text the raw message text, unparsed
 * @param target whether this was a server, channel, or private message
 */
public record ChatMessage(String senderName, String senderUniqueId, String text, Target target) {

  /** Where a chat message was sent, independent of {@code teamspeak3-api}'s own enum. */
  public enum Target {
    SERVER,
    CHANNEL,
    PRIVATE
  }
}
