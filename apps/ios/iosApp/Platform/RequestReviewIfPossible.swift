import ComposeApp
import StoreKit
import UIKit

class IOSRequestReviewIfPossible: RequestReviewIfPossible {

    @MainActor
    func __invoke() async throws {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first(where: { $0.activationState == .foregroundActive })
            ?? UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .first
        guard let windowScene = scene else { return }
        AppStore.requestReview(in: windowScene)
    }

    func invoke(completionHandler: @escaping (Error?) -> Void) {
        Task {
            do {
                try await __invoke()
                completionHandler(nil)
            } catch {
                completionHandler(error)
            }
        }
    }
}
