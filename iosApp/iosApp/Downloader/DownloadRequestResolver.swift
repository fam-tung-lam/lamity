import Foundation

/// Builds the `URLRequest` handed to the background session.
///
/// Background sessions follow redirects without consulting their delegate, so
/// an `Authorization` header would leak to every redirect target (S3-style
/// signed URLs reject such requests). When a bearer token is present, the
/// redirect chain is therefore resolved up front with a foreground session —
/// sending the token only to trusted hosts — and the final URL is downloaded
/// without it.
enum DownloadRequestResolver {

    static func resolve(metadata: DownloadMetadata) async throws -> URLRequest {
        guard var url = URL(string: metadata.url) else { throw DownloaderBridgeError.invalidUrl }
        guard metadata.bearerToken != nil else {
            return request(for: url, metadata: metadata, httpMethod: "GET")
        }

        for _ in 0..<8 {
            var headRequest = request(for: url, metadata: metadata, httpMethod: "HEAD")
            headRequest.timeoutInterval = 30
            let (_, response) = try await noRedirectSession.data(for: headRequest)
            guard let http = response as? HTTPURLResponse else { throw DownloaderBridgeError.invalidUrl }
            switch http.statusCode {
            case 301, 302, 303, 307, 308:
                guard
                    let location = http.value(forHTTPHeaderField: "Location"),
                    let next = URL(string: location, relativeTo: url)
                else { throw DownloaderBridgeError.invalidUrl }
                url = next.absoluteURL
            case 200..<300:
                return request(for: url, metadata: metadata, httpMethod: "GET")
            default:
                throw DownloaderBridgeError.httpError(http.statusCode)
            }
        }
        throw DownloaderBridgeError.tooManyRedirects
    }

    private static func request(
        for url: URL,
        metadata: DownloadMetadata,
        httpMethod: String
    ) -> URLRequest {
        var request = URLRequest(url: url)
        request.httpMethod = httpMethod
        request.allowsCellularAccess = !metadata.requireUnmetered
        metadata.headers.forEach { request.setValue($0.value, forHTTPHeaderField: $0.key) }
        if let token = metadata.bearerToken, isTrusted(host: url.host, metadata: metadata) {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        return request
    }

    private static func isTrusted(host: String?, metadata: DownloadMetadata) -> Bool {
        guard let host, !host.isEmpty else { return false }
        var trusted = metadata.trustedAuthHosts
        if trusted.isEmpty, let own = URL(string: metadata.url)?.host {
            trusted = [own]
        }
        return trusted.contains { host == $0 || host.hasSuffix(".\($0)") }
    }

    /// Foreground session whose delegate refuses to follow redirects, so 3xx
    /// responses surface to the caller with their `Location` header.
    private static let noRedirectSession = URLSession(
        configuration: .ephemeral,
        delegate: NoRedirectDelegate(),
        delegateQueue: nil
    )

    private final class NoRedirectDelegate: NSObject, URLSessionTaskDelegate {
        func urlSession(
            _ session: URLSession,
            task: URLSessionTask,
            willPerformHTTPRedirection response: HTTPURLResponse,
            newRequest request: URLRequest,
            completionHandler: @escaping (URLRequest?) -> Void
        ) {
            completionHandler(nil)
        }
    }
}
