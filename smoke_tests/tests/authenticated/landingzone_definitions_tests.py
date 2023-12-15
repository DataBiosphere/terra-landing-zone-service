import json

from tests.smoke_test_case import SmokeTestCase

"""Validates landingzone definitions endpoint"""


class LandingZoneDefinitionsTests(SmokeTestCase):
    CROMWELL_LZ_DEFINITION_NAME = "CromwellBaseResourcesFactory"

    @staticmethod
    def status_url() -> str:
        return SmokeTestCase.build_lz_url("/api/landingzones/definitions/v1/azure")

    """Validates that definitions endpoint return 401 when no token provided"""

    def test_status_code_is_401_when_no_token_provided(self):
        response = SmokeTestCase.call_lz(self.status_url())
        self.assertEqual(response.status_code, 401)

    """Validates lz definition response"""

    def test_definitions(self):
        response = SmokeTestCase.call_lz(self.status_url(), user_token=SmokeTestCase.USER_TOKEN)
        self.assertEqual(response.status_code, 200)
        lz_def_response = json.loads(response.text)
        self.assertIsNotNone(lz_def_response["landingzones"], "landingzones property is not defined.")
        self.assertEqual(len(lz_def_response["landingzones"]), 2, "unexpected number of landing zone definitions")
        lzs_definitions = list(lz_def_response["landingzones"])
        '''
        Currently the definition endpoint return 2 definitions:
            CromwellBaseResourcesFactory, ManagedNetworkWithSharedResourcesFactory.
        But the last one is not used. So this test validates existence of cromwell base landing zone only.
        Also the definition for protected data will be added soon.
        '''
        filtered_cromwell_lz_def = filter(
            lambda x: x["definition"] == LandingZoneDefinitionsTests.CROMWELL_LZ_DEFINITION_NAME, lzs_definitions)
        self.assertTrue(len(list(filtered_cromwell_lz_def)) == 1,
                        "unexpected response for CromwellBaseResourcesFactory definition")
