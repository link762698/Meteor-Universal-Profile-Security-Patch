#!/usr/bin/env python3
"""
Generate local-only Meteor profile import regression fixtures.

This tool intentionally does not accept arbitrary payload or destination-path
arguments. It creates a benign minimal JAR named test.jar and two blocked-path
profile fixtures for use only inside a disposable Minecraft instance.
"""

from __future__ import annotations

import argparse
import gzip
import json
import struct
import sys
import zipfile
from io import BytesIO
from pathlib import Path


TRAVERSAL_DESTINATION = "../../../mods/test.jar"
OUTPUT_NAMES = {
    "clean": "clean-control.nbt",
    "entry": "blocked-entry-traversal.nbt",
    "profile": "blocked-profile-name.nbt",
    "manifest": "manifest.json",
    "readme": "README.txt",
}


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Generate local-only Meteor profile security regression fixtures."
    )
    parser.add_argument(
        "--template",
        required=True,
        type=Path,
        help="Clean exported Meteor profile .nbt to use as the fixture template.",
    )
    parser.add_argument(
        "--out-dir",
        type=Path,
        default=Path("local-security-fixtures"),
        help="Directory for generated local fixtures. Default: local-security-fixtures",
    )
    parser.add_argument(
        "--i-understand-use-disposable-instance",
        action="store_true",
        help="Required acknowledgement that these fixtures are only for a disposable instance.",
    )

    args = parser.parse_args()

    if not args.i_understand_use_disposable_instance:
        parser.error(
            "refusing to generate fixtures without "
            "--i-understand-use-disposable-instance"
        )

    template_path = args.template.expanduser()
    out_dir = args.out_dir.expanduser()

    raw, body, is_gzip = read_template(template_path)
    payload = zip_shift(make_minimal_empty_jar(), 5)

    out_dir.mkdir(parents=True, exist_ok=True)

    write_bytes(out_dir / OUTPUT_NAMES["clean"], raw)

    entry_inner = replace_profile_name(
        body[:-1],
        "blocked-entry-traversal",
    )
    entry_fixture = (
        entry_inner
        + nbt_byte_array(overlong_modified_utf8(TRAVERSAL_DESTINATION), payload)
        + b"\x00"
    )
    write_fixture(out_dir / OUTPUT_NAMES["entry"], entry_fixture, is_gzip)

    profile_inner = replace_profile_name(
        body[:-1],
        "../../../mods",
    )
    profile_fixture = profile_inner + nbt_byte_array(b"test.jar", payload) + b"\x00"
    write_fixture(out_dir / OUTPUT_NAMES["profile"], profile_fixture, is_gzip)

    write_json(
        out_dir / OUTPUT_NAMES["manifest"],
        {
            "warning": "Local-only disposable-instance fixtures. Do not import into a real client profile.",
            "template": {
                "name": template_path.name,
                "bytes": len(raw),
                "format": "gzip" if is_gzip else "uncompressed",
            },
            "payload": {
                "name": "test.jar",
                "kind": "minimal empty JAR",
                "bytes_after_zip_shift": len(payload),
            },
            "fixtures": [
                {
                    "file": OUTPUT_NAMES["clean"],
                    "purpose": "Clean import control; should import successfully.",
                },
                {
                    "file": OUTPUT_NAMES["entry"],
                    "purpose": "Attempts top-level NBT key traversal to ../../../mods/test.jar; patched client should block.",
                },
                {
                    "file": OUTPUT_NAMES["profile"],
                    "purpose": "Attempts unsafe profile-name containment escape with test.jar entry; patched client should block.",
                },
            ],
            "expected_patched_result": "No file named mods/test.jar is created in the disposable instance.",
        },
    )

    write_text(out_dir / OUTPUT_NAMES["readme"], local_readme())

    print("Generated local-only fixtures:")
    for key in ("clean", "entry", "profile", "manifest", "readme"):
        print(f"  - {out_dir / OUTPUT_NAMES[key]}")
    print()
    print("Use these only with a disposable Minecraft game directory.")
    print("Expected patched result: disposable mods/test.jar does not exist.")
    return 0


