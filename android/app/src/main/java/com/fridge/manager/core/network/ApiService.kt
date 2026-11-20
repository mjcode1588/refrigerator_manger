package com.fridge.manager.core.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("auth/social/google")
    suspend fun loginWithGoogle(@Body body: SocialGoogleRequest): TokenResponseDto

    @POST("auth/social/kakao")
    suspend fun loginWithKakao(@Body body: SocialKakaoRequest): TokenResponseDto

    @POST("auth/dev-login")
    suspend fun devLogin(@Body body: DevLoginRequest): TokenResponseDto

    @POST("auth/refresh")
    suspend fun refreshTokens(@Body body: RefreshRequest): Response<TokenResponseDto>

    @GET("users/me")
    suspend fun getMe(): UserMeDto

    @POST("households")
    suspend fun createHousehold(@Body body: CreateHouseholdRequest): HouseholdDto

    @POST("households/join")
    suspend fun joinHousehold(@Body body: JoinHouseholdRequest): HouseholdDto

    @GET("households/{householdId}/members")
    suspend fun getMembers(@Path("householdId") householdId: Long): List<HouseholdMemberDto>

    @POST("households/{householdId}/invites")
    suspend fun createInvite(
        @Path("householdId") householdId: Long,
        @Body body: CreateInviteRequest = CreateInviteRequest(),
    ): InviteCodeDto

    @GET("households/{householdId}/locations")
    suspend fun getLocations(
        @Path("householdId") householdId: Long,
        @Query("format") format: String = "flat",
    ): List<LocationNodeDto>

    @POST("households/{householdId}/locations")
    suspend fun createLocation(
        @Path("householdId") householdId: Long,
        @Body body: CreateLocationRequest,
    ): LocationNodeDto

    @DELETE("households/{householdId}/locations/{locationId}")
    suspend fun deleteLocation(
        @Path("householdId") householdId: Long,
        @Path("locationId") locationId: Long,
        @Query("strategy") strategy: String = "cascade",
    )

    @GET("households/{householdId}/items")
    suspend fun getItems(
        @Path("householdId") householdId: Long,
        @Query("location_id") locationId: Long? = null,
        @Query("include_sublocations") includeSublocations: Boolean = true,
        @Query("status") status: String = "ACTIVE",
        @Query("q") query: String? = null,
        @Query("sort") sort: String = "expiry",
    ): List<ItemDto>

    @GET("households/{householdId}/items/expiring")
    suspend fun getExpiringItems(
        @Path("householdId") householdId: Long,
        @Query("within_days") withinDays: Int = 7,
    ): List<ItemDto>

    @POST("households/{householdId}/items")
    suspend fun createItem(
        @Path("householdId") householdId: Long,
        @Body body: CreateItemRequest,
    ): ItemDto

    @PATCH("households/{householdId}/items/{itemId}")
    suspend fun updateItem(
        @Path("householdId") householdId: Long,
        @Path("itemId") itemId: Long,
        @Body body: UpdateItemRequest,
    ): ItemDto

    @POST("households/{householdId}/items/{itemId}/consume")
    suspend fun consumeItem(
        @Path("householdId") householdId: Long,
        @Path("itemId") itemId: Long,
        @Body body: ConsumeItemRequest? = null,
    ): ItemDto

    @POST("households/{householdId}/items/{itemId}/discard")
    suspend fun discardItem(
        @Path("householdId") householdId: Long,
        @Path("itemId") itemId: Long,
    ): ItemDto

    @GET("households/{householdId}/activities")
    suspend fun getActivities(
        @Path("householdId") householdId: Long,
        @Query("limit") limit: Int = 20,
    ): ActivityListDto
}
