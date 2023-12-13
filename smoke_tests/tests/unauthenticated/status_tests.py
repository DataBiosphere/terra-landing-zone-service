import json

from tests.smoke_test_case import SmokeTestCase

class StatusTests(SmokeTestCase):
    @staticmethod
    def status_url() -> str:
        return SmokeTestCase.build_lz_url("/status")

    def test_status_code_is_200(self):
        response = SmokeTestCase.call_lz(self.status_url())
        self.assertEqual(response.status_code, 200)

    def test_subsystems(self):
        response = SmokeTestCase.call_lz(self.status_url())
        status = json.loads(response.text)
        for system in status["systems"]:
            self.assertEqual(status["systems"][system]["ok"], True, f"{system} is not OK")
