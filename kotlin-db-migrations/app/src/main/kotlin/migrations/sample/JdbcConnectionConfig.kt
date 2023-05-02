package migrations.sample

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.hocon.Hocon

@Serializable
data class JdbcConnectionConfig(
    val url: String,
    val driver: String,
    val username: String,
    val password: String,
    val migrationsTable: String,
    val migrationsLocations: List<String>,
    val migrationsPlaceholders: Map<String, String> = emptyMap()
) {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        suspend fun loadFromGlobal(
            configNamespace: String,
            config: Config? = null
        ): JdbcConnectionConfig =
            withContext(Dispatchers.IO) {
                val rawCfg = config ?: ConfigFactory.load().resolve()
                val cfg = rawCfg.getConfig(configNamespace)
                Hocon.decodeFromConfig(serializer(), cfg)
            }
    }
}
