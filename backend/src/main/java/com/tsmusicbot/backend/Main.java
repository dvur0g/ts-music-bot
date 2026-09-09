package com.tsmusicbot.backend;

import com.tsmusicbot.backend.config.BackendConfig;
import com.tsmusicbot.backend.teamspeak.ServerQueryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entry point for the backend process: ServerQuery chat listener, command dispatch, queue. */
public final class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);

  private Main() {}

  /** Starts the backend process. */
  public static void main(String[] args) {
    log.info("ts-music-bot backend starting");

    try {
      BackendConfig config = BackendConfig.fromEnv();
      ServerQueryConnection connection =
          ServerQueryConnection.connect(
              config,
              message ->
                  log.info(
                      "chat [{}] {} ({}): {}",
                      message.target(),
                      message.senderName(),
                      message.senderUniqueId(),
                      message.text()));

      Runtime.getRuntime().addShutdownHook(new Thread(connection::close, "shutdown"));

      log.info(
          "connected to {}:{} as \"{}\", listening for chat messages",
          config.host(),
          config.queryPort(),
          config.nickname());
    } catch (RuntimeException e) {
      log.error("Failed to start backend", e);
      System.exit(1);
    }
  }
}
