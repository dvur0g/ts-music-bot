package com.tsmusicbot.backend.teamspeak;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import com.tsmusicbot.backend.config.BackendConfig;
import java.util.function.Consumer;

/**
 * A live ServerQuery connection: logs in, selects the target virtual server, and forwards every
 * incoming chat message to a callback. See CLAUDE.md section 4 — this is the only component that
 * reads TeamSpeak chat; voice-agent never does.
 */
public final class ServerQueryConnection implements AutoCloseable {

  private final TS3Query query;

  private ServerQueryConnection(TS3Query query) {
    this.query = query;
  }

  /**
   * Connects to the TeamSpeak server described by {@code config}, logs in, and starts forwarding
   * every chat message (server, channel, and private) to {@code onChatMessage}.
   *
   * @throws com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException if the
   *     connection or login fails
   */
  public static ServerQueryConnection connect(
      BackendConfig config, Consumer<ChatMessage> onChatMessage) {
    TS3Config ts3Config = new TS3Config();
    ts3Config.setHost(config.host());
    ts3Config.setQueryPort(config.queryPort());
    ts3Config.setLoginCredentials(config.queryUsername(), config.queryPassword());

    TS3Query query = new TS3Query(ts3Config);
    query.connect();

    TS3Api api = query.getApi();
    api.selectVirtualServerById(config.virtualServerId(), config.nickname());
    api.addTS3Listeners(
        new TS3EventAdapter() {
          @Override
          public void onTextMessage(TextMessageEvent event) {
            onChatMessage.accept(
                new ChatMessage(
                    event.getInvokerName(),
                    event.getInvokerUniqueId(),
                    event.getMessage(),
                    toTarget(event.getTargetMode())));
          }
        });
    // Only subscribe to the text-message events we actually consume, not registerAllEvents()'s
    // full set (client join/leave, moves, channel/server edits, ...). Channel text is limited to
    // the channel the query is currently in; that limitation is inherent to ServerQuery itself.
    api.registerEvent(TS3EventType.TEXT_SERVER);
    api.registerEvent(TS3EventType.TEXT_CHANNEL);
    api.registerEvent(TS3EventType.TEXT_PRIVATE);

    return new ServerQueryConnection(query);
  }

  private static ChatMessage.Target toTarget(TextMessageTargetMode mode) {
    return switch (mode) {
      case SERVER -> ChatMessage.Target.SERVER;
      case CHANNEL -> ChatMessage.Target.CHANNEL;
      case CLIENT -> ChatMessage.Target.PRIVATE;
    };
  }

  /** Disconnects the query and releases its resources. Safe to call more than once. */
  @Override
  public void close() {
    query.exit();
  }
}
