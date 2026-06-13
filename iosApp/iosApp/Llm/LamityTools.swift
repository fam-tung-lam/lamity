import Foundation
import Shared
import LiteRTLM

/// Built-in tools for the Swift LiteRT-LM runtime.
///
/// Names, descriptions and parameter names mirror the shared specs in
/// `tools/ToolRegistry.kt`; execution is delegated to the shared Kotlin
/// dispatcher so behavior (and tool-event UI) is identical on both platforms.
enum LamityToolRunner {
    static func run(_ toolId: String, _ params: [String: Any]) -> Any {
        let cleaned = params.filter { !(($0.value as? String)?.isEmpty ?? false) }
        let json: String
        if let data = try? JSONSerialization.data(withJSONObject: cleaned),
           let text = String(data: data, encoding: .utf8) {
            json = text
        } else {
            json = "{}"
        }
        let result = MainViewControllerKt.executeBuiltinTool(toolId: toolId, paramsJson: json)
        if let data = result.data(using: .utf8),
           let object = try? JSONSerialization.jsonObject(with: data) {
            return object
        }
        return result
    }
}

struct GetCurrentTimeTool: Tool {
    static let name = "get_current_time"
    static let description = "Get the current date and time. Optionally pass an IANA timezone "
        + "id such as 'Asia/Ho_Chi_Minh'; defaults to the device timezone."

    @ToolParam(description: "Optional IANA timezone id, e.g. 'Europe/Paris'.")
    var timezone: String = ""

    func run() async throws -> Any {
        LamityToolRunner.run(Self.name, ["timezone": timezone])
    }
}

struct CalculateTool: Tool {
    static let name = "calculate"
    static let description = "Evaluate a math expression. Supports + - * / % ^, parentheses, "
        + "pi, e and functions sin cos tan sqrt abs ln log exp floor ceil round min max pow. "
        + "Trigonometry uses radians."

    @ToolParam(description: "The expression to evaluate, e.g. '2*(3+4)^2'.")
    var expression: String = ""

    func run() async throws -> Any {
        LamityToolRunner.run(Self.name, ["expression": expression])
    }
}

struct SetThemeTool: Tool {
    static let name = "set_theme"
    static let description = "Change the app color theme. Mode must be 'light', 'dark' or 'system'."

    @ToolParam(description: "One of: light, dark, system.")
    var mode: String = ""

    func run() async throws -> Any {
        LamityToolRunner.run(Self.name, ["mode": mode])
    }
}

struct SetLanguageTool: Tool {
    static let name = "set_language"
    static let description = "Change the app interface language. Supported: 'en' (English), "
        + "'vi' (Tiếng Việt), 'es' (Español)."

    @ToolParam(description: "One of: en, vi, es.")
    var language: String = ""

    func run() async throws -> Any {
        LamityToolRunner.run(Self.name, ["language": language])
    }
}

struct RandomNumberTool: Tool {
    static let name = "random_number"
    static let description = "Generate a random number between min and max (inclusive). "
        + "Defaults: min 1, max 100, integer true."

    @ToolParam(description: "Lower bound. Default 1.")
    var min: Double = 1

    @ToolParam(description: "Upper bound. Default 100.")
    var max: Double = 100

    @ToolParam(description: "Return an integer when true. Default true.")
    var integer: Bool = true

    func run() async throws -> Any {
        LamityToolRunner.run(Self.name, ["min": min, "max": max, "integer": integer])
    }
}

struct DeviceInfoTool: Tool {
    static let name = "device_info"
    static let description = "Get information about the device this app runs on: platform, "
        + "OS version and device model."

    func run() async throws -> Any {
        LamityToolRunner.run(Self.name, [:])
    }
}

struct LoadSkillTool: Tool {
    static let name = "load_skill"
    static let description = "Load the full instructions of an available skill by its exact "
        + "name. Always call this before applying a skill."

    @ToolParam(description: "Exact name of the skill to load.")
    var skill_name: String = ""

    func run() async throws -> Any {
        LamityToolRunner.run(Self.name, ["skill_name": skill_name])
    }
}