def read_template(path: Path) -> tuple[bytes, bytes, bool]:
    if not path.is_file():
        raise SystemExit(f"template does not exist or is not a file: {path}")

    raw = path.read_bytes()
    is_gzip = raw[:2] == b"\x1f\x8b"
    body = gzip.decompress(raw) if is_gzip else raw

    if len(body) < 4:
        raise SystemExit("template is too small to be a Meteor profile NBT file")
    if body[0] != 0x0A:
        raise SystemExit("template root is not a TAG_Compound")
    if body[-1:] != b"\x00":
        raise SystemExit("template root compound does not end with TAG_End")

    return raw, body, is_gzip


def replace_profile_name(inner: bytes, new_name: str) -> bytes:
    marker = b"\x08\x00\x04name"
    index = inner.find(marker)
    if index < 0:
        raise SystemExit("template does not contain a top-level name string")

    value_offset = index + len(marker)
    old_length = struct.unpack_from(">H", inner, value_offset)[0]
    old_end = value_offset + 2 + old_length
    encoded = new_name.encode("utf-8")

    return (
        inner[:value_offset]
        + struct.pack(">H", len(encoded))
        + encoded
        + inner[old_end:]
    )


def nbt_byte_array(key: bytes, payload: bytes) -> bytes:
    return (
        b"\x07"
        + struct.pack(">H", len(key))
        + key
        + struct.pack(">i", len(payload))
        + payload
    )


def overlong_modified_utf8(value: str) -> bytes:
    encoded = bytearray()
    for char in value:
        code_point = ord(char)
        if code_point > 0x7F:
            raise SystemExit("fixture path must stay ASCII-only")
        encoded += bytes(
            [
                0xE0,
                0x80 | (code_point >> 6),
                0x80 | (code_point & 0x3F),
            ]
        )
    return bytes(encoded)


def make_minimal_empty_jar() -> bytes:
    data = BytesIO()
    with zipfile.ZipFile(data, "w", compression=zipfile.ZIP_STORED) as jar:
        info = zipfile.ZipInfo("META-INF/MANIFEST.MF", (1980, 1, 1, 0, 0, 0))
        info.compress_type = zipfile.ZIP_STORED
        jar.writestr(
            info,
            b"Manifest-Version: 1.0\r\n"
            b"Created-By: Meteor Universal Profile Security Patch local fixture\r\n"
            b"\r\n",
        )
    return data.getvalue()


def zip_shift(data: bytes, delta: int) -> bytes:
    archive = bytearray(data)
    eocd_position = archive.rfind(b"PK\x05\x06")
    if eocd_position < 0:
        raise SystemExit("generated payload is not a valid ZIP/JAR")

    central_directory_position = struct.unpack_from("<I", archive, eocd_position + 16)[0]
    struct.pack_into(
        "<I",
        archive,
        eocd_position + 16,
        central_directory_position + delta,
    )

    position = central_directory_position
    while archive[position : position + 4] == b"PK\x01\x02":
        local_header_offset = struct.unpack_from("<I", archive, position + 42)[0]
        struct.pack_into("<I", archive, position + 42, local_header_offset + delta)
        name_length, extra_length, comment_length = struct.unpack_from(
            "<HHH",
            archive,
            position + 28,
        )
        position += 46 + name_length + extra_length + comment_length

    return bytes(archive)


def write_fixture(path: Path, body: bytes, is_gzip: bool) -> None:
    write_bytes(path, gzip.compress(body) if is_gzip else body)


def write_bytes(path: Path, data: bytes) -> None:
    path.write_bytes(data)


def write_text(path: Path, value: str) -> None:
    path.write_text(value, encoding="utf-8", newline="\n")


def write_json(path: Path, value: object) -> None:
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8", newline="\n")


def local_readme() -> str:
    return f"""# Local Meteor profile security fixtures

These files are intentionally generated under an ignored local directory.
Do not import them into your real Minecraft/Meteor profile.

Use only with a disposable Minecraft game directory.

Files:
- {OUTPUT_NAMES["clean"]}: clean control import.
- {OUTPUT_NAMES["entry"]}: traversal-key regression fixture.
- {OUTPUT_NAMES["profile"]}: unsafe-profile-name regression fixture.

Expected patched result:
- The clean control imports successfully.
- The blocked fixtures fail or are rejected.
- The disposable instance must not contain mods/test.jar after blocked imports.
"""


if __name__ == "__main__":
    sys.exit(main())
