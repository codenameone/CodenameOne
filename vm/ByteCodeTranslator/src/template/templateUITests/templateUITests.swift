import XCTest

final class templateUITests: XCTestCase {
    func testExample() {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.exists)
    }
}
