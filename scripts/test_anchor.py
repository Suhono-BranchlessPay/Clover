"""Test BP anchor API with license key from .env (M1 validation)."""

from __future__ import annotations

import hashlib
import json
import os
import sys
import uuid
from datetime import datetime, timezone

import requests
from dotenv import load_dotenv

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
load_dotenv(os.path.join(ROOT, ".env"), override=True)

API_URL = os.getenv("BP_API_URL", "https://branchlesspay.com/api/v1/anchor").rstrip("/")
LICENSE_KEY = os.getenv("BP_LICENSE_KEY", "").strip()


def legacy_content_hash(payload: dict) -> str:
    data = {k: v for k, v in payload.items() if k != "content_hash"}
    canonical = json.dumps(data, sort_keys=True, ensure_ascii=False)
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def build_test_payload() -> dict:
    suffix = uuid.uuid4().hex[:8].upper()
    return {
        "event_type": "sunmi_transaction",
        "reference_id": "TEST-%s" % suffix,
        "amount": 10000,
        "currency": "IDR",
        "timestamp": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "vendor": "sunmi",
        "merchant_id": "DEV-SN-001",
        "metadata": {
            "erp": "sunmi_pos",
            "erp_system": "Sunmi Android POS",
            "device_model": "V2 Pro",
            "device_sn": "DEV-SN-001",
        },
    }


def main() -> int:
    if not LICENSE_KEY:
        print("ERROR: BP_LICENSE_KEY not set in .env")
        return 1

    payload = build_test_payload()
    payload["content_hash"] = legacy_content_hash(payload)
    headers = {
        "Authorization": "Bearer %s" % LICENSE_KEY,
        "Content-Type": "application/json",
    }

    print("POST", API_URL)
    response = requests.post(API_URL, json=payload, headers=headers, timeout=20)
    print("HTTP", response.status_code)
    print(response.text[:500])

    if response.status_code not in (200, 202):
        return 1

    body = response.json()
    anchor_id = body.get("anchor_id")
    if anchor_id:
        print("\nVerify URL: https://branchlesspay.com/verify/%s" % anchor_id)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
