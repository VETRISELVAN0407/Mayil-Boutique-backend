"""Seed dummy categories, products and variants through the admin API (dev only).

    python backend/scripts/seed_demo_data.py          # add (skips anything that already exists)
    python backend/scripts/seed_demo_data.py --clean  # remove only what this script created

Needs the backend running. Reads ADMIN_MOBILE / ADMIN_PASSWORD from backend/.env (or the environment),
API base from API_URL (default http://localhost:8080/api). Demo products use SKU prefix DEMO- and
slug prefix demo-; demo categories are the slugs listed below. No images are uploaded.
"""
import json
import os
import random
import sys
import urllib.error
import urllib.request
from pathlib import Path

API = os.environ.get("API_URL", "http://localhost:8080/api")

# slug -> (name, price band, ten product names)
CATALOG = {
    "banarasi-silk-sarees": ("Banarasi Silk Sarees", (6500, 14500), [
        "Royal Maroon Banarasi", "Emerald Zari Banarasi", "Midnight Blue Banarasi", "Peacock Brocade Banarasi",
        "Ivory Gold Banarasi", "Rani Pink Banarasi", "Mustard Kadhua Banarasi", "Wine Katan Banarasi",
        "Teal Jaal Banarasi", "Bottle Green Banarasi"]),
    "kanchipuram-silk-sarees": ("Kanchipuram Silk Sarees", (8000, 22000), [
        "Temple Border Kanchipuram", "Mango Yellow Kanchipuram", "Arakku Red Kanchipuram", "Peacock Green Kanchipuram",
        "Rettai Pettu Kanchipuram", "Lavender Zari Kanchipuram", "Magenta Checks Kanchipuram", "Navy Gold Kanchipuram",
        "Pastel Pink Kanchipuram", "Bridal Orange Kanchipuram"]),
    "cotton-sarees": ("Cotton Sarees", (900, 2800), [
        "Handloom Indigo Cotton", "Block Print Cotton", "Kalamkari Cotton", "Checked Mulmul Cotton",
        "Ikat Border Cotton", "Plain Leaf Green Cotton", "Tangail Cotton", "Bandhani Cotton",
        "Zari Border Cotton", "Everyday Striped Cotton"]),
    "georgette-sarees": ("Georgette Sarees", (1500, 4200), [
        "Sequin Party Georgette", "Floral Print Georgette", "Ombre Georgette", "Embroidered Georgette",
        "Pleated Ruffle Georgette", "Shimmer Black Georgette", "Pastel Mint Georgette", "Bandhej Georgette",
        "Lace Border Georgette", "Chikankari Georgette"]),
    "kurta-sets": ("Kurta Sets", (1400, 4500), [
        "Mirror Work Kurta Set", "Anarkali Kurta Set", "Straight Cut Kurta Set", "Chanderi Kurta Set",
        "Printed Palazzo Kurta Set", "Embroidered Sharara Kurta Set", "Cotton Churidar Kurta Set",
        "Festive Silk Kurta Set", "Pastel Linen Kurta Set", "Bandhani Kurta Set"]),
    "lehengas": ("Lehengas", (9000, 28000), [
        "Bridal Red Lehenga", "Pastel Floral Lehenga", "Mirror Work Lehenga", "Velvet Wine Lehenga",
        "Sequin Ivory Lehenga", "Banarasi Silk Lehenga", "Mint Organza Lehenga", "Navy Zardozi Lehenga",
        "Ready To Wear Lehenga", "Half Saree Lehenga"]),
    "salwar-suits": ("Salwar Suits", (1800, 5200), [
        "Punjabi Patiala Suit", "Cotton Straight Suit", "Georgette Palazzo Suit", "Phulkari Suit",
        "Chikankari Suit", "Silk Churidar Suit", "Printed Cotton Suit", "Festive Anarkali Suit",
        "Bandhani Dupatta Suit", "Pastel Embroidered Suit"]),
    "dupattas-stoles": ("Dupattas & Stoles", (450, 2200), [
        "Phulkari Dupatta", "Banarasi Silk Dupatta", "Bandhani Dupatta", "Chiffon Gota Dupatta",
        "Cotton Mulmul Stole", "Pashmina Blend Stole", "Zari Net Dupatta", "Kalamkari Stole",
        "Organza Border Dupatta", "Block Print Stole"]),
    "blouses": ("Blouses", (600, 2400), [
        "Readymade Silk Blouse", "Embroidered Boat Neck Blouse", "Puff Sleeve Blouse", "Zari Brocade Blouse",
        "Velvet Back Knot Blouse", "Cotton Princess Cut Blouse", "Mirror Work Blouse", "Sequin Crop Blouse",
        "Elbow Sleeve Blouse", "Halter Neck Blouse"]),
    "western-wear": ("Western Wear", (800, 3200), [
        "Baggy Denim Jeans", "Floral Midi Dress", "Linen Co-ord Set", "Wrap Maxi Dress",
        "High Waist Trousers", "Boxy Cotton Top", "Pleated Skirt", "Satin Slip Dress",
        "Oversized Shirt", "Ribbed Knit Top"]),
}

