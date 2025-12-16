plugins {
    java
    application
}

group = "com.trafficanalyzer"
version = "1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

val osName = System.getProperty("os.name").lowercase()
val platform = when {
    osName.contains("win") -> "win"
    osName.contains("mac") -> "mac"
    osName.contains("nux") -> "linux"
    else -> error("Unsupported OS: $osName")
}

dependencies {
    // JavaFX
    implementation("org.openjfx:javafx-controls:21:${platform}")
    implementation("org.openjfx:javafx-graphics:21:${platform}")
    implementation("org.openjfx:javafx-base:21:${platform}")

    // Log4j2
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")

    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.0")
}

application {
    mainClass.set("com.trafficanalyzer.MainApp")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaExec> {
    val javaFxLibs = configurations.runtimeClasspath.get()
        .filter { it.name.contains("javafx") }
        .joinToString(";") { it.absolutePath }

    jvmArgs = listOf(
        "--module-path", javaFxLibs,
        "--add-modules", "javafx.controls,javafx.graphics,javafx.base"
    )
}