package migrations.sample

import com.typesafe.config.ConfigFactory
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.api.output.MigrateResult
import org.slf4j.LoggerFactory

/**
 * Given a JDBC configuration, run the associated DB migrations.
 *
 * NOTE: `adminUsername` and `adminPassword` are different from the credentials
 * specified in `JdbcConnectionConfig`. That's because the "admin" user may be
 * different from the app's user. So if an `adminUsername` and an `adminPassword`
 * are provided, Flyway will use that admin user to execute migrations.
 *
 * Flyway uses "placeholders" that can be used in the SQL migrations.
 * These can be specified in `JdbcConnectionConfig`, but this code also sets
 * 2 special placeholders to use from the `JdbcConnectionConfig` itself:
 * `dbUsername` and `dbPassword`. These can be used to create the app's user
 * as part of the defined migrations.
 */
suspend fun dbMigrate(
    config: JdbcConnectionConfig,
    adminUsername: String?,
    adminPassword: String?
): MigrateResult =
    withContext(Dispatchers.IO) {
        val m: FluentConfiguration = Flyway.configure()
            .dataSource(
                config.url,
                adminUsername ?: config.username,
                if (adminUsername != null) adminPassword else config.password,
            )
            .group(true)
            .outOfOrder(false)
            .table(config.migrationsTable)
            .locations(*config.migrationsLocations.toTypedArray())
            .baselineOnMigrate(true)
            .loggers("slf4j")
            .placeholders(
                config.migrationsPlaceholders + mapOf(
                    "dbUsername" to config.username,
                    "dbPassword" to config.password
                )
            )

        val validated = m
            .ignoreMigrationPatterns("*:pending")
            .load()
            .validateWithResult()

        if (!validated.validationSuccessful) {
            val logger = LoggerFactory.getLogger("RunMigrations")
            for (error in validated.invalidMigrations) {
                logger.warn(
                    """
                        |Failed to validate migration:
                        |  - version: ${error.version}
                        |  - path: ${error.filepath}
                        |  - description: ${error.description}
                        |  - error code: ${error.errorDetails.errorCode}
                        |  - error message: ${error.errorDetails.errorMessage}
                    """.trimMargin("|").trim()
                )
            }
        }
        m.load().migrate()
    }

object RunMigrations {
    private suspend fun migrateNamespace(
        label: String,
        config: JdbcConnectionConfig,
        adminUsername: String,
        adminPassword: String
    ): Unit = withContext(Dispatchers.IO) {
        val result = dbMigrate(
            config,
            adminUsername,
            adminPassword
        )
        println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")
        println("Migrating: $label")
        println("------------------------------------")
        println("Initial schema version: ${result.initialSchemaVersion}")
        println("Target schema version: ${result.targetSchemaVersion}")
        if (!result.migrations.isEmpty()) {
            println("------------------------------------")
            println("Executed migrations:")
            for (migration in result.migrations) {
                println(" - ${migration.version} ${migration.type} ${migration.description}")
            }
        }
        if (!result.warnings.isEmpty()) {
            println("------------------------------------")
            System.err.println("WARNINGS:")
            for (warning in result.warnings) {
                System.err.println(" - $warning")
            }
        }
        println("------------------------------------")
        if (result.success) {
            println("Successfully migrated: $label!")
        } else {
            System.err.println("ERROR: Failed to migrate $label!")
            System.exit(1)
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val parser = ArgParser("RunMigrations")
        val adminUsername by parser.argument(
            ArgType.String,
            fullName = "admin-username",
            description = "Admin username for the database. Example: postgres"
        )
        val adminPassword by parser.argument(
            ArgType.String,
            fullName = "admin-password",
            description = "Admin password for the database."
        )
        parser.parse(args)

        runBlocking {
            val config =
                ConfigFactory.load("database.conf").resolve()
            val mainConfig =
                JdbcConnectionConfig.loadFromGlobal(
                    "jdbc-connection.main",
                    config
                )
            migrateNamespace(
                "main",
                mainConfig,
                adminUsername,
                adminPassword
            )
        }
    }
}
