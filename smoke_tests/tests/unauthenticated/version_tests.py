import json

from tests.smoke_test_case import SmokeTestCase

class VersionTests(SmokeTestCase):
    @staticmethod
    def status_url() -> str:
        return SmokeTestCase.build_lz_url("/version")

    def test_status_code_is_200(self):
        response = SmokeTestCase.call_lz(self.status_url())
        self.assertEqual(response.status_code, 200)

    def test_version_details(self):
        response = SmokeTestCase.call_lz(self.status_url())
        version_details = json.loads(response.text)
        self.assertIsNotNone(version_details["gitTag"], "gitTag is not defined.")
        self.assertIsNotNone(version_details["gitHash"], "gitHash is not defined.")
        self.assertIsNotNone(version_details["github"], "github is not defined")
        self.assertIsNotNone(version_details["build"], "build is not defined")
