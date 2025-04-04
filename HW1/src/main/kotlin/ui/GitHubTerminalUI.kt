package project.ui

import kotlinx.coroutines.runBlocking
import project.api.GitHubApiService
import project.model.GitHubUser
import project.model.Result
import project.repository.GitHubRepository
import java.text.SimpleDateFormat
import java.util.*

class GitHubTerminalUI {
    private val scanner = Scanner(System.`in`)
    private lateinit var repository: GitHubRepository

    fun start() {
        println("Do you want to use persistent storage for GitHub data? (y/n): ")
        val usePersistentStorage = scanner.nextLine().trim().equals("y", ignoreCase = true)

        repository = GitHubRepository(GitHubApiService.create(), usePersistentStorage)

        if (usePersistentStorage) {
            println("Using persistent file storage for GitHub data.")
        } else {
            println("Using in-memory storage only.")
        }

        var running = true

        while (running) {
            printMenu()

            when (readMenuChoice()) {
                1 -> retrieveUserInfo()
                2 -> displayCachedUsers()
                3 -> searchByUsername()
                4 -> searchByRepositoryName()
                5 -> {
                    println("Exiting program...")
                    running = false
                }

                else -> println("Invalid choice. Please try again.")
            }

            if (running) {
                println("\nPress Enter to continue...")
                scanner.nextLine()
            }
        }
    }

    private fun printMenu() {
        println("\n===== GitHub User Information Retrieval System =====")
        println("1️⃣ Retrieve user information by username")
        println("2️⃣ Display the list of users in memory")
        println("3️⃣ Search by username among users in memory")
        println("4️⃣ Search by repository name among data in memory")
        println("5️⃣ Exit the program")
        print("Enter your choice (1-5): ")
    }

    private fun readMenuChoice(): Int {
        return try {
            val input = scanner.nextLine().trim()
            input.toInt()
        } catch (e: Exception) {
            -1
        }
    }

    private fun retrieveUserInfo() {
        print("Enter GitHub username: ")
        val username = scanner.nextLine().trim()

        if (username.isEmpty()) {
            println("Username cannot be empty")
            return
        }

        runBlocking {
            when (val userResult = repository.getUserInfo(username)) {
                is Result.Success -> {
                    val user = userResult.data
                    printUserInfo(user)

                    // Fetch and display repositories
                    when (val reposResult = repository.getUserRepositories(username)) {
                        is Result.Success -> {
                            val repos = reposResult.data
                            println("\nRepositories (${repos.size}):")
                            repos.forEach { repo ->
                                println("  - ${repo.name}: ${repo.description ?: "No description"}")
                            }
                        }

                        is Result.Error -> println("Failed to fetch repositories: ${reposResult.message}")
                    }
                }

                is Result.Error -> println("Error: ${userResult.message}")
            }
        }
    }

    private fun displayCachedUsers() {
        val users = repository.getCachedUsers()

        if (users.isEmpty()) {
            println("No users in memory. Retrieve some users first.")
            return
        }

        println("\n=== Users in Memory (${users.size}) ===")
        users.forEach { user ->
            println("${user.login} (${user.name ?: "No name"}) - ${user.publicRepos} repositories")
        }
    }

    private fun searchByUsername() {
        print("Enter search query: ")
        val query = scanner.nextLine().trim()

        if (query.isEmpty()) {
            println("Search query cannot be empty")
            return
        }

        val foundUsers = repository.searchUsersByName(query)

        if (foundUsers.isEmpty()) {
            println("No users found matching '$query'")
            return
        }

        println("\n=== Found Users (${foundUsers.size}) ===")
        foundUsers.forEach { user ->
            println("${user.login} (${user.name ?: "No name"})")
        }
    }

    private fun searchByRepositoryName() {
        print("Enter repository name to search: ")
        val query = scanner.nextLine().trim()

        if (query.isEmpty()) {
            println("Search query cannot be empty")
            return
        }

        val (users, repos) = repository.searchRepositories(query)

        if (repos.isEmpty()) {
            println("No repositories found matching '$query'")
            return
        }

        println("\n=== Found Repositories (${repos.size}) ===")
        repos.forEach { repo ->
            println("${repo.fullName}: ${repo.description ?: "No description"}")
        }
    }

    private fun printUserInfo(user: GitHubUser) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        println("\n=== User Information ===")
        println("Username: ${user.login}")
        println("Name: ${user.name ?: "Not provided"}")
        println("Bio: ${user.bio ?: "Not provided"}")
        println("Followers: ${user.followersCount}")
        println("Following: ${user.followingCount}")
        println("Public Repositories: ${user.publicRepos}")
        println("Account Created: ${dateFormat.format(user.createdAt)}")
    }
}