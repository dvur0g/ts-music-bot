# ts-music-bot

A self-hosted YouTube music bot for TeamSpeak 3, built in plain Java (Gradle,
multi-module), shipped as a two-container Docker Compose stack that starts
with a single command.

This document is the source of truth for the project's architecture and
conventions. Read it before making structural changes. If a decision recorded
here needs to change, update this file in the same commit.

## 1. Why this isn't a simple "Discord bot but for TeamSpeak"

A Discord bot can open one WebSocket + one voice UDP connection from a single
process and stream Opus frames straight into a channel. TeamSpeak does not
allow this:

- **ServerQuery** (`10011` raw / `10022` SSH) is TeamSpeak's text-based admin
  API. A query login can join as a "query client," read/send chat messages,
  move users, register for text-message events — but a query client has
  **no voice capability**. It cannot transmit audio, ever.
- Actually **speaking into a voice channel requires a real TeamSpeak client
  connection** using TeamSpeak's proprietary, undocumented voice/UDP
  protocol. There is no mature, maintained Java (or JVM) implementation of
  this protocol. C# has one (TSLib, used by TS3AudioBot); Java doesn't.

Given the constraint of staying in plain Java, the only realistic way to get
sound into a channel is to **run the real, official TeamSpeak 3 client**
headlessly and feed it audio through a virtual microphone, controlling it
via the client's built-in **ClientQuery** plugin (a documented local
loopback TCP control interface, distinct from ServerQuery). This is the
standard approach used by most self-hosted TS3 music bots that don't
reimplement the voice protocol.

That's why the project is two cooperating processes, not one:

| Concern | Component | Protocol used |
|---|---|---|
| Read chat commands, talk back in chat, resolve YouTube, manage queue | **backend** | ServerQuery |
| Actually join the channel and emit sound | **voice-agent** | Real TS3 client + ClientQuery + virtual audio |

**The backend is the only component that reads text chat.** The voice-agent
never parses commands — it is a dumb playback slave that backend tells what
to do. This was the open question in the original plan; resolved here.

## 2. Architecture

```
                         TeamSpeak Server
                         ┌───────────────┐
        ServerQuery      │               │      Real voice
        (text chat,      │               │      connection
        control)         │               │      (audio in channel)
             ▲            └───────┬───────┘             ▲
             │                    │                     │
     ┌───────┴────────┐           │            ┌────────┴─────────┐
     │    backend      │  control (HTTP/TCP)   │    voice-agent    │
     │  (plain Java)   ├───────────────────────►│  (Xvfb + real TS3 │
     │                 │  play/stop/volume/...  │   client + Pulse- │
     │  - chat parser  │                        │   Audio + a thin  │
     │  - command      │                        │   Java control    │
     │    dispatcher   │                        │   shim)           │
     │  - queue/state  │                        │                   │
     │  - yt-dlp       │                        │  - ClientQuery    │
     │    resolver     │                        │    automation     │
     └────────┬────────┘                        │  - ffmpeg decode  │
              │                                  │  - virtual mic   │
              │ shared DTOs (common module)      │    (PulseAudio    │
              └─────────────────────────────────►│     null sink)   │
                                                  └───────────────────┘
```

Two Docker containers, one Docker Compose stack, one network. Audio itself
never crosses the network between containers as raw bytes — the backend
resolves *what* to play (a direct media URL via `yt-dlp`) and tells
voice-agent to play it; voice-agent owns `ffmpeg`, PulseAudio, and the real
client, since decoding and audio output need to live next to each other.

## 3. Repository layout

