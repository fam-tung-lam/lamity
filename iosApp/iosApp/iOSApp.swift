import SwiftUI

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        DownloaderBootstrap.install()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
