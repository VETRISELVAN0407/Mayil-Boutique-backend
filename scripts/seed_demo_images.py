"""Attach pictures from backend/scripts/demo-images to the demo products (dev only).

    python backend/scripts/seed_demo_images.py

Run seed_demo_data.py first. Product `demo-<category>-NN` gets `demo-images/<category>/NN.<ext>` as its cover.
Products that already have an image are skipped, so re-running is safe. Same env/.env settings as seed_demo_data.py.
Photo credits (Wikimedia Commons) are in demo-images/CREDITS.md.
"""
import json
import os
import urllib.error
import urllib.request
import uuid
from pathlib import Path

from seed_demo_data import API, call, load_env, login

IMAGES = Path(__file__).resolve().parent / "demo-images"
TYPES = {"jpg": "image/jpeg", "png": "image/png", "webp": "image/webp"}


def upload(product_id, path, token):
    boundary = uuid.uuid4().hex
    ctype = TYPES[path.suffix.lstrip(".").lower()]
    body = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"images\"; filename=\"{path.name}\"\r\n"
            f"Content-Type: {ctype}\r\n\r\n").encode() + path.read_bytes() + f"\r\n--{boundary}--\r\n".encode()
    req = urllib.request.Request(f"{API}/admin/products/{product_id}/images", data=body, method="POST")
    req.add_header("Content-Type", f"multipart/form-data; boundary={boundary}")
    req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"upload for product {product_id} -> {e.code} {e.read().decode()[:200]}") from None


def main():
    load_env()
    token = login()
    done = skipped = missing = 0
    for p in call("GET", "/admin/products", token=token):
        slug = p["slug"]
        if not slug.startswith("demo-"):
            continue
        if p.get("images"):
            skipped += 1
            continue
        category, _, number = slug[len("demo-"):].rpartition("-")
        files = sorted((IMAGES / category).glob(f"{number}.*"))
        if not files:
            missing += 1
            print("  no image for", slug)
            continue
        upload(p["id"], files[0], token)
        done += 1
    print(f"Uploaded {done}, already had images {skipped}, no image file {missing}.")


if __name__ == "__main__":
    main()
