package com.tsmusicbot.common;

import java.util.Objects;

/**
 * A single track to play, already resolved by the backend (see CLAUDE.md section 6: the backend
 * resolves a YouTube URL to a direct media URL via {@code yt-dlp} before this is ever created).
 *
 * @param mediaUrl a direct, playable media URL — not a YouTube page URL
 * @param requestedBy the display name of the TeamSpeak user who requested this track
 * @param title the resolved track title, shown back to users in chat and in {@code !queue}
 */
public record PlayRequest(String mediaUrl, String requestedBy, String title) {

  /** Validates that no required field is {@code null}. */
  public PlayRequest {
    Objects.requireNonNull(mediaUrl, "mediaUrl");
    Objects.requireNonNull(requestedBy, "requestedBy");
    Objects.requireNonNull(title, "title");
  }
}
