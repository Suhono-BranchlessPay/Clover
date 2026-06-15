"""Merge Clover credentials from Downloads .env.txt into repo .env (local only)."""

from __future__ import annotations

import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ENV_PATH = os.path.join(ROOT, ".env")
SOURCE = os.path.join(os.path.dirname(ROOT), "..", "Clover", ".env.txt")
SOURCE = os.path.normpath(os.path.join(ROOT, "..", "..", "Clover", ".env.txt"))
if not os.path.isfile(SOURCE):
    SOURCE = r"C:\Users\Thinkbook\Downloads\Clover\.env.txt"


def sanitize_value(value: str) -> str:
    cleaned = value.strip()
    for suffix in ("visibility_off", "visibility_on"):
        if cleaned.endswith(suffix):
            cleaned = cleaned[: -len(suffix)]
    return cleaned.strip()


def parse_source(path: str) -> dict[str, str]:
    out: dict[str, str] = {}
    with open(path, encoding="utf-8", errors="replace") as handle:
        for line in handle:
            raw = line.strip()
            if not raw or raw.startswith("#"):
                continue
            if "=" in raw and not raw.startswith("{"):
                key, value = raw.split("=", 1)
                key = key.strip()
                value = sanitize_value(value)
                key_upper = key.upper().replace(" ", "_")
                if key_upper in ("MERCHANT_ID", "MERCHANTID"):
                    out["CLOVER_MERCHANT_ID"] = value
                elif key_upper in ("CLOVER_API_TOKEN", "NEW_CLOVER_API_TOKEN"):
                    out["CLOVER_API_TOKEN"] = value
                elif key.startswith("CLOVER_") or key.startswith("BP_"):
                    out[key] = value
                continue
            uuid_token = re.fullmatch(
                r"([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})",
                raw,
                re.IGNORECASE,
            )
            if uuid_token:
                out["CLOVER_API_TOKEN"] = uuid_token.group(1)
                continue            if raw.startswith("{"):
                try:
                    payload = json.loads(raw)
                    token = payload.get("access_token")
                    merchant = payload.get("merchant_id")
                    if token and "CLOVER_API_TOKEN" not in out:
                        out["CLOVER_API_TOKEN"] = sanitize_value(str(token))
                    if merchant and "CLOVER_MERCHANT_ID" not in out:
                        out["CLOVER_MERCHANT_ID"] = sanitize_value(str(merchant))
                    note = str(payload.get("note", "")).lower()
                    if "sandbox" in note and "CLOVER_API_BASE" not in out:
                        out["CLOVER_API_BASE"] = "https://apisandbox.dev.clover.com"
                except json.JSONDecodeError:
                    pass
                continue
            match = re.search(r"merchant[_\s-]*id\s*[:=]\s*([A-Z0-9]+)", raw, re.IGNORECASE)
            if match and "CLOVER_MERCHANT_ID" not in out:
                out["CLOVER_MERCHANT_ID"] = match.group(1)
            match = re.search(r"bp_[a-z0-9_]+", raw, re.IGNORECASE)
            if match and "BP_LICENSE_KEY" not in out:
                out["BP_LICENSE_KEY"] = match.group(0)
            match = re.search(r"(?:clover[_\s-]*)?(?:api[_\s-]*)?token\s*[:=]\s*(\S+)", raw, re.IGNORECASE)
            if match and "CLOVER_API_TOKEN" not in out:
                out["CLOVER_API_TOKEN"] = sanitize_value(match.group(1))
    return out


def read_env(path: str) -> list[str]:
    if not os.path.isfile(path):
        return []
    with open(path, encoding="utf-8", errors="replace") as handle:
        return handle.read().splitlines()


def write_env(path: str, lines: list[str]) -> None:
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        handle.write("\n".join(lines).rstrip() + "\n")


def upsert(lines: list[str], key: str, value: str) -> list[str]:
    prefix = key + "="
    replaced = False
    result: list[str] = []
    for line in lines:
        if line.startswith(prefix):
            result.append(prefix + value)
            replaced = True
        else:
            result.append(line)
    if not replaced:
        if result and result[-1].strip():
            result.append("")
        result.append(prefix + value)
    return result


def main() -> int:
    if not os.path.isfile(SOURCE):
        print("Source not found:", SOURCE)
        return 1

    incoming = parse_source(SOURCE)
    lines = read_env(ENV_PATH)
    if not lines:
        lines = ["# Local dev — do NOT commit"]

    for key in (
        "BP_LICENSE_KEY",
        "BP_API_URL",
        "CLOVER_APP_ID",
        "CLOVER_APP_SECRET",
        "CLOVER_API_TOKEN",
        "CLOVER_MERCHANT_ID",
        "CLOVER_API_BASE",
    ):
        if incoming.get(key):
            lines = upsert(lines, key, incoming[key])

    write_env(ENV_PATH, lines)
    print("Updated .env keys:", ", ".join(sorted(incoming.keys())))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
