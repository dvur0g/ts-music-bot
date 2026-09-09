package com.tsmusicbot.common;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;

/**
 * A command sent from the backend to voice-agent's control API. This is a closed set — voice-agent
 * never originates commands, it only executes them and reports {@link PlaybackStatus} back.
 *
 * <p>Encoded on the wire with a {@code type} discriminator field so a single JSON payload can be
 * deserialized back into the correct variant.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = ControlCommand.Join.class, name = "JOIN"),
  @JsonSubTypes.Type(value = ControlCommand.Leave.class, name = "LEAVE"),
  @JsonSubTypes.Type(value = ControlCommand.Play.class, name = "PLAY"),
  @JsonSubTypes.Type(value = ControlCommand.Pause.class, name = "PAUSE"),
  @JsonSubTypes.Type(value = ControlCommand.Resume.class, name = "RESUME"),
  @JsonSubTypes.Type(value = ControlCommand.Stop.class, name = "STOP"),
  @JsonSubTypes.Type(value = ControlCommand.SetVolume.class, name = "SET_VOLUME"),
})
public sealed interface ControlCommand {

  /** Join the given channel, connecting the real TeamSpeak client if not already connected. */
  record Join(String channelName) implements ControlCommand {
    public Join {
      Objects.requireNonNull(channelName, "channelName");
    }
  }

  /** Disconnect the real TeamSpeak client from the server. */
  record Leave() implements ControlCommand {}

  /** Start playing the given track, replacing whatever is currently playing. */
  record Play(PlayRequest request) implements ControlCommand {
    public Play {
      Objects.requireNonNull(request, "request");
    }
  }

  /** Pause the current track. No-op if idle or already paused. */
  record Pause() implements ControlCommand {}

  /** Resume a paused track. No-op if idle or already playing. */
  record Resume() implements ControlCommand {}

  /** Stop playback and discard the current track. */
  record Stop() implements ControlCommand {}

  /** Set playback volume as a percentage. */
  record SetVolume(int percent) implements ControlCommand {
    public SetVolume {
      if (percent < 0 || percent > 100) {
        throw new IllegalArgumentException("percent must be between 0 and 100, was " + percent);
      }
    }
  }
}