COLOURS = ["Maroon", "Emerald", "Navy", "Mustard", "Pink", "Teal", "Ivory", "Black", "Wine", "Green"]
SIZES = [("S", "36"), ("M", "38"), ("L", "40"), ("XL", "42")]


def load_env():
    env = Path(__file__).resolve().parent.parent / ".env"
    if env.exists():
        for line in env.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                os.environ.setdefault(k.strip(), v.strip())


def call(method, path, body=None, token=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(API + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req) as resp:
            raw = resp.read()
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"{method} {path} -> {e.code} {e.read().decode()[:200]}") from None


def login():
    out = call("POST", "/auth/admin/login", {
        "mobile": os.environ.get("ADMIN_MOBILE"), "password": os.environ.get("ADMIN_PASSWORD")})
    return out["accessToken"]


def seed(token):
    random.seed(7)  # same data every run
    cats = {c["slug"]: c for c in call("GET", "/admin/categories", token=token)}
    existing = {p["slug"] for p in call("GET", "/admin/products", token=token)}
    made_c = made_p = 0
    for cslug, (cname, (lo, hi), names) in CATALOG.items():
        cat = cats.get(cslug)
        if not cat:
            cat = call("POST", "/admin/categories", {
                "name": cname, "slug": cslug, "description": f"Demo collection: {cname}."}, token)
            made_c += 1
        for i, pname in enumerate(names, 1):
            slug = f"demo-{cslug}-{i:02d}"
            if slug in existing:
                continue
            price = round(random.randint(lo, hi) / 50) * 50
            sale = round(price * random.choice([0.85, 0.9, 0.8]) / 10) * 10 if i % 3 == 0 else None
            sku = f"DEMO-{cslug[:3].upper()}-{i:02d}"
            product = call("POST", "/admin/products", {
                "name": pname, "slug": slug, "categoryId": cat["id"], "price": price, "salePrice": sale,
                "sku": sku, "description": f"{pname}. Dummy product for testing the storefront."}, token)
            colours = random.sample(COLOURS, 2)
            # sarees are one-size; everything else gets a few sizes per colour
            sizes = [(None, None)] if cslug.endswith("sarees") else SIZES[: random.choice([2, 3, 4])]
            for colour in colours:
                for size, number in sizes:
                    # in-stock mix: every 5th product has one variant sold out, every 10th is fully sold out
                    stock = 0 if i % 10 == 0 else random.choice([0, 3, 8, 15, 25]) if i % 5 == 0 else random.randint(4, 30)
                    vsku = f"{sku}-{colour[:3].upper()}" + (f"-{size}" if size else "")
                    call("POST", f"/admin/products/{product['id']}/variants", {
                        "sku": vsku, "size": size, "sizeNumber": number, "colour": colour,
                        "stockQuantity": stock}, token)
            call("PATCH", f"/admin/products/{product['id']}/status", {"status": "PUBLISHED"}, token)
            made_p += 1
        print(f"  {cname}: done")
    print(f"Created {made_c} categories and {made_p} products.")


def clean(token):
    removed_p = removed_c = 0
    for p in call("GET", "/admin/products", token=token):
        if p["sku"].startswith("DEMO-"):
            try:
                if p["status"] == "PUBLISHED":
                    call("PATCH", f"/admin/products/{p['id']}/status", {"status": "DRAFT"}, token)
                call("DELETE", f"/admin/products/{p['id']}", token=token)
                removed_p += 1
            except RuntimeError as e:
                print("  skipped:", e)
    for c in call("GET", "/admin/categories", token=token):
        if c["slug"] in CATALOG:
            try:
                call("DELETE", f"/admin/categories/{c['id']}", token=token)
                removed_c += 1
            except RuntimeError as e:
                print("  skipped:", e)
    print(f"Removed {removed_p} products and {removed_c} categories.")


if __name__ == "__main__":
    load_env()
    tok = login()
    clean(tok) if "--clean" in sys.argv else seed(tok)
