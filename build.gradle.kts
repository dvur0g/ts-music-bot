plugins {
    alias(libs.plugins.spotless) apply false
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "jvm-test-suite")
    apply(plugin = "checkstyle")
    apply(plugin = "com.diffplug.spotless")

    repositories {
        mavenCentral()
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    configure<CheckstyleExtension> {
        toolVersion = "10.20.2"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            googleJavaFormat()
            target("src/*/java/**/*.java")
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint()
        }
    }

    configure<TestingExtension> {
        suites {
            named<JvmTestSuite>("test") {
                useJUnitJupiter(rootProject.libs.versions.junit.jupiter)
                dependencies {
                    implementation(rootProject.libs.assertj.core)
                }
            }
        }
    }

    tasks.named("check") {
        dependsOn(tasks.named("test"))
    }
}
