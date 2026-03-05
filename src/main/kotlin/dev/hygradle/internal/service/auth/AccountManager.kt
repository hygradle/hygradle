package dev.hygradle.internal.service.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.ServiceReference
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

abstract class AccountManager : BuildService<BuildServiceParameters.None> {
  @get:ServiceReference abstract val oauthManager: Property<OAuthManager>

  protected val service =
      Retrofit.Builder()
          .baseUrl(AccountService.BASE_URL)
          .addConverterFactory(
              Json.asConverterFactory("application/json; charset=utf-8".toMediaType())
          )
          .build()
          .create<AccountService>()

  interface AccountService {
    @GET("my-account/get-profiles")
    suspend fun getProfiles(@Header("Authorization") authorization: String): GetProfilesResponse

    @GET("game-assets/builds/{patchline}/{version}.zip")
    suspend fun getAssetBundle(
        @Path("patchline") patchline: String,
        @Path("version") version: String,
        @Header("Authorization") authorization: String,
    ): AssetBundleUrlResponse

    companion object {
      const val BASE_URL = "https://account-data.hytale.com"
    }

    @Serializable data class AssetBundleUrlResponse(val url: String)

    @Serializable data class GetProfilesResponse(val profiles: List<Profile>)

    @Serializable data class Profile(val uuid: String, val username: String)
  }
}
