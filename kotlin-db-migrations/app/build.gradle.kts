plugins {
  kotlin("jvm") version "1.8.21"
  kotlin("plugin.serialization") version "1.8.21"
  application
}

repositories {
  // Use Maven Central for resolving dependencies.
  mavenCentral()
}

dependencies {
  // For managing our database migrations
  // https://github.com/flyway/flyway
  implementation("org.flywaydb:flyway-core:9.17.0")

  // For parsing CLI arguments
  // https://github.com/Kotlin/kotlinx-cli
  implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")

  // For couroutines support; not strictly needed, but it's nice to
  // indicate when blocking I/O needs the thread-pool meant for blocking stuff.
  // https://github.com/Kotlin/kotlinx.coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-RC")

  // For parsing our configuration file. Using:
  //  - https://github.com/Kotlin/kotlinx.serialization
  //  - https://github.com/lightbend/config (HOCON as the format)
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-hocon:1.5.0")

  // Database driver (JDBC)
  implementation("org.postgresql:postgresql:42.6.0")

  // Flyway has built-in logging, which we can expose via SLF4J/Logback
  implementation("ch.qos.logback:logback-classic:1.4.7")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}

tasks.register<JavaExec>("migrate") {
  group = "Execution"
  description = "Migrates the database to the latest version"
  classpath = sourceSets.getByName("main").runtimeClasspath
  mainClass.set("migrations.sample.RunMigrations")

  val user = System.getenv("POSTGRES_ADMIN_USER")
    ?: "postgres"
  val pass = System.getenv("POSTGRES_ADMIN_PASSWORD")
    ?: throw GradleException(
      "POSTGRES_ADMIN_PASSWORD environment variable must be set"
    )
  args = listOf(user, pass)
}
