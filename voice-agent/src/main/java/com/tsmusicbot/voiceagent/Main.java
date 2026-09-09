package com.tsmusicbot.voiceagent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entry point for the voice-agent process: real TS3 client control shim and audio playback. */
public final class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);

  private Main() {}

  /** Starts the voice-agent process. */
  public static void main(String[] args) {
    log.info("ts-music-bot voice-agent starting");
  }
}
