import re
from unittest import TestCase
from urllib.parse import urljoin

import requests
from requests import Response


class SmokeTestCase(TestCase):
    LZ_HOST = None
    USER_TOKEN = None

    @staticmethod
    def build_lz_url(path: str) -> str:
        assert SmokeTestCase.LZ_HOST, "ERROR - BPMSmokeTests.LZ_HOST not properly set"
        if re.match(r"^\s*https?://", SmokeTestCase.LZ_HOST):
            return urljoin(SmokeTestCase.LZ_HOST, path)
        else:
            return urljoin(f"https://{SmokeTestCase.LZ_HOST}", path)

    @staticmethod
    def call_lz(url: str, params: dict = None, user_token: str = None) -> Response:
        """Function is memoized so that we only make the call once"""
        headers = {"Authorization": f"Bearer {user_token}"} if user_token else {}
        return requests.get(url, params=params, headers=headers)
