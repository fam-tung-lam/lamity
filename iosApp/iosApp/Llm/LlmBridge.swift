import Foundation
import Shared
import LiteRTLM

/// LiteRT-LM Swift runtime behind the shared `NativeLlmBridge` contract.
/// Shared Kotlin code drives it through callbacks; tool calls made by the
/// model are routed back into shared code via `MainViewControllerKt.executeBuiltinTool`.
final class SwiftLlmBridge: NSObject, NativeLlmBridge {

    static let shared = SwiftLlmBridge()

    /// Set by shared code (ChatSessionManager via Koin); tools call back into shared code instead.
    var toolExecutor: (any Shared.ToolExecutor)?

    private var engine: Engine?
    // Qualified: the Shared (Kotlin) framework also exports a `Conversation` type.
    private var conversations: [String: LiteRTLM.Conversation] = [:]
    private var generations: [String: Task<Void, Never>] = [:]

    // MARK: - Engine

    func initializeEngine(setup: EngineSetup, callback: any EngineCallback) {
        Task {
            do {
                self.teardownEngine()
                let config = try EngineConfig(
                    modelPath: setup.modelPath,
                    backend: setup.backend == "gpu" ? .gpu : .cpu(),
                    maxNumTokens: Int(setup.maxTokens),
                    cacheDir: setup.cacheDir
                )
                let engine = Engine(engineConfig: config)
                try await engine.initialize()
                self.engine = engine
                callback.onEngineReady()
            } catch {
                callback.onEngineError(message: describe(error))
            }
        }
    }

    func closeEngine() {
        teardownEngine()
    }

    private func teardownEngine() {
        generations.values.forEach { $0.cancel() }
        generations.removeAll()
        conversations.removeAll()
        engine = nil
    }

    // MARK: - Conversations

    func createConversation(setup: ConversationSetup, callback: any ConversationCallback) {
        Task {
            do {
                guard let engine = self.engine else {
                    callback.onConversationError(message: "Engine is not initialized")
                    return
                }
                let sampler = try SamplerConfig(
                    topK: Int(setup.topK),
                    topP: Float(setup.topP),
                    temperature: Float(setup.temperature)
                )
                let systemMessage = (setup.systemPrompt?.isEmpty == false)
                    ? Message(setup.systemPrompt!, role: .system)
                    : nil
                let config = ConversationConfig(
                    systemMessage: systemMessage,
                    initialMessages: self.initialMessages(fromHistoryJson: setup.historyJson),
                    tools: self.tools(for: setup.toolIds),
                    samplerConfig: sampler
                )
                let conversation = try await engine.createConversation(with: config)
                let handle = UUID().uuidString
                self.conversations[handle] = conversation
                callback.onConversationReady(handle: handle)
            } catch {
                callback.onConversationError(message: describe(error))
            }
        }
    }

    func closeConversation(handle: String) {
        generations[handle]?.cancel()
        generations[handle] = nil
        if let conversation = conversations.removeValue(forKey: handle) {
            try? conversation.cancel()
        }
    }

    // MARK: - Generation

    func sendMessage(handle: String, text: String, listener: any GenerationListener) {
        guard let conversation = conversations[handle] else {
            listener.onGenerationError(message: "Conversation is not active")
            return
        }
        let task = Task {
            do {
                for try await chunk in conversation.sendMessageStream(Message(text)) {
                    if Task.isCancelled { break }
                    let piece = chunk.toString
                    if !piece.isEmpty {
                        listener.onChunk(text: piece)
                    }
                    if let thought = chunk.channels["thought"], !thought.isEmpty {
                        listener.onThought(text: thought)
                    }
                }
                listener.onGenerationDone()
            } catch {
                if Task.isCancelled || error is CancellationError {
                    listener.onGenerationDone()
                } else {
                    listener.onGenerationError(message: self.describe(error))
                }
            }
            self.generations[handle] = nil
        }
        generations[handle] = task
    }

    func cancelGeneration(handle: String) {
        if let conversation = conversations[handle] {
            try? conversation.cancel()
        }
        generations[handle]?.cancel()
        generations[handle] = nil
    }

    // MARK: - Helpers

    private func initialMessages(fromHistoryJson json: String) -> [Message] {
        guard
            let data = json.data(using: .utf8),
            let array = (try? JSONSerialization.jsonObject(with: data)) as? [[String: Any]]
        else { return [] }
        return array.compactMap { item in
            guard let role = item["role"] as? String, let text = item["text"] as? String else {
                return nil
            }
            return Message(text, role: role == "user" ? .user : .model)
        }
    }

    private func tools(for ids: [String]) -> [any Tool] {
        ids.compactMap { id in
            switch id {
            case "get_current_time": return GetCurrentTimeTool()
            case "calculate": return CalculateTool()
            case "set_theme": return SetThemeTool()
            case "set_language": return SetLanguageTool()
            case "random_number": return RandomNumberTool()
            case "device_info": return DeviceInfoTool()
            case "load_skill": return LoadSkillTool()
            default: return nil
            }
        }
    }

    private func describe(_ error: Error) -> String {
        let text = error.localizedDescription
        return text.isEmpty ? String(describing: error) : text
    }
}
