import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif


struct CreateGameResponse: Codable {
    let game_id: String
}

struct GuessRequest: Codable {
    let game_id: String
    let guess: String
}

struct GuessResponse: Codable {
    let black: Int
    let white: Int
}

struct ErrorResponse: Codable {
    let error: String
}

class MastermindAPIClient {
    private let baseURL = "https://mastermind.darkube.app"
    
    func createGame() async throws -> String {
        guard let url = URL(string: "\(baseURL)/game") else {
            throw NetworkError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw NetworkError.invalidResponse
        }
        
        if httpResponse.statusCode == 200 {
            let gameResponse = try JSONDecoder().decode(CreateGameResponse.self, from: data)
            return gameResponse.game_id
        } else {
            let errorResponse = try JSONDecoder().decode(ErrorResponse.self, from: data)
            throw NetworkError.serverError(errorResponse.error)
        }
    }
    
    func makeGuess(gameID: String, guess: String) async throws -> GuessResponse {
        guard let url = URL(string: "\(baseURL)/guess") else {
            throw NetworkError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let guessRequest = GuessRequest(game_id: gameID, guess: guess)
        let requestData = try JSONEncoder().encode(guessRequest)
        request.httpBody = requestData
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw NetworkError.invalidResponse
        }
        
        if httpResponse.statusCode == 200 {
            return try JSONDecoder().decode(GuessResponse.self, from: data)
        } else {
            let errorResponse = try JSONDecoder().decode(ErrorResponse.self, from: data)
            throw NetworkError.serverError(errorResponse.error)
        }
    }
    
    func deleteGame(gameID: String) async throws {
        guard let url = URL(string: "\(baseURL)/game/\(gameID)") else {
            throw NetworkError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"
        
        let (_, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw NetworkError.invalidResponse
        }
        
        if httpResponse.statusCode != 204 {
            throw NetworkError.serverError("Failed to delete game")
        }
    }
}

enum NetworkError: Error, LocalizedError {
    case invalidURL
    case invalidResponse
    case serverError(String)
    
    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid URL"
        case .invalidResponse:
            return "Invalid response from server"
        case .serverError(let message):
            return "Server error: \(message)"
        }
    }
}

enum GameError: Error, LocalizedError {
    case invalidGuess
    
    var errorDescription: String? {
        switch self {
        case .invalidGuess:
            return "Invalid guess. Please enter a 4-digit code with numbers between 1 and 6."
        }
    }
}

class MastermindGame {
    private let apiClient = MastermindAPIClient()
    private var gameID: String?
    private var guessCount = 0
    
    func start() async {
        print("🎯 Welcome to Mastermind!")
        print("=============================")
        print("Rules:")
        print("- Guess a 4-digit code")
        print("- Each digit must be between 1 and 6")
        print("- B (Black): Correct digit in correct position")
        print("- W (White): Correct digit in wrong position")
        print("- Type 'exit' to quit at any time")
        print("=============================")
        
        do {
            gameID = try await apiClient.createGame()
            print("✅ New game started! Game ID: \(gameID!)")
            await playGame()
        } catch {
            print("❌ Error starting game: \(error.localizedDescription)")
        }
    }
    
    private func playGame() async {
        while true {
            print("\n🔢 Enter your 4-digit guess (or 'exit' to quit):")
            
            guard let input = readLine()?.trimmingCharacters(in: .whitespacesAndNewlines) else {
                continue
            }
            
            if input.lowercased() == "exit" {
                await exitGame()
                return
            }
            
            do {
                try validateGuess(input)
                let response = try await apiClient.makeGuess(gameID: gameID!, guess: input)
                guessCount += 1
                
                await handleGuessResponse(guess: input, response: response)
                
                if response.black == 4 {
                    print("🎉 Congratulations! You cracked the code in \(guessCount) attempts!")
                    await exitGame()
                    return
                }
                
            } catch GameError.invalidGuess {
                print("❌ \(GameError.invalidGuess.localizedDescription)")
            } catch {
                print("❌ Error making guess: \(error.localizedDescription)")
            }
        }
    }
    
    private func validateGuess(_ guess: String) throws {
        guard guess.count == 4,
              guess.allSatisfy({ $0.isNumber && $0 >= "1" && $0 <= "6" }) else {
            throw GameError.invalidGuess
        }
    }
    
    private func handleGuessResponse(guess: String, response: GuessResponse) async {
        let blackPegs = String(repeating: "B", count: response.black)
        let whitePegs = String(repeating: "W", count: response.white)
        let feedback = blackPegs + whitePegs
        
        print("📊 Guess \(guessCount): \(guess) → \(feedback.isEmpty ? "No matches" : feedback)")
        
        if response.black > 0 {
            print("   ⚫ \(response.black) correct digit(s) in correct position")
        }
        if response.white > 0 {
            print("   ⚪ \(response.white) correct digit(s) in wrong position")
        }
        if response.black == 0 && response.white == 0 {
            print("   ❌ No correct digits")
        }
    }
    
    private func exitGame() async {
        if let gameID = gameID {
            do {
                try await apiClient.deleteGame(gameID: gameID)
                print("🧹 Game cleaned up successfully")
            } catch {
                print("⚠️  Warning: Could not clean up game: \(error.localizedDescription)")
            }
        }
        print("👋 Thanks for playing Mastermind!")
        exit(0)
    }
}

@main
struct MastermindApp {
    static func main() async {
        let game = MastermindGame()
        await game.start()
    }
}
