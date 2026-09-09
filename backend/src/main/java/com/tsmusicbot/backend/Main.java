package com.tsmusicbot.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entry point for the backend process: ServerQuery chat listener, command dispatch, queue. */
public final class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);

  private Main() {}

  public static void main(String[] args) {
    log.info("ts-music-bot backend starting");
  }
}
