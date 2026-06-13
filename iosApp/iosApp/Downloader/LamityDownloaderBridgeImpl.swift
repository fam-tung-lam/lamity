import CryptoKit
import Foundation
import Shared

/// Background-`URLSession` implementation of the Kotlin `LamityDownloaderBridge`.
///
/// Downloads continue while the app is suspended; the system relaunches the
/// app when they finish (see `AppDelegate` + `DownloaderBackgroundSessionCompletionRegistry`).
/// Pause produces resume data, completion optionally verifies SHA-256, and all
/// state changes are reported back to Kotlin through the registered observer.
final class LamityDownloaderBridgeImpl: NSObject, LamityDownloaderBridge, URLSessionDownloadDelegate, @unchecked Sendable {

    static let backgroundSessionIdentifier = "com.phamtunglam.lamity.downloads"

    private let fileManager = FileManager.default
    private let lock = NSLock()
    private var observers: [String: DownloadObserver] = [:]
    private var rateSamples: [String: [DownloadRateSample]] = [:]
    private lazy var session: URLSession = {
        let config = URLSessionConfiguration.background(
            withIdentifier: Self.backgroundSessionIdentifier
        )
        config.sessionSendsLaunchEvents = true
        config.isDiscretionary = false
        return URLSession(configuration: config, delegate: self, delegateQueue: nil)
    }()

    override init() {
        super.init()
        reattachActiveTasks()
    }

    // MARK: - LamityDownloaderBridge

    func start(
        id: String,
        url: String,
        destinationPath: String,
        headerKeys: [String],
        headerValues: [String],
        bearerToken: String?,
        trustedAuthHosts: [String],
        expectedSizeBytes: Int64,
        sha256: String?,
        requireUnmetered: Bool
    ) {
        let metadata = DownloadMetadata(
            id: id,
            url: url,
            destinationPath: destinationPath,
            headers: Dictionary(uniqueKeysWithValues: zip(headerKeys, headerValues)),
            bearerToken: bearerToken,
            trustedAuthHosts: trustedAuthHosts,
            expectedSizeBytes: expectedSizeBytes,
            sha256: sha256,
            requireUnmetered: requireUnmetered
        )
        do {
            try save(metadata)
        } catch {
            fail(id: id, message: error.localizedDescription)
            return
        }
        try? fileManager.removeItem(at: resumeDataURL(for: id))
        cancelTasks(id: id) { [weak self] in
            self?.startTask(metadata: metadata, resumeData: nil)
        }
    }

    func pause(id: String) {
        session.getAllTasks { tasks in
            let matching = tasks.matchingDownload(id: id)
            guard !matching.isEmpty else { return }
            for task in matching {
                guard let downloadTask = task as? URLSessionDownloadTask else {
                    task.cancel()
                    continue
                }
                downloadTask.cancel { [weak self] resumeData in
                    guard let self else { return }
                    if let resumeData {
                        try? resumeData.write(to: self.resumeDataURL(for: id), options: .atomic)
                    }
                    self.emit(id: id, state: "PAUSED", downloadedBytes: downloadTask.countOfBytesReceived,
                              totalBytes: self.metadata(for: id)?.expectedSizeBytes ?? 0)
                }
            }
        }
    }

    func resume(id: String) {
        guard let metadata = metadata(for: id) else {
            fail(id: id, message: "No stored download request for '\(id)'.")
            return
        }
        let resumeData = try? Data(contentsOf: resumeDataURL(for: id))
        try? fileManager.removeItem(at: resumeDataURL(for: id))
        startTask(metadata: metadata, resumeData: resumeData)
    }

    func cancel(id: String) {
        cancelTasks(id: id) { [weak self] in
            guard let self else { return }
            try? self.fileManager.removeItem(at: self.metadataURL(for: id))
            try? self.fileManager.removeItem(at: self.resumeDataURL(for: id))
            self.emit(id: id, state: "CANCELLED", downloadedBytes: 0)
        }
    }

    func observe(
        id: String,
        onProgress: @escaping (String, KotlinLong, KotlinLong, KotlinLong, KotlinLong) -> Void,
        onError: @escaping (String) -> Void
    ) {
        lock.lock()
        observers[id] = DownloadObserver(onProgress: onProgress, onError: onError)
        lock.unlock()
        reportKnownState(id: id)
    }

    // MARK: - URLSessionDownloadDelegate

    func urlSession(
        _ session: URLSession,
        downloadTask: URLSessionDownloadTask,
        didWriteData bytesWritten: Int64,
        totalBytesWritten: Int64,
        totalBytesExpectedToWrite: Int64
    ) {
        guard let id = downloadTask.taskDescription else { return }
        let expected = metadata(for: id)?.expectedSizeBytes ?? 0
        let totalBytes = expected > 0 ? expected : max(totalBytesExpectedToWrite, 0)
        let rate = recordRate(id: id, bytesWritten: bytesWritten,
                              downloadedBytes: totalBytesWritten, totalBytes: totalBytes)
        emit(
            id: id,
            state: "RUNNING",
            downloadedBytes: totalBytesWritten,
            totalBytes: totalBytes,
            bytesPerSecond: rate.bytesPerSecond,
            etaMillis: rate.etaMillis
        )
    }

