"""Diagnose Clover sandbox token permissions."""

from __future__ import annotations

import os
import sys

import requests
from dotenv import load_dotenv

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
load_dotenv(os.path.join(ROOT, ".env"), override=True)

TOKEN = os.getenv("CLOVER_API_TOKEN", "").strip()
MID = os.getenv("CLOVER_MERCHANT_ID", "").strip()
REST = os.getenv("CLOVER_API_BASE", "https://apisandbox.dev.clover.com").rstrip("/")
ECOMM = "https://scl-sandbox.dev.clover.com"

ORDER_ID = "KA6JTN3VX023P"
PAYMENT_ID = "OS69ARZ44Q8QC"


def check(label: str, url: str) -> int:
    response = requests.get(
        url,
        headers={"Authorization": f"Bearer {TOKEN}", "Accept": "application/json"},
        timeout=20,
    )
    print(f"{label:20} HTTP {response.status_code}  {response.text[:100]}")
    return response.status_code


def main() -> int:
    if not TOKEN or not MID:
        print("ERROR: CLOVER_API_TOKEN and CLOVER_MERCHANT_ID required in .env")
        return 1

    print(f"Token: {TOKEN[:8]}... ({len(TOKEN)} chars)")
    print(f"Merchant: {MID}\n")

    merchant_ok = check("REST merchant", f"{REST}/v3/merchants/{MID}") == 200
    rest_pay = check(
        "REST payment",
        f"{REST}/v3/merchants/{MID}/payments/{PAYMENT_ID}",
    )
    rest_order = check(
        "REST order",
        f"{REST}/v3/merchants/{MID}/orders/{ORDER_ID}",
    )
    ecomm_order = check("ECOMM order", f"{ECOMM}/v1/orders/{ORDER_ID}")

    print()
    if ecomm_order == 200:
        print("OK: ECOMM read works — run E2E without --fixture")
        return 0
    if ecomm_order == 403:
        print("BLOCKED: ECOMM read permission missing on this token.")
        print("Fix: Test Merchants -> Bp Audit shield -> gear -> API Tokens -> NEW token")
        print("     Enable Ecommerce + Orders + Payments READ, then update .env.txt")
    if rest_pay == 404 and rest_order == 404:
        print("NOTE: ECOMM sale not visible on REST v3 — need ECOMM token OR new Register sale.")

    if merchant_ok:
        print("\nWorkaround: npm run e2e:sandbox -- --payment-id=... --order-id=... --fixture=true")
    return 2 if ecomm_order != 200 else 0


if __name__ == "__main__":
    raise SystemExit(main())
