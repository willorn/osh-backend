#!/usr/bin/env python3
"""Patch built artifacts for 149 deploy: replace 25 test host refs with 149 slot targets."""
import argparse
import glob
import os
import re
import shutil
import sys
import tempfile
import zipfile

SRC_HOST = "43.242.200.25"

SLOTS = {
    "green": {
        "host": "149.88.92.159",
        "nginx_port": "28080",
        "backend_port": "28081",
        "nacos_addr": "osh-g-nacos:8848",
        "secret_url": "http://osh-g-secret-manager:59100",
    },
    "blue": {
        "host": "149.88.92.159",
        "nginx_port": "58080",
        "backend_port": "58081",
        "nacos_addr": "osh-nacos:8848",
        "secret_url": "http://osh-secret-manager:59100",
    },
}


def patch_frontend_text(text, cfg):
    src = SRC_HOST
    dst = cfg["host"]
    nginx_p = cfg["nginx_port"]
    backend_p = cfg["backend_port"]

    # HTTP API → same-origin /pc (via nginx)
    text = re.sub(rf"https?://{re.escape(src)}:8081/pc", "/pc", text)
    text = re.sub(rf"https?://{re.escape(dst)}:{backend_p}/pc", "/pc", text)
    text = re.sub(rf"https?://{re.escape(src)}:8081(?=[/\"'\s])", "/pc", text)
    text = re.sub(rf"https?://{re.escape(dst)}:{backend_p}(?=[/\"'\s])", "/pc", text)
    text = re.sub(r"https?://localhost:8081/pc", "/pc", text)
    text = re.sub(r"https?://localhost:8081(?=[/\"'\s])", "/pc", text)

    # WebSocket → nginx port on 149
    ws_dst = f"ws://{dst}:{nginx_p}"
    text = re.sub(rf"wss?://{re.escape(src)}:8081", ws_dst, text)
    text = re.sub(rf"wss?://{re.escape(dst)}:{backend_p}", ws_dst, text)
    text = re.sub(rf"'{re.escape(src)}:8081'", "'/pc'", text)
    text = re.sub(rf'"{re.escape(src)}:8081"', '"/pc"', text)
    text = text.replace(f"{src}:8081", "/pc")
    text = text.replace(src, dst)
    return text


def patch_bootstrap_text(text, cfg):
    text = re.sub(
        rf"server-addr:\s*{re.escape(SRC_HOST)}:\d+",
        f"server-addr: {cfg['nacos_addr']}",
        text,
    )
    text = text.replace(f"{SRC_HOST}:58848", cfg["nacos_addr"])
    text = text.replace(f"{SRC_HOST}:8848", cfg["nacos_addr"])
    text = re.sub(
        rf"https?://{re.escape(SRC_HOST)}:59100",
        cfg["secret_url"],
        text,
    )
    text = text.replace(SRC_HOST, cfg["host"])
    return text


def patch_frontend_dir(root, slot):
    cfg = SLOTS[slot]
    changed = 0
    for pattern in ("**/*.js", "**/*.html", "**/*.json"):
        for path in glob.glob(os.path.join(root, pattern), recursive=True):
            if "/node_modules/" in path:
                continue
            try:
                raw = open(path, encoding="utf-8", errors="ignore").read()
            except OSError:
                continue
            new = patch_frontend_text(raw, cfg)
            if new != raw:
                open(path, "w", encoding="utf-8").write(new)
                changed += 1
    return changed


def patch_jar(path, slot):
    cfg = SLOTS[slot]
    entry = "BOOT-INF/classes/bootstrap.yml"
    with zipfile.ZipFile(path, "r") as zf:
        if entry not in zf.namelist():
            raise SystemExit(f"missing {entry} in {path}")
        raw = zf.read(entry).decode("utf-8")

    new = patch_bootstrap_text(raw, cfg)
    if new == raw:
        print("jar bootstrap.yml unchanged")
        return False

    tmp_path = tempfile.mktemp(suffix=".jar")
    try:
        with zipfile.ZipFile(path, "r") as zin, zipfile.ZipFile(
            tmp_path, "w", compression=zipfile.ZIP_DEFLATED
        ) as zout:
            for item in zin.infolist():
                data = zin.read(item.filename)
                if item.filename == entry:
                    data = new.encode("utf-8")
                zout.writestr(item, data)
        shutil.move(tmp_path, path)
    except Exception:
        if os.path.exists(tmp_path):
            os.remove(tmp_path)
        raise
    return True


def verify_no_src_host(target, kind):
    if kind == "frontend":
        files = glob.glob(os.path.join(target, "**/*.js"), recursive=True)
    else:
        files = [target]

    offenders = []
    for path in files:
        try:
            if kind == "jar":
                with zipfile.ZipFile(path, "r") as zf:
                    data = zf.read("BOOT-INF/classes/bootstrap.yml").decode("utf-8", errors="ignore")
            else:
                data = open(path, encoding="utf-8", errors="ignore").read()
        except OSError:
            continue
        if SRC_HOST in data:
            offenders.append(path)

    if offenders:
        sample = offenders[:5]
        raise SystemExit(f"still contains {SRC_HOST}: {sample}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--slot", choices=SLOTS.keys(), required=True)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--frontend", help="frontend static output directory")
    group.add_argument("--jar", help="backstage-admin.jar path")
    args = parser.parse_args()

    if args.frontend:
        n = patch_frontend_dir(args.frontend, args.slot)
        verify_no_src_host(args.frontend, "frontend")
        print(f"patched {n} frontend files for slot={args.slot}")
        return

    if patch_jar(args.jar, args.slot):
        print(f"patched jar bootstrap for slot={args.slot}")
    verify_no_src_host(args.jar, "jar")
    print(f"verified jar for slot={args.slot}")


if __name__ == "__main__":
    main()
