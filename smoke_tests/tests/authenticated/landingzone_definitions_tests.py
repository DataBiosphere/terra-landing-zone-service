import json

from tests.smoke_test_case import SmokeTestCase

"""Validates landing zone definitions endpoint"""


class LandingZoneDefinitionsTests(SmokeTestCase):

    @staticmethod
    def status_url() -> str:
        return SmokeTestCase.build_lz_url("/api/landingzones/definitions/v1/azure")

    def test_status_code_is_401_when_no_token_provided(self):
        """Validates that definitions endpoint return 401 when no token provided"""
        response = SmokeTestCase.call_lz(self.status_url())
        self.assertEqual(response.status_code, 401)

    def test_definitions(self):
        """Validates lz definition response"""
        response = SmokeTestCase.call_lz(self.status_url(), user_token=SmokeTestCase.USER_TOKEN)
        self.assertEqual(response.status_code, 200)
        lz_def_response = json.loads(response.text)
        self.assertIsNotNone(lz_def_response["landingzones"], "landingzones property is not defined.")
        self.assertEqual(len(lz_def_response["landingzones"]), 2, "unexpected number of landing zone definitions")
