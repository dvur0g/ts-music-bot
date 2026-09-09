package com.tsmusicbot.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BackendConfigTest {

  private static Map<String, String> completeEnv() {
    Map<String, String> env = new HashMap<>();
    env.put("TS_HOST", "ts.example.com");
    env.put("TS_QUERY_USERNAME", "serveradmin");
    env.put("TS_QUERY_PASSWORD", "hunter2");
    return env;
  }

  @Test
  void readsAllValuesWhenFullyConfigured() {
    Map<String, String> env = completeEnv();
    env.put("TS_QUERY_PORT", "10022");
    env.put("TS_VIRTUAL_SERVER_ID", "2");
    env.put("TS_NICKNAME", "Custom Name");

    BackendConfig config = BackendConfig.fromEnv(env::get);

    assertThat(config.host()).isEqualTo("ts.example.com");
    assertThat(config.queryPort()).isEqualTo(10022);
    assertThat(config.queryUsername()).isEqualTo("serveradmin");
    assertThat(config.queryPassword()).isEqualTo("hunter2");
    assertThat(config.virtualServerId()).isEqualTo(2);
    assertThat(config.nickname()).isEqualTo("Custom Name");
  }

  @Test
  void appliesDefaultsWhenOptionalValuesAreMissing() {
    BackendConfig config = BackendConfig.fromEnv(completeEnv()::get);

    assertThat(config.queryPort()).isEqualTo(10011);
    assertThat(config.virtualServerId()).isEqualTo(1);
    assertThat(config.nickname()).isEqualTo("MusicBot [Query]");
  }

  @Test
  void rejectsMissingHost() {
    Map<String, String> env = completeEnv();
    env.remove("TS_HOST");

    assertThatThrownBy(() -> BackendConfig.fromEnv(env::get))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("TS_HOST");
  }

  @Test
  void rejectsMissingQueryUsername() {
    Map<String, String> env = completeEnv();
    env.remove("TS_QUERY_USERNAME");

    assertThatThrownBy(() -> BackendConfig.fromEnv(env::get))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("TS_QUERY_USERNAME");
  }

  @Test
  void rejectsMissingQueryPassword() {
    Map<String, String> env = completeEnv();
    env.remove("TS_QUERY_PASSWORD");

    assertThatThrownBy(() -> BackendConfig.fromEnv(env::get))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("TS_QUERY_PASSWORD");
  }

  @Test
  void rejectsNonNumericQueryPort() {
    Map<String, String> env = completeEnv();
    env.put("TS_QUERY_PORT", "not-a-number");

    assertThatThrownBy(() -> BackendConfig.fromEnv(env::get))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("TS_QUERY_PORT");
  }
}
