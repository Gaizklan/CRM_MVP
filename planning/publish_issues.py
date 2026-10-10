"""Publica labels, milestones, epics e issues de planning/issues.json en GitHub (API REST vía `gh api`).

Idempotente: identifica issues existentes por el marcador <!-- key:XXX --> del cuerpo.
Uso: python3 planning/publish_issues.py owner/repo [--dry-run]
"""
import json
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
DRY = "--dry-run" in sys.argv
REPO = next(a for a in sys.argv[1:] if not a.startswith("--"))


def api(method, path, payload=None, paginate=False):
    cmd = ["gh", "api", "-X", method, f"repos/{REPO}/{path}", "-H", "Accept: application/vnd.github+json"]
    if paginate:
        cmd.append("--paginate")
    if DRY and method != "GET":
        print("DRY", method, path, (payload or {}).get("title") or (payload or {}).get("name", ""))
        return {"number": 0}
    r = subprocess.run(cmd + (["--input", "-"] if payload is not None else []),
                       input=json.dumps(payload) if payload is not None else None,
                       capture_output=True, text=True)
    if r.returncode != 0:
        raise RuntimeError(f"{method} {path}: {r.stderr or r.stdout}")
    out = r.stdout.strip()
    if paginate and out.startswith("[") and "][" in out:
        out = out.replace("]\n[", ",").replace("][", ",")
    return json.loads(out) if out else {}


def main():
    data = json.loads((ROOT / "planning/issues.json").read_text())

    existing_labels = {l["name"] for l in api("GET", "labels?per_page=100", paginate=True)}
    for name, color in data["labels"].items():
        if name not in existing_labels:
            api("POST", "labels", {"name": name, "color": color})

    ms = {m["title"]: m["number"] for m in api("GET", "milestones?state=all&per_page=100", paginate=True)}
    ms_num = {}
    for m in data["milestones"]:
        if m["title"] not in ms:
            ms[m["title"]] = api("POST", "milestones", {"title": m["title"], "description": m["description"]})["number"]
        ms_num[m["key"]] = ms[m["title"]]

    by_key = {}
    for it in api("GET", "issues?state=all&per_page=100", paginate=True):
        mk = re.search(r"<!-- key:(\w+) -->", it.get("body") or "")
        if mk and "pull_request" not in it:
            by_key[mk.group(1)] = it["number"]

    items = [dict(e, milestone=None) for e in data["epics"]] + data["issues"]
    for it in items:
        if it["key"] in by_key:
            continue
        payload = {"title": it["title"], "body": it["body"], "labels": it["labels"]}
        if it["milestone"]:
            payload["milestone"] = ms_num[it["milestone"]]
        by_key[it["key"]] = api("POST", "issues", payload)["number"]
        print(f"#{by_key[it['key']]} {it['key']} {it['title']}")

    # Segunda pasada: sustituir {{KEY}} por #número
    for it in items:
        body = re.sub(r"\{\{(\w+)\}\}", lambda m: f"#{by_key[m.group(1)]}", it["body"])
        if body != it["body"]:
            api("PATCH", f"issues/{by_key[it['key']]}", {"body": body})

    (ROOT / "planning/issue-map.json").write_text(json.dumps(by_key, indent=2, sort_keys=True))
    print(f"OK: {len(items)} issues enlazados")


if __name__ == "__main__":
    main()