```
ts-music-bot/
├── CLAUDE.md
├── README.md
├── settings.gradle.kts
├── build.gradle.kts              # shared conventions (Java toolchain, spotless, checkstyle)
├── gradle/
│   └── libs.versions.toml        # version catalog — single source of dependency versions
├── common/                       # shared DTOs/protocol records, no framework deps
│   ├── build.gradle.kts
│   └── src/main/java/com/tsmusicbot/common/...
├── backend/                      # ServerQuery client, command parsing, queue, yt-dlp
│   ├── build.gradle.kts
│   ├── Dockerfile
│   └── src/{main,test}/java/com/tsmusicbot/backend/...
├── voice-agent/                  # control shim + Dockerfile that builds the real-client image
│   ├── build.gradle.kts
│   ├── Dockerfile
│   ├── docker/                   # entrypoint scripts, pulseaudio + ts3 client config
│   └── src/{main,test}/java/com/tsmusicbot/voiceagent/...
├── docker-compose.yml
└── .env.example
```

## 4. Module responsibilities

### `common`
Pure data: `PlayRequest`, `PlaybackStatus`, `ControlCommand` (records),
serialized as JSON between backend and voice-agent. No TeamSpeak-specific or
framework code lives here — it's the contract the two other modules share.

### `backend`
- Connects to the TeamSpeak server over **ServerQuery** using
  `com.github.theholywaffle:teamspeak3-api`.
- Registers for `textchannel`/`textserver` notify events and parses incoming
  messages for commands (`!play`, `!skip`, `!pause`, `!resume`, `!stop`,
  `!queue`, `!volume`, `!leave`, `!help`).
- Enforces permissions (allowlist of client unique IDs or server groups,
  configurable — see §8).
- Resolves YouTube URLs to a direct, streamable audio URL using `yt-dlp`
  (invoked as an external process — there is no reliable pure-Java YouTube
  extractor, and reimplementing one is out of scope and a maintenance trap).
- Owns the play queue and playback state machine (idle / playing / paused).
- Talks to `voice-agent`'s control API to start/stop/adjust playback and to
  join/leave a channel.
- Sends status/errors back into TeamSpeak chat via ServerQuery
  (`sendtextmessage`) — since it already holds the chat connection, it is
  also the one that replies.
- Uses the JDK's built-in `com.sun.net.httpserver.HttpServer` for its own
  control API rather than pulling in a web framework — keeps the "plain
  Java" constraint honest. Reach for a lightweight library only if the
  hand-rolled server becomes a real maintenance burden.

### `voice-agent`
- Base image: a minimal Debian/Ubuntu with `Xvfb`, `pulseaudio`, the
  official TeamSpeak 3 client, `ffmpeg`, and a JRE.
- Startup script: launches `Xvfb`, starts `pulseaudio` with a null sink
  configured as the default source (the "virtual microphone"), launches the
  TS3 client pointed at that virtual mic with the ClientQuery plugin
  enabled, then starts the small Java control shim.
