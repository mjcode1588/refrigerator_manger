#!/usr/bin/env python3
"""Upload an Android APK (or other file) to Naver MYBOX using its Open API."""

from __future__ import annotations

import argparse
import http.client
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path
from typing import Any

MYBOX_API_BASE = "https://open-api.mybox.naver.com"
DEFAULT_FOLDER_NAME = "FridgeManager-APK"
CHUNK_SIZE = 1024 * 1024  # 1 MiB


class MyboxError(RuntimeError):
    """Raised when Naver MYBOX API validation or upload fails."""


def _sanitize_url_for_log(url: str) -> str:
    """Strip sensitive query parameters (such as stoken) from a URL for logging."""
    parsed = urllib.parse.urlsplit(url)
    return f"{parsed.scheme}://{parsed.netloc}{parsed.path}"


def _json_request(
    method: str,
    url: str,
    token: str,
    payload: dict[str, Any] | None = None,
    timeout: int = 30,
) -> dict[str, Any]:
    """Perform an authenticated JSON request against the Naver MYBOX API."""
    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/json",
    }
    data: bytes | None = None
    if payload is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(payload).encode("utf-8")

    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            status = getattr(response, "status", 200)
            raw = response.read().decode("utf-8")
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace") if exc.fp else ""
        raise MyboxError(
            f"MYBOX API {method} {_sanitize_url_for_log(url)} failed with HTTP {exc.code}: {body}"
        ) from exc
    except urllib.error.URLError as exc:
        raise MyboxError(
            f"MYBOX API {method} {_sanitize_url_for_log(url)} connection error: {exc.reason}"
        ) from exc

    if status not in (200, 201):
        raise MyboxError(
            f"MYBOX API {method} {_sanitize_url_for_log(url)} returned unexpected status {status}"
        )

    if not raw.strip():
        return {}
    return json.loads(raw)


def find_root_folder(token: str, folder_name: str, api_base: str = MYBOX_API_BASE) -> str | None:
    """Search root MYBOX resources for a folder with the given name and return its resourceId."""
    cursor: str | None = None
    visited_cursors: set[str] = set()

    while True:
        params: dict[str, str | int] = {"count": 100}
        if cursor:
            params["cursor"] = cursor
        query = urllib.parse.urlencode(params)
        url = f"{api_base.rstrip('/')}/v1/drive/resources?{query}"

        data = _json_request("GET", url, token=token)
        for resource in data.get("resources", []):
            if resource.get("type") == "folder" and resource.get("name") == folder_name:
                resource_id = resource.get("resourceId")
                if resource_id:
                    return str(resource_id)

        meta = data.get("responseMetaData") or {}
        next_cursor = meta.get("nextCursor")
        if not next_cursor or next_cursor in visited_cursors:
            return None
        visited_cursors.add(next_cursor)
        cursor = str(next_cursor)


def create_folder(
    token: str,
    folder_name: str,
    parent_id: str | None = None,
    api_base: str = MYBOX_API_BASE,
) -> str:
    """Create a folder in Naver MYBOX and return its resourceId."""
    url = f"{api_base.rstrip('/')}/v1/drive/folders"
    payload: dict[str, Any] = {"folderName": folder_name}
    if parent_id:
        payload["parentId"] = parent_id

    data = _json_request("POST", url, token=token, payload=payload)
    resource_id = data.get("resourceId")
    if not resource_id:
        raise MyboxError("MYBOX folder creation response did not include 'resourceId'")
    return str(resource_id)


def ensure_folder(
    token: str,
    folder_name: str,
    api_base: str = MYBOX_API_BASE,
) -> str:
    """Return existing root folder ID or create it if missing."""
    existing_id = find_root_folder(token=token, folder_name=folder_name, api_base=api_base)
    if existing_id:
        return existing_id
    return create_folder(token=token, folder_name=folder_name, api_base=api_base)


def create_upload_url(
    token: str,
    file_name: str,
    file_size: int,
    parent_id: str | None = None,
    overwrite: bool = True,
    api_base: str = MYBOX_API_BASE,
) -> str:
    """Step 1 of MYBOX upload: request a one-time uploadUrl."""
    url = f"{api_base.rstrip('/')}/v1/drive/files"
    payload: dict[str, Any] = {
        "fileName": file_name,
        "fileSize": file_size,
        "isOverwrite": overwrite,
    }
    if parent_id:
        payload["parentId"] = parent_id

    data = _json_request("POST", url, token=token, payload=payload)
    upload_url = data.get("uploadUrl")
    if not upload_url:
        raise MyboxError("MYBOX upload initialization response did not include 'uploadUrl'")
    return str(upload_url)


