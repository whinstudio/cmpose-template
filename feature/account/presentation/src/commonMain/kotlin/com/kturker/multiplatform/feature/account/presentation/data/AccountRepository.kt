package com.kturker.multiplatform.feature.account.presentation.data

import com.kturker.multiplatform.core.api.ApiRoutes
import com.kturker.multiplatform.core.api.Profile
import com.kturker.multiplatform.core.api.Role
import com.kturker.multiplatform.core.api.Space
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.core.network.BaseRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** The backend calls the account screens make. */
interface AccountRepository {
    /** GET spaces: every space the user is a member of, their own personal space included. */
    suspend fun spaces(): RestResult<List<Space>>

    /**
     * PUT me/profile: the display name and the (localized) name of the personal space. The backend replaces the
     * display name each time (null clears it), so the caller sends both current values, never only a changed one.
     */
    suspend fun updateProfile(profile: Profile): RestResult<Unit>

    /** DELETE me: the server deletes the user, the spaces they own with their content, and their memberships. */
    suspend fun deleteServerAccount(): RestResult<Unit>
}

/**
 * The spaces that go with the account and that someone else uses too: owned and with other members, the personal
 * space included once it is shared. Deleting the account deletes them for those members as well.
 */
fun List<Space>.sharedOwnedCount(): Int = count { it.role == Role.OWNER && it.memberCount > 1 }

/** [AccountRepository] over the backend client (base URL, bearer token, JSON; a 401 refreshes the token once). */
internal class KtorAccountRepository(private val client: HttpClient) :
    BaseRepository(),
    AccountRepository {

    override suspend fun spaces(): RestResult<List<Space>> =
        request { client.get(ApiRoutes.SPACES) }

    override suspend fun updateProfile(profile: Profile): RestResult<Unit> = request {
        client.put(ApiRoutes.PROFILE) {
            contentType(ContentType.Application.Json)
            setBody(profile)
        }
    }

    override suspend fun deleteServerAccount(): RestResult<Unit> =
        request { client.delete(ApiRoutes.ME) }
}