- The **Java control shim** exposes a minimal HTTP/TCP API (defined by the
  `common` module's DTOs) that:
  - drives the client via **ClientQuery** (connect to server/channel, set
    nickname, move channel, disconnect);
  - spawns/kills an `ffmpeg` process per play request that decodes the
    resolved media URL and writes PCM straight into the PulseAudio virtual
    sink;
  - reports playback status back to backend on request.
- Never parses TeamSpeak chat and never talks ServerQuery.

## 5. TeamSpeak-specific implementation notes

- **Identity persistence**: the real TS3 client's identity (private key)
  must be generated once and persisted in a named Docker volume
  (`voice_agent_identity`). Regenerating it on every container restart
  looks like a new, unverified client each time and can trip server-side
  abuse/flood protection.
- **ServerQuery login**: use a dedicated query login (created via
  `serverqueryaddlogin` or the initial admin token), not the primary admin
  account. Least privilege: it only needs rights to read/send chat and see
  the client list.
- **ClientQuery**: TeamSpeak's official client ships a plugin
  (`ClientQuery`) that opens a local loopback TCP interface (default port
  `25639`) for automation — this is the documented, supported way to script
  the real client. It must be enabled in the client's config baked into the
  voice-agent image/volume.
- **Naming**: give the ServerQuery connection and the real voice client
  visibly different nicknames (e.g. `MusicBot [Query]` vs `MusicBot`) so
  users in the client list understand there are two bot-controlled entities
  and don't try to `/msg` the wrong one.
- **Rate limiting**: ServerQuery connections are subject to TeamSpeak's
  command flood protection; the backend must not poll or send faster than a
  few commands per second.

## 6. Audio pipeline

1. User sends `!play <youtube-url>` in a text channel.
2. Backend resolves the URL with `yt-dlp -f bestaudio -g <url>` to get a
   direct, time-limited media URL (no download to disk needed).
3. Backend enqueues a `PlayRequest{ url, requestedBy, title }` and, if idle,
   sends it to voice-agent's control API.
4. Voice-agent runs
   `ffmpeg -re -i <url> -f s16le -ar 48000 -ac 2 pulse://<virtual-sink>`
   (exact args to be finalized during implementation), which streams decoded
   PCM into the virtual microphone the TS3 client is listening on as input.
5. TS3 client transmits that "microphone" input into the voice channel as
   normal voice.
6. On track end/skip/stop, voice-agent kills the `ffmpeg` process and
   reports idle; backend advances the queue.

## 7. Docker / one-command startup

Everything runs via Docker Compose; a single `docker compose up -d --build`
(or a `Makefile`/`run.sh` wrapper around it, if the user prefers a shorter
command) brings up both containers.

```yaml
# docker-compose.yml (shape, not final)
services:
  backend:
    build: ./backend
    env_file: .env
    depends_on:
      - voice-agent
    restart: unless-stopped

  voice-agent:
    build: ./voice-agent
    env_file: .env
    volumes:
      - voice_agent_identity:/data/identity
    restart: unless-stopped
    # no ports need to be published to the host; both services
    # talk to each other over the compose network

volumes:
  voice_agent_identity:
```

Configuration is entirely via a `.env` file (see `.env.example`), following
12-factor: TeamSpeak host/port, ServerQuery credentials, target channel,
command prefix, permission allowlist, log level. No secrets committed to
the repo.

## 8. Command set (v1)

| Command | Effect |
|---|---|
| `!play <url>` | Resolve and enqueue a YouTube URL; start playing if idle |
| `!skip` | Stop current track, advance queue |
| `!pause` / `!resume` | Pause/resume current playback |
| `!stop` | Clear queue and stop |
| `!queue` | List upcoming tracks in chat |
| `!volume <0-100>` | Adjust playback volume |
| `!leave` | Disconnect voice-agent from the channel |
| `!help` | List commands |

Permission model: a configurable list of allowed TeamSpeak client unique IDs
and/or server group IDs. Unauthorized users get a polite chat reply, not a
silent ignore.

## 9. Build & tooling conventions

- **Java 21** (LTS) toolchain, enforced via Gradle's toolchain API so local
  and container builds match regardless of host JDK.
- **Gradle Kotlin DSL**, multi-module, with a version catalog
  (`gradle/libs.versions.toml`) as the single place dependency versions are
  pinned.
- **Formatting/linting**: Spotless (google-java-format) + Checkstyle,
  wired into `./gradlew check` and CI. No unformatted code merges.
- **Testing**: JUnit 5 + AssertJ. Command parsing, queue state machine, and
  permission checks get unit tests. The ServerQuery and ClientQuery
  integrations get thin adapters so the business logic behind them is
  testable without a live TeamSpeak server.
- **Packaging**: each runnable module (`backend`, `voice-agent`) uses the
  `application` plugin; Docker images are built via plain multi-stage
  Dockerfiles (Gradle build stage → slim JRE runtime stage). `voice-agent`'s
  runtime stage additionally needs the TS3 client, Xvfb, PulseAudio, and
  ffmpeg, so it can't use a distroless/JRE-only base — it's a fuller OS
  image by necessity.
- **Logging**: SLF4J + Logback, structured (JSON) output optional but the
  logger abstraction is used everywhere from day one — no `System.out`.
- **CI**: GitHub Actions running `./gradlew check` (build, test, lint) on
  every push/PR, plus a job that builds both Docker images to catch
  Dockerfile breakage early.

## 10. Non-goals (v1)

- No other audio sources (Spotify, SoundCloud, local files) — YouTube only,
  revisit once the core pipeline is solid.
- No web UI/dashboard — chat commands only.
- No multi-server support — one bot instance targets one TeamSpeak server
  and one voice channel per deployment.
- No attempt to reimplement the TeamSpeak voice protocol in Java — the
  real-client-plus-virtual-audio approach is deliberate, not a stopgap.

## 11. Legal/ToS note

Downloading/extracting audio via `yt-dlp` and running an automated client
against a TeamSpeak server both sit in a gray area relative to YouTube's and
TeamSpeak's terms of service. This is intended for personal/self-hosted use
against servers you have the right to administer. Don't distribute this as
a public service.

## 12. Suggested implementation order

Build bottom-up so each phase is independently testable before the next
depends on it:

1. **Scaffold** — root Gradle multi-module setup (`common`, `backend`,
   `voice-agent`), version catalog, Spotless/Checkstyle, empty `application`
   entry points that just log "started" for each module. Get CI green on an
   empty skeleton first.
2. **`common` DTOs** — `PlayRequest`, `PlaybackStatus`, `ControlCommand`,
   with unit-tested JSON (de)serialization. Nothing else depends on
   TeamSpeak yet.
3. **Backend: ServerQuery connectivity** — connect, log in, register for
   text-message events, log every incoming chat message. Verify against a
   real (or disposable test) TeamSpeak server before building anything on
   top.
4. **Backend: command parsing + permissions** — parse `!play`/`!skip`/etc.
   into typed commands, enforce the allowlist, reply in chat. Fully
   unit-testable without a live server via a fake ServerQuery adapter.
5. **Backend: YouTube resolution** — wrap `yt-dlp` as an external process,
   resolve a URL to a direct media link, handle failures (private/removed
   videos, non-YouTube links) with a clear chat reply.
6. **Voice-agent: real client boots headlessly** — Xvfb + PulseAudio null
   sink + official TS3 client connecting to a test server with a persisted
   identity volume, verified manually before automating it.
7. **Voice-agent: ClientQuery control shim** — the Java control API that
   can join/leave a channel and report status, driven manually (curl) first.
8. **Voice-agent: audio playback** — wire `ffmpeg` to decode a resolved URL
   into the virtual sink; confirm audio is audible in the test channel.
9. **Wire backend → voice-agent** — full `!play` end-to-end: chat command →
   resolve → control API call → audible playback → status back to chat.
10. **Queue + playback controls** — `!skip`/`!pause`/`!resume`/`!stop`/
    `!queue`/`!volume` against the now-working single-track pipeline.
11. **Docker Compose hardening** — restart policies, `.env`-driven config,
    named volumes, confirm the whole stack survives `docker compose down &&
    docker compose up -d` with identity and no re-registration.
12. **Polish** — structured logging, `!help`, error-message consistency,
    README with setup instructions for a new deployer.

Treat each numbered step as a mergeable unit of work with its own tests
where feasible, rather than one large branch.

## 13. Open decisions to revisit during implementation

- Exact control-API transport between backend and voice-agent (plain JSON
  over HTTP via `HttpServer` is the default plan; revisit only if it proves
  awkward).
- Whether volume control is done in `ffmpeg` (per-track `-af volume=`) or at
  the PulseAudio sink level (global, adjustable mid-track). Leaning
  PulseAudio sink for the mid-track case.
- Whether the backend downloads audio to a temp file vs. lets `ffmpeg`
  stream directly from the resolved URL. Leaning direct streaming to avoid
  disk I/O and cleanup.