    func urlSession(
        _ session: URLSession,
        downloadTask: URLSessionDownloadTask,
        didFinishDownloadingTo location: URL
    ) {
        guard let id = downloadTask.taskDescription, let metadata = metadata(for: id) else { return }
        let status = (downloadTask.response as? HTTPURLResponse)?.statusCode ?? 200
        guard (200..<300).contains(status) else {
            fail(id: id, message: "HTTP \(status)" + (status == 401 || status == 403
                ? " — the resource may require a valid access token" : ""))
            return
        }
        do {
            if let expectedSha256 = metadata.sha256 {
                emit(id: id, state: "VERIFYING",
                     downloadedBytes: metadata.expectedSizeBytes, totalBytes: metadata.expectedSizeBytes)
                let actual = try sha256Hex(of: location)
                guard actual.lowercased() == expectedSha256.lowercased() else {
                    throw DownloaderBridgeError.checksumMismatch
                }
            }
            let destination = URL(fileURLWithPath: metadata.destinationPath)
            try fileManager.createDirectory(
                at: destination.deletingLastPathComponent(),
                withIntermediateDirectories: true
            )
            try? fileManager.removeItem(at: destination)
            try fileManager.moveItem(at: location, to: destination)
            try? fileManager.removeItem(at: metadataURL(for: id))
            try? fileManager.removeItem(at: resumeDataURL(for: id))
            clearRate(id: id)
            emit(id: id, state: "SUCCEEDED",
                 downloadedBytes: metadata.expectedSizeBytes, totalBytes: metadata.expectedSizeBytes)
        } catch {
            fail(id: id, message: error.localizedDescription)
        }
    }

    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        guard let id = task.taskDescription, let error = error as NSError? else { return }
        if error.code == NSURLErrorCancelled { return } // pause()/cancel() already reported
        if let resumeData = error.userInfo[NSURLSessionDownloadTaskResumeData] as? Data {
            try? resumeData.write(to: resumeDataURL(for: id), options: .atomic)
            emit(id: id, state: "PAUSED", downloadedBytes: 0,
                 totalBytes: metadata(for: id)?.expectedSizeBytes ?? 0)
        } else {
            fail(id: id, message: error.localizedDescription)
        }
    }

    func urlSessionDidFinishEvents(forBackgroundURLSession session: URLSession) {
        DownloaderBackgroundSessionCompletionRegistry.shared.complete(
            identifier: Self.backgroundSessionIdentifier
        )
    }

    // MARK: - Task management

    private func startTask(metadata: DownloadMetadata, resumeData: Data?) {
        if let resumeData {
            let task = session.downloadTask(withResumeData: resumeData)
            task.taskDescription = metadata.id
            task.resume()
            emit(id: metadata.id, state: "QUEUED", downloadedBytes: 0, totalBytes: metadata.expectedSizeBytes)
            return
        }
        Task { [weak self] in
            guard let self else { return }
            do {
                let request = try await DownloadRequestResolver.resolve(metadata: metadata)
                let task = self.session.downloadTask(with: request)
                task.taskDescription = metadata.id
                task.resume()
                self.emit(id: metadata.id, state: "QUEUED",
                          downloadedBytes: 0, totalBytes: metadata.expectedSizeBytes)
            } catch {
                self.fail(id: metadata.id, message: error.localizedDescription)
            }
        }
    }

    private func cancelTasks(id: String, then completion: @escaping () -> Void) {
        session.getAllTasks { tasks in
            tasks.matchingDownload(id: id).forEach { $0.cancel() }
            completion()
        }
    }

    private func reattachActiveTasks() {
        session.getAllTasks { [weak self] tasks in
            guard let self else { return }
            for id in tasks.compactMap(\.taskDescription) {
                guard let metadata = self.metadata(for: id) else { continue }
                self.emit(id: id, state: "RUNNING", downloadedBytes: 0,
                          totalBytes: metadata.expectedSizeBytes)
            }
        }
    }

    private func reportKnownState(id: String) {
        session.getAllTasks { [weak self] tasks in
            guard let self else { return }
            let active = tasks.matchingDownload(id: id).first
            if let active {
                self.emit(id: id, state: "RUNNING",
                          downloadedBytes: active.countOfBytesReceived,
                          totalBytes: self.metadata(for: id)?.expectedSizeBytes ?? 0)
            } else if let metadata = self.metadata(for: id) {
                // Stored request without a live task: resumable.
                self.emit(id: id, state: "PAUSED", downloadedBytes: 0,
                          totalBytes: metadata.expectedSizeBytes)
            }
        }
    }

    // MARK: - Progress plumbing

    private func emit(
        id: String,
        state: String,
        downloadedBytes: Int64,
        totalBytes: Int64 = 0,
        bytesPerSecond: Int64 = 0,
        etaMillis: Int64 = 0
    ) {
        lock.lock()
        let observer = observers[id]
        lock.unlock()
        observer?.onProgress(
            state,
            KotlinLong(value: downloadedBytes),
            KotlinLong(value: totalBytes),
            KotlinLong(value: bytesPerSecond),
            KotlinLong(value: etaMillis)
        )
    }

    private func fail(id: String, message: String) {
        clearRate(id: id)
        lock.lock()
        let observer = observers[id]
        lock.unlock()
        observer?.onError(message)
    }

    private func recordRate(
        id: String,
        bytesWritten: Int64,
        downloadedBytes: Int64,
        totalBytes: Int64
    ) -> (bytesPerSecond: Int64, etaMillis: Int64) {
        lock.lock()
        var samples = rateSamples[id, default: []]
        samples.append(DownloadRateSample(bytes: bytesWritten, timestamp: Date().timeIntervalSince1970))
        samples = Array(samples.suffix(5))
        rateSamples[id] = samples
        lock.unlock()

        guard let first = samples.first, let last = samples.last, last.timestamp > first.timestamp else {
            return (0, 0)
        }
        let bytesPerSecond = Int64(
            Double(samples.map(\.bytes).reduce(0, +)) / (last.timestamp - first.timestamp)
        )
        let etaMillis = bytesPerSecond > 0 && totalBytes > 0
            ? max(0, totalBytes - downloadedBytes) * 1_000 / bytesPerSecond
            : 0
        return (bytesPerSecond, etaMillis)
    }

    private func clearRate(id: String) {
        lock.lock()
        rateSamples[id] = nil
        lock.unlock()
    }

    // MARK: - Persistence

    private var storeDir: URL {
        fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("LamityDownloads", isDirectory: true)
    }

    private func metadataURL(for id: String) -> URL {
        storeDir.appendingPathComponent("\(id.fileSafeName).json")
    }

    private func resumeDataURL(for id: String) -> URL {
        storeDir.appendingPathComponent("\(id.fileSafeName).resume")
    }

    private func save(_ metadata: DownloadMetadata) throws {
        try fileManager.createDirectory(at: storeDir, withIntermediateDirectories: true)
        let data = try JSONEncoder().encode(metadata)
        try data.write(to: metadataURL(for: metadata.id), options: .atomic)
    }

    private func metadata(for id: String) -> DownloadMetadata? {
        guard let data = try? Data(contentsOf: metadataURL(for: id)) else { return nil }
        return try? JSONDecoder().decode(DownloadMetadata.self, from: data)
    }

    private func sha256Hex(of file: URL) throws -> String {
        let handle = try FileHandle(forReadingFrom: file)
        defer { try? handle.close() }
        var hasher = SHA256()
        while let data = try handle.read(upToCount: 1024 * 1024), !data.isEmpty {
            hasher.update(data: data)
        }
        return hasher.finalize().map { String(format: "%02x", $0) }.joined()
    }
}

