package com.tsmusicbot.backend.config;

import java.util.Objects;
import java.util.function.Function;

/**
 * Backend configuration, read from environment variables (12-factor style — see CLAUDE.md section
 * 7). {@link #fromEnv()} reads real process environment variables; {@link #fromEnv(Function)} takes
 * an injected lookup function so the parsing/validation logic is unit-testable without touching the
 * real environment.
 *
 * @param host TeamSpeak server hostname or IP ({@code TS_HOST})
 * @param queryPort ServerQuery raw protocol port ({@code TS_QUERY_PORT}, default {@code 10011})
 * @param queryUsername ServerQuery login username ({@code TS_QUERY_USERNAME})
 * @param queryPassword ServerQuery login password ({@code TS_QUERY_PASSWORD})
 * @param virtualServerId the virtual server to select after connecting ({@code
 *     TS_VIRTUAL_SERVER_ID}, default {@code 1})
 * @param nickname the nickname the ServerQuery connection shows up as ({@code TS_NICKNAME}, default
 *     {@code "MusicBot [Query]"})
 */
public record BackendConfig(
    String host,
    int queryPort,
    String queryUsername,
    String queryPassword,
    int virtualServerId,
    String nickname) {

  private static final int DEFAULT_QUERY_PORT = 10011;
  private static final int DEFAULT_VIRTUAL_SERVER_ID = 1;
  private static final String DEFAULT_NICKNAME = "MusicBot [Query]";

  /** Validates that no required field is {@code null}. */
  public BackendConfig {
    Objects.requireNonNull(host, "host");
    Objects.requireNonNull(queryUsername, "queryUsername");
    Objects.requireNonNull(queryPassword, "queryPassword");
    Objects.requireNonNull(nickname, "nickname");
  }

  /** Reads configuration from the real process environment. */
  public static BackendConfig fromEnv() {
    return fromEnv(System::getenv);
  }

  /**
   * Reads configuration using the given environment lookup function.
   *
   * @throws IllegalStateException if a required variable is missing
   */
  public static BackendConfig fromEnv(Function<String, String> env) {
    return new BackendConfig(
        required(env, "TS_HOST"),
        optionalInt(env, "TS_QUERY_PORT", DEFAULT_QUERY_PORT),
        required(env, "TS_QUERY_USERNAME"),
        required(env, "TS_QUERY_PASSWORD"),
        optionalInt(env, "TS_VIRTUAL_SERVER_ID", DEFAULT_VIRTUAL_SERVER_ID),
        optionalString(env, "TS_NICKNAME", DEFAULT_NICKNAME));
  }

  private static String required(Function<String, String> env, String name) {
    String value = env.apply(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Missing required environment variable: " + name);
    }
    return value;
  }

  private static String optionalString(
      Function<String, String> env, String name, String defaultValue) {
    String value = env.apply(name);
    return (value == null || value.isBlank()) ? defaultValue : value;
  }

  private static int optionalInt(Function<String, String> env, String name, int defaultValue) {
    String value = env.apply(name);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException(
          "Environment variable " + name + " must be an integer, was: " + value, e);
    }
  }
}
