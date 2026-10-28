import SwiftUI
import UIKit
import shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

/// Edge to edge, as on Android: Compose draws behind the status bar, home
/// indicator and keyboard and keeps content clear of them itself (the tab
/// scaffold, and safeDrawingPadding on full-screen pages - see App.kt), so
/// the app's own background fills the whole screen instead of the system's
/// white or black showing above and below it.
struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
