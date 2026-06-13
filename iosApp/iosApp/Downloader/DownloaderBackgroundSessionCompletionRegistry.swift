import Foundation

/// Holds the UIKit completion handlers the system passes in
/// `application(_:handleEventsForBackgroundURLSession:completionHandler:)`
/// until the URLSession has drained all its delegate events.
final class DownloaderBackgroundSessionCompletionRegistry: @unchecked Sendable {

    static let shared = DownloaderBackgroundSessionCompletionRegistry()

    private let lock = NSLock()
    private var handlers: [String: () -> Void] = [:]

    func register(identifier: String, completionHandler: @escaping () -> Void) {
        lock.lock()
        handlers[identifier] = completionHandler
        lock.unlock()
    }

    func complete(identifier: String) {
        lock.lock()
        let handler = handlers.removeValue(forKey: identifier)
        lock.unlock()
        guard let handler else { return }
        DispatchQueue.main.async {
            handler()
        }
    }
}
