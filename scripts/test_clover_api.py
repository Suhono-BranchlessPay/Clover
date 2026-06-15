"""Validate Clover API token from .env (M1 smoke test)."""

from __future__ import annotations

import os
import sys

import requests
from dotenv import load_dotenv

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
load_dotenv(os.path.join(ROOT, ".env"), override=True)

API_BASE = os.getenv("CLOVER_API_BASE", "https://apisandbox.dev.clover.com").rstrip("/")
ACCESS_TOKEN = os.getenv("CLOVER_API_TOKEN", "").strip()
MERCHANT_ID = os.getenv("CLOVER_MERCHANT_ID", "").strip()


def main() -> int:
    if not ACCESS_TOKEN:
        print("SKIP: CLOVER_API_TOKEN not set in .env")
        return 0

    headers = {
        "Authorization": "Bearer %s" % ACCESS_TOKEN,
        "Accept": "application/json",
    }

    if MERCHANT_ID:
        url = "%s/v3/merchants/%s" % (API_BASE, MERCHANT_ID)
        print("GET", url)
        response = requests.get(url, headers=headers, timeout=20)
    else:
        url = "%s/v3/merchants?limit=1" % API_BASE
        print("GET", url, "(no CLOVER_MERCHANT_ID — listing merchants)")
        response = requests.get(url, headers=headers, timeout=20)

    print("HTTP", response.status_code)
    print(response.text[:500])

    if response.status_code == 401:
        print("\nERROR: Clover token rejected (401). Check CLOVER_API_TOKEN.")
        return 1
    if response.status_code >= 400:
        return 1

    print("\nOK: Clover API token accepted.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
