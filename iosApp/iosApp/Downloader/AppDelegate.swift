import UIKit

final class AppDelegate: NSObject, UIApplicationDelegate {

    /// Called when the system relaunches/wakes the app because background
    /// downloads finished; recreating the bridge reattaches its session, and
    /// the completion handler is invoked once all delegate events are drained.
    func application(
        _ application: UIApplication,
        handleEventsForBackgroundURLSession identifier: String,
        completionHandler: @escaping () -> Void
    ) {
        DownloaderBackgroundSessionCompletionRegistry.shared.register(
            identifier: identifier,
            completionHandler: completionHandler
        )
        DownloaderBootstrap.install()
    }
}
