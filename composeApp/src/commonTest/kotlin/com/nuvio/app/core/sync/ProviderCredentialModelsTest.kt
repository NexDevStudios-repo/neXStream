package com.nuvio.app.core.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ProviderCredentialModelsTest {
    @Test
    fun `TMDB credential snapshot contains the personal override`() {
        val credential = ProviderCredentialValue(ProviderCredentialIds.TMDB, PROVIDER_API_KEY_FIELD, "personal-key")
        assertEquals(buildJsonObject { put("api_key", "personal-key") }, credential.credentialJson())
    }

    @Test
    fun `empty TMDB override syncs a clear tombstone`() {
        val credential = ProviderCredentialValue(ProviderCredentialIds.TMDB, PROVIDER_API_KEY_FIELD, "")
        assertEquals(buildJsonObject { put("api_key", "") }, credential.credentialJson())
    }

    @Test
    fun `remote TMDB override can be replaced and cleared`() {
        val local = ProviderCredentialSnapshot(
            profileId = 1,
            values = listOf(ProviderCredentialValue(ProviderCredentialIds.TMDB, PROVIDER_API_KEY_FIELD, "local-key")),
        )
        val remote = listOf(
            SupabaseProviderCredential("tmdb", buildJsonObject { put("api_key", "remote-key") }),
        )
        val merged = local.mergeRemote(remote)

        assertEquals("remote-key", merged.values.single { it.provider == ProviderCredentialIds.TMDB }.value)
        assertEquals("", merged.mergeRemote(emptyList()).values.single { it.provider == ProviderCredentialIds.TMDB }.value)
    }

    @Test
    fun `complete remote snapshot replaces cached credentials`() {
        val snapshot = ProviderCredentialSnapshot(
            profileId = 1,
            values = listOf(
                ProviderCredentialValue("tmdb", "api_key", "local-tmdb"),
                ProviderCredentialValue("animeskip", "client_id", "local-anime"),
            ),
        )
        val rows = listOf(
            SupabaseProviderCredential("TMDB", buildJsonObject { put("api_key", "remote") }),
            SupabaseProviderCredential("animeskip", buildJsonObject { put("client_id", "remote") }),
        )

        assertEquals(listOf("remote", "remote"), snapshot.mergeRemote(rows).values.map { it.value })
    }

    @Test
    fun `missing remote provider clears its cached credential`() {
        val snapshot = ProviderCredentialSnapshot(
            profileId = 1,
            values = listOf(
                ProviderCredentialValue("tmdb", "api_key", "local-tmdb"),
                ProviderCredentialValue("animeskip", "client_id", "local-anime"),
            ),
        )
        val rows = listOf(
            SupabaseProviderCredential("tmdb", buildJsonObject { put("api_key", "remote") }),
        )

        assertEquals(listOf("remote", ""), snapshot.mergeRemote(rows).values.map { it.value })
    }

    @Test
    fun `remote values replace only supported local providers`() {
        val local = ProviderCredentialSnapshot(
            profileId = 2,
            values = listOf(
                ProviderCredentialValue("tmdb", "api_key", "local-tmdb"),
                ProviderCredentialValue("animeskip", "client_id", "local-anime"),
            ),
        )
        val remote = listOf(
            SupabaseProviderCredential(
                provider = "tmdb",
                credentialJson = buildJsonObject { put("api_key", "remote-tmdb") },
            ),
            SupabaseProviderCredential(
                provider = "unsupported",
                credentialJson = buildJsonObject { put("api_key", "ignored") },
            ),
        )

        val merged = local.mergeRemote(remote)

        assertEquals("remote-tmdb", merged.values[0].value)
        assertEquals("", merged.values[1].value)
    }

    @Test
    fun `empty remote snapshot clears all cached credentials`() {
        val local = ProviderCredentialSnapshot(
            profileId = 1,
            values = listOf(
                ProviderCredentialValue("tmdb", "api_key", "local-tmdb"),
                ProviderCredentialValue("animeskip", "client_id", "local-anime"),
            ),
        )

        assertEquals(listOf("", ""), local.mergeRemote(emptyList()).values.map { it.value })
    }

    @Test
    fun `blank remote value is retained as a clear tombstone`() {
        val local = ProviderCredentialSnapshot(
            profileId = 1,
            values = listOf(ProviderCredentialValue("mdblist", "api_key", "local")),
        )
        val remote = listOf(
            SupabaseProviderCredential(
                provider = "mdblist",
                credentialJson = buildJsonObject { put("api_key", "") },
            ),
        )

        assertEquals("", local.mergeRemote(remote).values.single().value)
    }
}

