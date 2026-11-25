"""Unit tests for scripts/upload_to_mybox.py."""

from __future__ import annotations

import io
import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import upload_to_mybox  # noqa: E402


class _FakeUrlopenResponse:
    def __init__(self, payload: dict, status: int = 200) -> None:
        self._body = json.dumps(payload).encode("utf-8")
        self.status = status

    def read(self) -> bytes:
        return self._body

    def __enter__(self) -> "_FakeUrlopenResponse":
        return self

    def __exit__(self, exc_type, exc, tb) -> None:
        return None


class _FakeHttpResponse:
    def __init__(self, payload: dict, status: int = 201) -> None:
        self._body = json.dumps(payload).encode("utf-8")
        self.status = status

    def read(self) -> bytes:
        return self._body


class UploadToMyboxTests(unittest.TestCase):
    def test_find_root_folder_supports_pagination(self) -> None:
        calls: list[str] = []

        def fake_urlopen(request, timeout=30):
            calls.append(request.full_url)
            self.assertEqual(request.headers.get("Authorization"), "Bearer mbx_pat_test")
            if "cursor=page2" not in request.full_url:
                return _FakeUrlopenResponse(
                    {
                        "resources": [
                            {"resourceId": "f1", "name": "Other", "type": "folder"},
                        ],
                        "responseMetaData": {"nextCursor": "page2"},
                    }
                )
            return _FakeUrlopenResponse(
                {
                    "resources": [
                        {"resourceId": "target-123", "name": "FridgeManager-APK", "type": "folder"},
                    ],
                    "responseMetaData": {},
                }
            )

        with mock.patch("urllib.request.urlopen", side_effect=fake_urlopen):
            folder_id = upload_to_mybox.find_root_folder("mbx_pat_test", "FridgeManager-APK")

        self.assertEqual(folder_id, "target-123")
        self.assertEqual(len(calls), 2)

    def test_ensure_folder_creates_when_missing(self) -> None:
        seen_payloads: list[tuple[str, str, dict | None]] = []

        def fake_urlopen(request, timeout=30):
            body = json.loads(request.data.decode("utf-8")) if request.data else None
            seen_payloads.append((request.get_method(), request.full_url, body))
            if request.get_method() == "GET":
                return _FakeUrlopenResponse({"resources": [], "responseMetaData": {}})
            return _FakeUrlopenResponse({"resourceId": "created-999", "name": "FridgeManager-APK"}, 201)

        with mock.patch("urllib.request.urlopen", side_effect=fake_urlopen):
            folder_id = upload_to_mybox.ensure_folder("mbx_pat_test", "FridgeManager-APK")

        self.assertEqual(folder_id, "created-999")
        self.assertEqual(seen_payloads[1][0], "POST")
        self.assertTrue(seen_payloads[1][1].endswith("/v1/drive/folders"))
        self.assertEqual(seen_payloads[1][2], {"folderName": "FridgeManager-APK"})

    def test_two_step_upload_sends_metadata_and_multipart_filedata(self) -> None:
        recorded_requests: list[tuple[str, str, dict | None]] = []
        sent_multipart = io.BytesIO()
        sent_headers: dict[str, str] = {}

        def fake_urlopen(request, timeout=30):
            body = json.loads(request.data.decode("utf-8")) if request.data else None
            recorded_requests.append((request.get_method(), request.full_url, body))
            if request.get_method() == "GET":
                return _FakeUrlopenResponse(
                    {
                        "resources": [
                            {"resourceId": "apk-folder-1", "name": "FridgeManager-APK", "type": "folder"}
                        ],
                        "responseMetaData": {},
                    }
                )
            if request.full_url.endswith("/v1/drive/files"):
                return _FakeUrlopenResponse(
                    {
                        "offset": 0,
                        "uploadUrl": "https://upload.example.com/v1/storage/upload?auth=4&stoken=secret",
                    },
                    status=201,
                )
            raise AssertionError(f"Unexpected URL: {request.full_url}")

        class FakeHTTPSConnection:
            def __init__(self, host: str, timeout: int = 120) -> None:
                self.host = host

            def putrequest(self, method: str, path: str) -> None:
                sent_headers["method"] = method
                sent_headers["path"] = path

            def putheader(self, key: str, value: str) -> None:
                sent_headers[key] = value

            def endheaders(self) -> None:
                return None

            def send(self, data: bytes) -> None:
                sent_multipart.write(data)

            def getresponse(self) -> _FakeHttpResponse:
                return _FakeHttpResponse({"resourceId": "file-abc"}, status=201)

            def close(self) -> None:
                return None

        with tempfile.TemporaryDirectory() as tmpdir:
            apk_path = Path(tmpdir) / "app-release.apk"
            apk_bytes = b"PK\x03\x04dummy-apk-payload"
            apk_path.write_bytes(apk_bytes)

            with (
                mock.patch("urllib.request.urlopen", side_effect=fake_urlopen),
                mock.patch("http.client.HTTPSConnection", FakeHTTPSConnection),
            ):
                summary = upload_to_mybox.upload_file(
                    token="mbx_pat_test",
                    file_path=apk_path,
                    remote_names=["fridge-manager-v0.1.0-b1.apk"],
                )

        self.assertEqual(summary["folderId"], "apk-folder-1")
        self.assertEqual(summary["fileSize"], len(apk_bytes))
        self.assertEqual(
            recorded_requests[1][2],
            {
                "fileName": "fridge-manager-v0.1.0-b1.apk",
                "fileSize": len(apk_bytes),
                "isOverwrite": True,
                "parentId": "apk-folder-1",
            },
        )
        self.assertEqual(sent_headers["method"], "POST")
        self.assertEqual(sent_headers["path"], "/v1/storage/upload?auth=4&stoken=secret")
        self.assertTrue(sent_headers["Content-Type"].startswith("multipart/form-data; boundary="))
        body = sent_multipart.getvalue()
        self.assertEqual(int(sent_headers["Content-Length"]), len(body))
        self.assertIn(b'name="Filedata"; filename="fridge-manager-v0.1.0-b1.apk"', body)
        self.assertIn(apk_bytes, body)

    def test_upload_file_validates_token_and_missing_file(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            missing = Path(tmpdir) / "missing.apk"
            with self.assertRaises(upload_to_mybox.MyboxError):
                upload_to_mybox.upload_file(
                    token="",
                    file_path=missing,
                    remote_names=["app.apk"],
                )
            with self.assertRaises(upload_to_mybox.MyboxError):
                upload_to_mybox.upload_file(
                    token="mbx_pat_test",
                    file_path=missing,
                    remote_names=["app.apk"],
                )


if __name__ == "__main__":
    unittest.main()
