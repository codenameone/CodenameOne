import unittest

from upload_youtube_studio import StudioUploader, chrome_user_agent


class FakeElement:
    def __init__(self, displayed=True, href=None):
        self.displayed = displayed
        self.href = href

    def is_displayed(self):
        return self.displayed

    def get_attribute(self, name):
        return self.href if name == "href" else None


class FakeDriver:
    def __init__(self, warning=True):
        self.warning = warning
        self.urls = []
        self.clicked = []

    def get(self, url):
        self.urls.append(url)
        if "skip_browser_check" in url:
            self.warning = False

    def find_elements(self, by, selector):
        if self.warning and selector == "//*[normalize-space(.)='SKIP TO YOUTUBE STUDIO']":
            return [FakeElement(False), FakeElement(True, "https://studio.youtube.com/?skip_browser_check=true")]
        if not self.warning and selector == "ytcp-app":
            return [FakeElement(True)]
        return []

    def execute_script(self, script, element):
        self.clicked.append(element)
        self.warning = False


class NavigateTest(unittest.TestCase):
    def test_headless_user_agent_is_a_supported_chrome_identity(self):
        agent = chrome_user_agent(150)
        self.assertIn("Chrome/150.0.0.0", agent)
        self.assertNotIn("HeadlessChrome", agent)

    def uploader(self, driver):
        uploader = StudioUploader.__new__(StudioUploader)
        uploader.driver = driver
        uploader.body = lambda: (
            "You are using an unsupported browser. SKIP TO YOUTUBE STUDIO"
            if driver.warning else "Channel content"
        )
        return uploader

    def test_dismisses_supported_browser_warning(self):
        driver = FakeDriver(warning=True)
        self.uploader(driver).navigate("https://studio.youtube.com/channel/test/videos")
        self.assertEqual(
            [
                "https://studio.youtube.com/channel/test/videos",
                "https://studio.youtube.com/channel/test/videos?skip_browser_check=true",
            ],
            driver.urls,
        )
        self.assertEqual(0, len(driver.clicked))
        self.assertFalse(driver.warning)

    def test_normal_page_is_not_clicked(self):
        driver = FakeDriver(warning=False)
        self.uploader(driver).navigate("https://studio.youtube.com/channel/test/videos")
        self.assertEqual([], driver.clicked)


if __name__ == "__main__":
    unittest.main()
