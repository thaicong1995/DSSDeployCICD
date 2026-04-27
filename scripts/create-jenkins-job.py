#!/usr/bin/env python3
import base64
import os
import pathlib
import sys
import urllib.error
import urllib.parse
import urllib.request


ROOT = pathlib.Path(__file__).resolve().parent.parent
ENV_PATH = ROOT / ".ci" / "jenkins-job.env"
TEMPLATE_PATH = ROOT / ".ci" / "jenkins-job-config.xml.template"


def load_env(path: pathlib.Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for line in path.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        key, _, value = line.partition("=")
        values[key.strip()] = value.strip()
    return values


def require(cfg: dict[str, str], key: str) -> str:
    value = cfg.get(key, "").strip()
    if not value:
        raise SystemExit(f"Missing required value: {key} in {ENV_PATH}")
    return value


def request(url: str, method: str = "GET", data: bytes | None = None, headers: dict[str, str] | None = None):
    req = urllib.request.Request(url, data=data, method=method)
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    return urllib.request.urlopen(req, timeout=20)


def build_auth_header(username: str, token: str) -> dict[str, str]:
    raw = f"{username}:{token}".encode()
    return {"Authorization": f"Basic {base64.b64encode(raw).decode()}"}


def get_crumb(base_url: str, headers: dict[str, str]) -> tuple[str, str] | None:
    crumb_url = f"{base_url}/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,\":\",//crumb)"
    try:
        with request(crumb_url, headers=headers) as resp:
            text = resp.read().decode().strip()
            if ":" in text:
                field, value = text.split(":", 1)
                return field, value
    except urllib.error.HTTPError as exc:
        if exc.code not in {404, 403}:
            raise
    except Exception:
        return None
    return None


def render_config(template: str, scm_url: str, scm_branch: str, script_path: str) -> str:
    return (
        template.replace("__SCM_URL__", scm_url)
        .replace("__SCM_BRANCH__", scm_branch)
        .replace("__SCRIPT_PATH__", script_path)
    )


def main() -> int:
    if not ENV_PATH.exists():
        raise SystemExit(f"Missing {ENV_PATH}. Copy .ci/jenkins-job.env.example first.")

    cfg = load_env(ENV_PATH)
    jenkins_url = require(cfg, "JENKINS_URL").rstrip("/")
    username = require(cfg, "JENKINS_USERNAME")
    token = require(cfg, "JENKINS_API_TOKEN")
    job_name = require(cfg, "JOB_NAME")
    scm_url = require(cfg, "SCM_URL")
    scm_branch = require(cfg, "SCM_BRANCH")
    script_path = require(cfg, "SCRIPT_PATH")

    xml = render_config(TEMPLATE_PATH.read_text(), scm_url, scm_branch, script_path).encode()
    headers = build_auth_header(username, token)
    crumb = get_crumb(jenkins_url, headers)
    if crumb:
        headers[crumb[0]] = crumb[1]

    encoded_job = urllib.parse.quote(job_name, safe="")
    job_url = f"{jenkins_url}/job/{encoded_job}/config.xml"
    create_url = f"{jenkins_url}/createItem?name={encoded_job}"

    try:
        with request(job_url, headers=headers) as resp:
            exists = resp.status == 200
    except urllib.error.HTTPError as exc:
        if exc.code == 404:
            exists = False
        else:
            raise

    if exists:
        with request(job_url, method="POST", data=xml, headers={**headers, "Content-Type": "application/xml"}) as resp:
            print(f"Updated Jenkins job {job_name}: HTTP {resp.status}")
    else:
        with request(create_url, method="POST", data=xml, headers={**headers, "Content-Type": "application/xml"}) as resp:
            print(f"Created Jenkins job {job_name}: HTTP {resp.status}")

    build_url = f"{jenkins_url}/job/{encoded_job}/build"
    with request(build_url, method="POST", headers=headers) as resp:
        print(f"Triggered build for {job_name}: HTTP {resp.status}")
    print(f"Open: {jenkins_url}/job/{encoded_job}/")
    return 0


if __name__ == "__main__":
    sys.exit(main())
