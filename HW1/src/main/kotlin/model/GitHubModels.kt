package project.model

import com.google.gson.annotations.SerializedName
import java.util.*

data class GitHubUser(
    val id: Long,
    val login: String,
    val name: String?,
    @SerializedName("avatar_url") val avatarUrl: String,
    @SerializedName("followers") val followersCount: Int,
    @SerializedName("following") val followingCount: Int,
    @SerializedName("created_at") val createdAt: Date,
    @SerializedName("public_repos") val publicRepos: Int,
    val bio: String?
)

data class Repository(
    val id: Long,
    val name: String,
    @SerializedName("full_name") val fullName: String,
    val description: String?,
    @SerializedName("html_url") val htmlUrl: String,
    val language: String?,
    @SerializedName("stargazers_count") val stars: Int,
    val fork: Boolean
)