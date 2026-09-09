plugins {
    application
}

dependencies {
    implementation(project(":common"))
    implementation(libs.teamspeak3.api)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
}

application {
    mainClass.set("com.tsmusicbot.backend.Main")
}
