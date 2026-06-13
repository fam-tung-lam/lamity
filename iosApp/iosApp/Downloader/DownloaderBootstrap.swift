import Shared

/// Wires the Swift downloader into shared Kotlin code. Call once at startup,
/// before the Compose entry point resolves the `Downloader`.
enum DownloaderBootstrap {
    static func install() {
        guard LamityDownloaderIos.shared.bridge == nil else { return }
        LamityDownloaderIos.shared.bridge = LamityDownloaderBridgeImpl()
    }
}
