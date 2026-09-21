#!/usr/bin/env python3
"""Optional CacaMusicPlayer sync + catalog server.

Local music playback does NOT need this process.
Run only if you want account sync or to host your own freely licensed catalog.

  python3 server.py

Default: http://127.0.0.1:8088
  GET  /catalog.json     sample catalog
  POST /api/register     {"username","password","displayName"}
  POST /api/login        {"username","password"}
  POST /api/sync         {"username","displayName","favorites","playlists"}
"""
from __future__ import print_function

import json
import os
import sqlite3
import hashlib
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer

ROOT = os.path.dirname(os.path.abspath(__file__))
DB = os.path.join(ROOT, "data", "caca_backend.db")
PORT = int(os.environ.get("CACA_PORT", "8088"))
LOCK = threading.Lock()


def db():
    os.makedirs(os.path.dirname(DB), exist_ok=True)
    con = sqlite3.connect(DB)
    con.execute(
        "CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password_hash TEXT, display_name TEXT, payload TEXT)"
    )
    con.commit()
    return con


def hash_pw(password):
    return hashlib.sha256(("caca:" + (password or "")).encode("utf-8")).hexdigest()


class Handler(BaseHTTPRequestHandler):
    def _send(self, code, obj):
        data = json.dumps(obj).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Access-Control-Allow-Methods", "GET,POST,OPTIONS")
        self.end_headers()

    def do_GET(self):
        if self.path.startswith("/catalog"):
            path = os.path.join(ROOT, "sample-catalog.json")
            with open(path, "rb") as f:
                data = f.read()
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)
            return
        if self.path in ("/", "/health"):
            self._send(200, {"ok": True, "app": "CacaMusicPlayer"})
            return
        self._send(404, {"error": "not found"})

    def do_POST(self):
        length = int(self.headers.get("Content-Length", "0") or 0)
        raw = self.rfile.read(length) if length else b"{}"
        try:
            body = json.loads(raw.decode("utf-8") or "{}")
        except Exception:
            self._send(400, {"error": "invalid json"})
            return
        with LOCK:
            con = db()
            try:
                if self.path == "/api/register":
                    user = (body.get("username") or "").strip()
                    pw = body.get("password") or ""
                    if not user or not pw:
                        self._send(400, {"error": "missing fields"})
                        return
                    try:
                        con.execute(
                            "INSERT INTO users(username,password_hash,display_name,payload) VALUES(?,?,?,?)",
                            (user, hash_pw(pw), body.get("displayName") or user, "{}"),
                        )
                        con.commit()
                    except sqlite3.IntegrityError:
                        self._send(409, {"error": "username taken"})
                        return
                    self._send(200, {"ok": True, "username": user})
                    return
                if self.path == "/api/login":
                    user = (body.get("username") or "").strip()
                    row = con.execute(
                        "SELECT password_hash, display_name FROM users WHERE username=?", (user,)
                    ).fetchone()
                    if not row or row[0] != hash_pw(body.get("password") or ""):
                        self._send(401, {"error": "login failed"})
                        return
                    self._send(200, {"ok": True, "username": user, "displayName": row[1]})
                    return
                if self.path == "/api/sync":
                    user = (body.get("username") or "").strip()
                    row = con.execute("SELECT username FROM users WHERE username=?", (user,)).fetchone()
                    if not row:
                        con.execute(
                            "INSERT INTO users(username,password_hash,display_name,payload) VALUES(?,?,?,?)",
                            (user, "", body.get("displayName") or user, json.dumps(body)),
                        )
                    else:
                        con.execute(
                            "UPDATE users SET display_name=?, payload=? WHERE username=?",
                            (body.get("displayName") or user, json.dumps(body), user),
                        )
                    con.commit()
                    self._send(200, {"ok": True})
                    return
            finally:
                con.close()
        self._send(404, {"error": "not found"})

    def log_message(self, fmt, *args):
        print("[caca-backend]", fmt % args)


if __name__ == "__main__":
    db()
    httpd = HTTPServer(("0.0.0.0", PORT), Handler)
    print("CacaMusicPlayer backend http://127.0.0.1:%s" % PORT)
    httpd.serve_forever()
