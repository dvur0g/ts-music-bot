package com.tsmusicbot.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PlayRequestTest {

  @Test
  void roundTripsThroughJson() throws Exception {
    var request = new PlayRequest("https://example.com/audio.m4a", "SomeUser", "Some Track Title");

    var json = Json.mapper().writeValueAsString(request);
    var deserialized = Json.mapper().readValue(json, PlayRequest.class);

    assertThat(deserialized).isEqualTo(request);
  }

  @Test
  void rejectsNullMediaUrl() {
    assertThatThrownBy(() -> new PlayRequest(null, "SomeUser", "Title"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsNullRequestedBy() {
    assertThatThrownBy(() -> new PlayRequest("https://example.com/audio.m4a", null, "Title"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsNullTitle() {
    assertThatThrownBy(() -> new PlayRequest("https://example.com/audio.m4a", "SomeUser", null))
        .isInstanceOf(NullPointerException.class);
  }
}
