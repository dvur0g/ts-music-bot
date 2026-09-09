plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ts-music-bot"

include("common", "backend", "voice-agent")
