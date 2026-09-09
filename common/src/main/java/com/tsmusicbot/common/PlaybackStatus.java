package com.tsmusicbot.common;

/**
 * Playback state reported by voice-agent back to the backend, either as a response to a control
 * command or polled on demand.
 *
 * @param state the current playback state
 * @param currentTrackTitle the title of the track currently playing/paused, or {@code null} when
 *     {@link State#IDLE}
 * @param volumePercent current playback volume, {@code 0}-{@code 100}
 */
public record PlaybackStatus(State state, String currentTrackTitle, int volumePercent) {

  /** An idle status at the given volume, with no current track. */
  public static PlaybackStatus idle(int volumePercent) {
    return new PlaybackStatus(State.IDLE, null, volumePercent);
  }

  /** Playback state machine — see CLAUDE.md section 4. */
  public enum State {
    IDLE,
    PLAYING,
    PAUSED
  }
}