// MARK: - Supporting types

struct DownloadMetadata: Codable {
    let id: String
    let url: String
    let destinationPath: String
    let headers: [String: String]
    let bearerToken: String?
    let trustedAuthHosts: [String]
    let expectedSizeBytes: Int64
    let sha256: String?
    let requireUnmetered: Bool
}

private struct DownloadObserver {
    let onProgress: (String, KotlinLong, KotlinLong, KotlinLong, KotlinLong) -> Void
    let onError: (String) -> Void
}

private struct DownloadRateSample {
    let bytes: Int64
    let timestamp: TimeInterval
}

enum DownloaderBridgeError: LocalizedError {
    case checksumMismatch
    case invalidUrl
    case tooManyRedirects
    case httpError(Int)

    var errorDescription: String? {
        switch self {
        case .checksumMismatch:
            return "Checksum mismatch — the downloaded file is corrupt."
        case .invalidUrl:
            return "Invalid download URL."
        case .tooManyRedirects:
            return "Too many redirects."
        case .httpError(let code):
            return "HTTP \(code)" + (code == 401 || code == 403
                ? " — the resource may require a valid access token" : "")
        }
    }
}

private extension Array where Element == URLSessionTask {
    func matchingDownload(id: String) -> [URLSessionTask] {
        filter { $0.taskDescription == id }
    }
}

private extension String {
    var fileSafeName: String {
        addingPercentEncoding(withAllowedCharacters: .alphanumerics) ?? self
    }
}