def upload_multipart_file(
    upload_url: str,
    file_path: Path,
    remote_name: str,
    content_type: str = "application/vnd.android.package-archive",
    timeout: int = 120,
) -> dict[str, Any]:
    """Step 2 of MYBOX upload: stream file as multipart/form-data field 'Filedata'."""
    parsed = urllib.parse.urlsplit(upload_url)
    if parsed.scheme not in ("https", "http") or not parsed.netloc:
        raise MyboxError("Invalid uploadUrl returned by MYBOX API")

    safe_name = remote_name.replace('"', "_")
    boundary = f"----MyboxUpload{uuid.uuid4().hex}"
    preamble = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="Filedata"; filename="{safe_name}"\r\n'
        f"Content-Type: {content_type}\r\n"
        "\r\n"
    ).encode("utf-8")
    epilogue = f"\r\n--{boundary}--\r\n".encode("utf-8")

    file_size = file_path.stat().st_size
    total_length = len(preamble) + file_size + len(epilogue)

    path_with_query = parsed.path or "/"
    if parsed.query:
        path_with_query = f"{path_with_query}?{parsed.query}"

    conn_cls = (
        http.client.HTTPSConnection if parsed.scheme == "https" else http.client.HTTPConnection
    )
    conn = conn_cls(parsed.netloc, timeout=timeout)
    try:
        conn.putrequest("POST", path_with_query)
        conn.putheader("Content-Type", f"multipart/form-data; boundary={boundary}")
        conn.putheader("Content-Length", str(total_length))
        conn.endheaders()

        conn.send(preamble)
        with file_path.open("rb") as fh:
            while True:
                chunk = fh.read(CHUNK_SIZE)
                if not chunk:
                    break
                conn.send(chunk)
        conn.send(epilogue)

        response = conn.getresponse()
        body = response.read().decode("utf-8", errors="replace")
        if response.status not in (200, 201):
            raise MyboxError(
                f"MYBOX file transfer to {_sanitize_url_for_log(upload_url)} "
                f"failed with HTTP {response.status}: {body}"
            )

        if body.strip():
            try:
                return json.loads(body)
            except json.JSONDecodeError:
                return {"raw": body}
        return {}
    except OSError as exc:
        raise MyboxError(
            f"MYBOX file transfer to {_sanitize_url_for_log(upload_url)} failed: {exc}"
        ) from exc
    finally:
        conn.close()


def upload_file(
    token: str,
    file_path: Path,
    remote_names: list[str],
    folder_id: str | None = None,
    folder_name: str | None = DEFAULT_FOLDER_NAME,
    overwrite: bool = True,
    api_base: str = MYBOX_API_BASE,
) -> dict[str, Any]:
    """Resolve target folder (if needed) and upload file under each requested remote name."""
    if not token or not token.strip():
        raise MyboxError("NAVER_MY_BOX token is required")
    token = token.strip()

    if not file_path.is_file():
        raise MyboxError(f"Local file does not exist: {file_path}")

    file_size = file_path.stat().st_size
    if file_size <= 0:
        raise MyboxError(f"Local file is empty: {file_path}")

    target_folder_id = folder_id.strip() if folder_id and folder_id.strip() else None
    if not target_folder_id and folder_name and folder_name.strip():
        target_folder_id = ensure_folder(
            token=token,
            folder_name=folder_name.strip(),
            api_base=api_base,
        )

    uploaded: list[dict[str, Any]] = []
    for name in remote_names:
        upload_url = create_upload_url(
            token=token,
            file_name=name,
            file_size=file_size,
            parent_id=target_folder_id,
            overwrite=overwrite,
            api_base=api_base,
        )
        result = upload_multipart_file(
            upload_url=upload_url,
            file_path=file_path,
            remote_name=name,
        )
        uploaded.append({"remoteName": name, "response": result})

    return {
        "folderId": target_folder_id,
        "fileSize": file_size,
        "uploaded": uploaded,
    }


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Upload APK to Naver MYBOX via Open API")
    parser.add_argument("--file", required=True, help="Path to local APK file")
    parser.add_argument(
        "--remote-name",
        default=None,
        help="Remote file name in MYBOX (defaults to basename of --file)",
    )
    parser.add_argument(
        "--also-upload-as",
        action="append",
        default=[],
        help="Additional remote file name(s) to upload the same APK under",
    )
    parser.add_argument(
        "--folder-id",
        default=os.environ.get("NAVER_MYBOX_FOLDER_ID"),
        help="Target MYBOX folder resourceId (overrides --folder-name)",
    )
    parser.add_argument(
        "--folder-name",
        default=os.environ.get("NAVER_MYBOX_FOLDER_NAME") or DEFAULT_FOLDER_NAME,
        help=f"Target root MYBOX folder name (default: {DEFAULT_FOLDER_NAME})",
    )
    parser.add_argument(
        "--token",
        default=os.environ.get("NAVER_MY_BOX"),
        help="Naver MYBOX Personal Access Token (defaults to NAVER_MY_BOX env var)",
    )
    parser.add_argument(
        "--no-overwrite",
        action="store_true",
        help="Do not overwrite if a file with the same name already exists",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    file_path = Path(args.file)
    primary_name = args.remote_name or file_path.name

    remote_names: list[str] = [primary_name]
    for extra in args.also_upload_as:
        if extra and extra not in remote_names:
            remote_names.append(extra)

    try:
        summary = upload_file(
            token=args.token or "",
            file_path=file_path,
            remote_names=remote_names,
            folder_id=args.folder_id,
            folder_name=args.folder_name,
            overwrite=not args.no_overwrite,
        )
    except MyboxError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1

    print(
        f"Uploaded {len(summary['uploaded'])} file(s) ({summary['fileSize']} bytes) "
        f"to MYBOX folderId={summary['folderId'] or 'root'}: "
        + ", ".join(item["remoteName"] for item in summary["uploaded"])
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
