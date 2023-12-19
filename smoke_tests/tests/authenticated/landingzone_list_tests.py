from tests.smoke_test_case import SmokeTestCase

"""Validates landing zone endpoint which returns all landing zones available to user."""


class LandingZoneListTests(SmokeTestCase):

    @staticmethod
    def status_url() -> str:
        return SmokeTestCase.build_lz_url("/api/landingzones/v1/azure")

    def test_status_code_is_401_when_no_token_provided(self):
        """Validates that lz create endpoint return 401 when no payload provided"""
        response = SmokeTestCase.call_lz(self.status_url())
        self.assertEqual(response.status_code, 401)

    def test_available_landingzones(self):
        response = SmokeTestCase.call_lz(self.status_url(), user_token=SmokeTestCase.USER_TOKEN)
        self.assertEqual(response.status_code, 200)
