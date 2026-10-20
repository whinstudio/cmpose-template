package com.kturker.multiplatform.feature.sharing.presentation.data

import com.kturker.multiplatform.core.api.ApiRoutes
import com.kturker.multiplatform.core.api.InviteLink
import com.kturker.multiplatform.core.api.InvitePreview
import com.kturker.multiplatform.core.api.InviteRequest
import com.kturker.multiplatform.core.api.InviteToken
import com.kturker.multiplatform.core.api.Me
import com.kturker.multiplatform.core.api.Member
import com.kturker.multiplatform.core.api.Role
import com.kturker.multiplatform.core.api.RoleChange
import com.kturker.multiplatform.core.api.Space
import com.kturker.multiplatform.core.api.SpaceDraft
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.core.network.BaseRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * The backend's spaces, members and invite calls. A space the user is not a member of, or whose role is too low for
 * the call, answers 404, like one that does not exist; accepting a used invite answers 409, an expired one 410.
 */
interface SpacesRepository {
    /** GET spaces: every space the user is a member of, their personal space included. */
    suspend fun list(): RestResult<List<Space>>
    suspend fun create(name: String): RestResult<Space>
    suspend fun rename(
        id: String,
        name: String
    ): RestResult<Space>
    suspend fun delete(id: String): RestResult<Unit>
    suspend fun members(id: String): RestResult<List<Member>>
    suspend fun setRole(
        spaceId: String,
        userId: String,
        role: Role
    ): RestResult<Unit>

    /** An owner removes [userId] (the backend's user id, [Member.userId]) from the space; to leave, see [leave]. */
    suspend fun remove(
        spaceId: String,
        userId: String
    ): RestResult<Unit>

    /** POST spaces/{id}/leave: the signed-in user leaves the space; its last owner cannot (409). */
    suspend fun leave(spaceId: String): RestResult<Unit>
    suspend fun invite(
        id: String,
        role: Role
    ): RestResult<InviteLink>

    /** POST invites/preview with the token in the body ([InviteToken]), never in the path. */
    suspend fun preview(token: String): RestResult<InvitePreview>

    /** Joins the invite's space; an existing member gets the space back and the invite stays unused. */
    suspend fun accept(token: String): RestResult<Space>

    /** GET me: the signed-in user as the backend knows them; its [Me.id] is what [Member.userId] holds. */
    suspend fun me(): RestResult<Me>
}

/** [SpacesRepository] over the backend client (base URL, bearer token, JSON; a 401 refreshes the token once). */
internal class KtorSpacesRepository(private val client: HttpClient) :
    BaseRepository(),
    SpacesRepository {

    override suspend fun list(): RestResult<List<Space>> = request { client.get(ApiRoutes.SPACES) }

    override suspend fun create(name: String): RestResult<Space> =
        request { client.post(ApiRoutes.SPACES) { json(SpaceDraft(name)) } }

    override suspend fun rename(
        id: String,
        name: String
    ): RestResult<Space> = request { client.patch(ApiRoutes.space(id)) { json(SpaceDraft(name)) } }

    override suspend fun delete(id: String): RestResult<Unit> =
        request { client.delete(ApiRoutes.space(id)) }

    override suspend fun members(id: String): RestResult<List<Member>> =
        request { client.get(ApiRoutes.members(id)) }

    override suspend fun setRole(
        spaceId: String,
        userId: String,
        role: Role
    ): RestResult<Unit> =
        request { client.patch(ApiRoutes.member(spaceId, userId)) { json(RoleChange(role)) } }

    override suspend fun remove(
        spaceId: String,
        userId: String
    ): RestResult<Unit> = request { client.delete(ApiRoutes.member(spaceId, userId)) }

    override suspend fun leave(spaceId: String): RestResult<Unit> =
        request { client.post(ApiRoutes.leave(spaceId)) }

    override suspend fun invite(
        id: String,
        role: Role
    ): RestResult<InviteLink> =
        request { client.post(ApiRoutes.invites(id)) { json(InviteRequest(role)) } }

    override suspend fun preview(token: String): RestResult<InvitePreview> =
        request { client.post(ApiRoutes.INVITE_PREVIEW) { json(InviteToken(token)) } }

    override suspend fun accept(token: String): RestResult<Space> =
        request { client.post(ApiRoutes.INVITE_ACCEPT) { json(InviteToken(token)) } }

    override suspend fun me(): RestResult<Me> = request { client.get(ApiRoutes.ME) }

    private inline fun <reified T : Any> HttpRequestBuilder.json(body: T) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }
}
