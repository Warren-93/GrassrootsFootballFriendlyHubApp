import SwiftUI
import UIKit
import UserNotifications
import shared

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

/// Push notifications. Asks for permission at launch, registers with APNs,
/// and hands the device token to the shared Kotlin code (PushBridge.kt),
/// which sends it to `POST /api/v1/me/push-tokens` once someone is signed in.
class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, _ in
            guard granted else { return }
            PushBridgeKt.markIosPushAvailable()
            // Registering has to happen on the main thread.
            DispatchQueue.main.async {
                UIApplication.shared.registerForRemoteNotifications()
            }
        }
        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        // The API stores APNs tokens as lowercase hex.
        let hex = deviceToken.map { String(format: "%02x", $0) }.joined()
        PushBridgeKt.registerIosPushToken(hexToken: hex)
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        // Nothing to show anyone: without a token the app simply gets no
        // push, which is also the case on a simulator with no APNs support.
        NSLog("Kickabout: could not register for push notifications - %@", error.localizedDescription)
    }

    /// iOS hides notifications for the app you're already looking at; show them as banners instead.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .badge, .sound])
    }
}
