import argparse
import sys
import unittest
from unittest import TestSuite
import requests

from tests.smoke_test_case import SmokeTestCase
from tests.unauthenticated.status_tests import StatusTests
from tests.unauthenticated.version_tests import VersionTests
from tests.authenticated.landingzone_definitions_tests import LandingZoneDefinitionsTests
from tests.authenticated.landingzone_list_tests import LandingZoneListTests

DESCRIPTION = """
LandingZone Smoke Test
Enter the host (domain and optional port) of the LandingZone instance you want to to test. This test will ensure that the LandingZone 
instance running on that host is minimally functional.
"""


def gather_tests(is_authenticated: bool = False) -> TestSuite:
    suite = unittest.TestSuite()
    status_tests = unittest.defaultTestLoader.loadTestsFromTestCase(StatusTests)
    suite.addTests(status_tests)
    version_tests = unittest.defaultTestLoader.loadTestsFromTestCase(VersionTests)
    suite.addTests(version_tests)
    if is_authenticated:
        lz_definitions_tests = unittest.defaultTestLoader.loadTestsFromTestCase(LandingZoneDefinitionsTests)
        suite.addTests(lz_definitions_tests)
        lz_list_tests = unittest.defaultTestLoader.loadTestsFromTestCase(LandingZoneListTests)
        suite.addTests(lz_list_tests)
    return suite


def main(main_args):
    SmokeTestCase.LZ_HOST = main_args.lz_host
    SmokeTestCase.USER_TOKEN = main_args.user_token

    valid_user_token = main_args.user_token is not None and verify_user_token(main_args.user_token)
    test_suite = gather_tests(valid_user_token)

    runner = unittest.TextTestRunner(verbosity=main_args.verbosity)
    runner.run(test_suite)


def verify_user_token(user_token: str) -> bool:
    response = requests.get(f"https://www.googleapis.com/oauth2/v1/tokeninfo?access_token={user_token}")
    assert response.status_code == 200, "User Token is no longer valid.  Please generate a new token and try again."
    return True


def parse_args():
    parser = argparse.ArgumentParser(
        prog="LandingZone Smoke Tests",
        description=DESCRIPTION,
    )
    parser.add_argument(
        "lz_host",
        type=str,
        help="Required domain with optional port number of the LandingZone host you want to test"
    )
    parser.add_argument(
        "user_token",
        nargs='?',
        default=None,
        type=str,
        help="Optional. If present, will test additional authenticated endpoints using the specified token"
    )
    parser.add_argument(
        "-v",
        "--verbosity",
        type=int,
        choices=[0, 1, 2],
        default=1,
        help="""Python unittest verbosity setting: 
    0: Quiet - Prints only number of tests executed
    1: Minimal - (default) Prints number of tests executed plus a dot for each success and an F for each failure
    2: Verbose - Help string and its result will be printed for each test"""
    )
    parsed_args = parser.parse_args()
    # Need to pop off sys.argv values to avoid messing with args passed to unittest.main()
    for _ in range(len(sys.argv[1:])):
        sys.argv.pop()
    return parsed_args


if __name__ == "__main__":
    args = parse_args()
    main(args)
    sys.exit(0)
