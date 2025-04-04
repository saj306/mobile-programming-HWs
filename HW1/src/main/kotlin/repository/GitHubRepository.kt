package project.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import project.api.GitHubApiService
import project.model.GitHubUser
import project.model.Repository
import project.model.Result
import java.io.File
import java.io.IOException

class GitHubRepository(
    private val apiService: GitHubApiService,
    private val usePersistentStorage: Boolean = false
) {
    private val userCache = mutableMapOf<String, GitHubUser>()
    private val repositoriesCache = mutableMapOf<String, List<Repository>>()
    private val gson = Gson()

    private val usersFile = File("github_users.json")
    private val reposFile = File("github_repos.json")

    init {
        if (usePersistentStorage) {
            loadCachedData()
        }
    }

    private fun loadCachedData() {
        try {
            if (usersFile.exists()) {
                val usersJson = usersFile.readText()
                val userMapType = object : TypeToken<Map<String, GitHubUser>>() {}.type
                val loadedUsers: Map<String, GitHubUser> = gson.fromJson(usersJson, userMapType)
                userCache.putAll(loadedUsers)
            }

            if (reposFile.exists()) {
                val reposJson = reposFile.readText()
                val repoMapType = object : TypeToken<Map<String, List<Repository>>>() {}.type
                val loadedRepos: Map<String, List<Repository>> = gson.fromJson(reposJson, repoMapType)
                repositoriesCache.putAll(loadedRepos)
            }
        } catch (e: IOException) {
            println("Error loading cached data: ${e.message}")
        }
    }

    private fun saveCachedData() {
        if (!usePersistentStorage) return

        try {
            val usersJson = gson.toJson(userCache)
            usersFile.writeText(usersJson)

            val reposJson = gson.toJson(repositoriesCache)
            reposFile.writeText(reposJson)
        } catch (e: IOException) {
            println("Error saving cached data: ${e.message}")
        }
    }

    suspend fun getUserInfo(username: String): Result<GitHubUser> = withContext(Dispatchers.IO) {
        userCache[username]?.let {
            return@withContext Result.Success(it)
        }

        try {
            val response = apiService.getUser(username)
            if (response.isSuccessful) {
                response.body()?.let { user ->
                    userCache[username] = user
                    saveCachedData()
                    Result.Success(user)
                } ?: Result.Error("User data is null")
            } else {
                Result.Error("Failed to fetch user: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun getUserRepositories(username: String): Result<List<Repository>> = withContext(Dispatchers.IO) {
        repositoriesCache[username]?.let {
            return@withContext Result.Success(it)
        }

        try {
            val response = apiService.getRepositories(username)
            if (response.isSuccessful) {
                response.body()?.let { repos ->
                    repositoriesCache[username] = repos
                    saveCachedData()
                    Result.Success(repos)
                } ?: Result.Error("Repository data is null")
            } else {
                Result.Error("Failed to fetch repositories: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    fun getCachedUsers(): List<GitHubUser> = userCache.values.toList()

    fun searchUsersByName(query: String): List<GitHubUser> {
        return userCache.values.filter {
            it.login.contains(query, ignoreCase = true) ||
                    (it.name?.contains(query, ignoreCase = true) ?: false)
        }
    }

    fun searchRepositories(query: String): Pair<List<GitHubUser>, List<Repository>> {
        val matchingRepos = mutableListOf<Repository>()
        val usersWithMatchingRepos = mutableListOf<GitHubUser>()

        repositoriesCache.forEach { (username, repos) ->
            val filteredRepos = repos.filter { it.name.contains(query, ignoreCase = true) }
            if (filteredRepos.isNotEmpty()) {
                matchingRepos.addAll(filteredRepos)
                userCache[username]?.let { usersWithMatchingRepos.add(it) }
            }
        }

        return Pair(usersWithMatchingRepos, matchingRepos)
    }
}