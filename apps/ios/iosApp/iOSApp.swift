import SwiftUI
import UIKit
import ComposeApp

@main
struct iOSApp: App {

    let requestReviewIfPossible = IOSRequestReviewIfPossible()
    private let iOSAppComponent: IosAppComponent

    init() {
        self.iOSAppComponent = create(
            requestReviewIfPossible: requestReviewIfPossible
        )
        iOSAppComponent.telemetry.initialize()
        // Force eager initialization of lifecycle observer
        _ = iOSAppComponent.appEventDispatcher
    }

    var body: some Scene {
        WindowGroup {
            RootComposeView(appComponent: iOSAppComponent)
                .ignoresSafeArea()
        }
    }
}

struct RootComposeView: UIViewControllerRepresentable {
    let appComponent: IosAppComponent

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(appComponent: appComponent)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
