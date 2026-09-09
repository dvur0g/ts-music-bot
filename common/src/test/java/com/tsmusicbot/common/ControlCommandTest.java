package com.tsmusicbot.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ControlCommandTest {

  @Test
  void roundTripsJoinThroughTheSealedInterface() throws Exception {
    ControlCommand command = new ControlCommand.Join("Music Channel");

    var json = Json.mapper().writeValueAsString(command);
    var deserialized = Json.mapper().readValue(json, ControlCommand.class);

    assertThat(json).contains("\"type\":\"JOIN\"");
    assertThat(deserialized).isEqualTo(command).isInstanceOf(ControlCommand.Join.class);
  }

  @Test
  void roundTripsPlayThroughTheSealedInterface() throws Exception {
    var request = new PlayRequest("https://example.com/audio.m4a", "SomeUser", "Some Track");
    ControlCommand command = new ControlCommand.Play(request);

    var json = Json.mapper().writeValueAsString(command);
    var deserialized = Json.mapper().readValue(json, ControlCommand.class);

    assertThat(json).contains("\"type\":\"PLAY\"");
    assertThat(deserialized).isEqualTo(command);
    assertThat(((ControlCommand.Play) deserialized).request()).isEqualTo(request);
  }

  @Test
  void roundTripsSetVolumeThroughTheSealedInterface() throws Exception {
    ControlCommand command = new ControlCommand.SetVolume(65);

    var json = Json.mapper().writeValueAsString(command);
    var deserialized = Json.mapper().readValue(json, ControlCommand.class);

    assertThat(json).contains("\"type\":\"SET_VOLUME\"");
    assertThat(deserialized).isEqualTo(command);
  }

  @Test
  void roundTripsEachNoArgCommandThroughTheSealedInterface() throws Exception {
    for (ControlCommand command :
        new ControlCommand[] {
          new ControlCommand.Leave(),
          new ControlCommand.Pause(),
          new ControlCommand.Resume(),
          new ControlCommand.Stop(),
        }) {
      var json = Json.mapper().writeValueAsString(command);
      var deserialized = Json.mapper().readValue(json, ControlCommand.class);

      assertThat(deserialized).isEqualTo(command);
    }
  }

  @Test
  void rejectsVolumeBelowZero() {
    assertThatThrownBy(() -> new ControlCommand.SetVolume(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsVolumeAboveOneHundred() {
    assertThatThrownBy(() -> new ControlCommand.SetVolume(101))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullChannelNameOnJoin() {
    assertThatThrownBy(() -> new ControlCommand.Join(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsNullRequestOnPlay() {
    assertThatThrownBy(() -> new ControlCommand.Play(null))
        .isInstanceOf(NullPointerException.class);
  }
}
