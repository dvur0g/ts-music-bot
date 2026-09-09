package com.tsmusicbot.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlaybackStatusTest {

  @Test
  void idleFactoryHasNoCurrentTrack() {
    var status = PlaybackStatus.idle(50);

    assertThat(status.state()).isEqualTo(PlaybackStatus.State.IDLE);
    assertThat(status.currentTrackTitle()).isNull();
    assertThat(status.volumePercent()).isEqualTo(50);
  }

  @Test
  void roundTripsThroughJson() throws Exception {
    var status = new PlaybackStatus(PlaybackStatus.State.PLAYING, "Some Track", 80);

    var json = Json.mapper().writeValueAsString(status);
    var deserialized = Json.mapper().readValue(json, PlaybackStatus.class);

    assertThat(deserialized).isEqualTo(status);
  }

  @Test
  void idleStatusRoundTripsWithNullTrackTitle() throws Exception {
    var status = PlaybackStatus.idle(30);

    var json = Json.mapper().writeValueAsString(status);
    var deserialized = Json.mapper().readValue(json, PlaybackStatus.class);

    assertThat(deserialized).isEqualTo(status);
  }
}
