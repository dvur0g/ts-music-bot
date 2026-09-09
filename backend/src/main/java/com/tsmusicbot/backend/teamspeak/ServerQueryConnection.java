package com.tsmusicbot.backend.teamspeak;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
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
                    event.getTargetMode()));
          }
        });
    // Chat notifications only arrive for events the query has registered for; this covers
    // server, channel (in the query's current channel), and private text messages.
    api.registerAllEvents();

    return new ServerQueryConnection(query);
  }

  /** Disconnects the query and releases its resources. Safe to call more than once. */
  @Override
  public void close() {
    query.exit();
  }
}
