"""Tests for scripts/auth.py: who may call which endpoint, and logging in.   Run:  python -m pytest -v

Tokens here are signed with the test secret from conftest.py, the way auth-service signs real ones.
auth-service itself isn't running, so call_auth_service is swapped for a fake with monkeypatch.
"""
import time

import jwt
import pandas as pd
import pytest
from fastapi.testclient import TestClient

import api
import auth

client = TestClient(api.app)

ANALYST_URL = "/api/analyst/top-clients?start=2026-01-01&end=2026-02-01"
ADMIN_URL = "/api/admin/orders"


def token(roles, secret=auth.SECRET, expires_in=3600, algorithm="HS256"):
    claims = {"sub": "7", "roles": roles, "exp": int(time.time()) + expires_in}
    return jwt.encode(claims, secret, algorithm=algorithm)


def get(url, bearer=None):
    headers = {"Authorization": f"Bearer {bearer}"} if bearer else {}
    return client.get(url, headers=headers)


@pytest.fixture(autouse=True)
def no_database(monkeypatch):
    # the endpoints these tests reach answer with empty data instead of asking the database
    monkeypatch.setattr(api.tf, "top_clients", lambda engine, filters, top_n: pd.DataFrame())
    monkeypatch.setattr(api.tf, "order_history", lambda *args: (0, pd.DataFrame()))


# --- no valid token: 401 ------------------------------------------------------

@pytest.mark.parametrize("bearer", [
    None,                                                       # not logged in
    "not-a-jwt",
    token(["ADMIN"], secret="someone-elses-secret-32-characters!!"),  # forged
    token(["ADMIN"], expires_in=-60),                           # expired
])
def test_without_a_valid_token_returns_401(bearer):
    response = get(ANALYST_URL, bearer)
    assert response.status_code == 401
    assert response.headers["WWW-Authenticate"] == "Bearer"


def test_a_token_using_another_algorithm_is_rejected():
    unsigned = jwt.encode({"sub": "7", "roles": ["ADMIN"], "exp": int(time.time()) + 60}, None, algorithm="none")
    assert get(ANALYST_URL, unsigned).status_code == 401


def test_every_api_endpoint_needs_a_token():
    # catches a new endpoint added without dependencies=[...]
    open_paths = {"/api/auth/login", "/api/auth/refresh"}
    for route in api.app.routes:
        path = getattr(route, "path", "")
        if path.startswith("/api/") and path not in open_paths:
            url = path.replace("{trade_id}", "1")
            assert get(url).status_code == 401, path


# --- roles: 200 or 403 ----------------------------------------------------------

@pytest.mark.parametrize("roles, analyst, admin", [
    (["ANALYST"], 200, 403),
    (["ADMIN"], 200, 200),
    (["CLIENT"], 403, 403),
    ([], 403, 403),
    (["CLIENT", "ANALYST"], 200, 403),
])
def test_roles_decide_which_pages_a_user_can_use(roles, analyst, admin):
    bearer = token(roles)
    assert get(ANALYST_URL, bearer).status_code == analyst
    assert get(ADMIN_URL, bearer).status_code == admin


# --- logging in through auth-service ------------------------------------------

def fake_auth_service(monkeypatch, status, tokens):
    calls = []

    def fake(path, body):
        calls.append((path, body))
        return status, tokens

    monkeypatch.setattr(api, "call_auth_service", fake)
    return calls


def test_login_passes_the_password_to_auth_service_and_returns_the_tokens(monkeypatch):
    access = token(["ANALYST"])
    calls = fake_auth_service(monkeypatch, 200, {"accessToken": access, "refreshToken": "r1"})

    response = client.post("/api/auth/login", json={"username": "analyst", "password": "analyst123"})

    assert response.status_code == 200
    assert response.json() == {"accessToken": access, "refreshToken": "r1", "roles": ["ANALYST"]}
    assert calls == [("/auth/login", {"username": "analyst", "password": "analyst123"})]


def test_wrong_password_returns_401(monkeypatch):
    fake_auth_service(monkeypatch, 401, None)
    response = client.post("/api/auth/login", json={"username": "analyst", "password": "nope"})
    assert response.status_code == 401
    assert response.json()["detail"] == "Wrong username or password"


def test_a_client_login_cannot_use_the_dashboard(monkeypatch):
    fake_auth_service(monkeypatch, 200, {"accessToken": token(["CLIENT"]), "refreshToken": "r1"})
    response = client.post("/api/auth/login", json={"username": "efe", "password": "secret"})
    assert response.status_code == 403


def test_a_token_signed_with_another_secret_is_a_server_problem(monkeypatch):
    # auth-service and the dashboard were given different JWT_SHARED_SECRETs
    other = token(["ADMIN"], secret="someone-elses-secret-32-characters!!")
    fake_auth_service(monkeypatch, 200, {"accessToken": other, "refreshToken": "r1"})
    response = client.post("/api/auth/login", json={"username": "admin", "password": "admin123"})
    assert response.status_code == 502


def test_refresh_swaps_the_refresh_token(monkeypatch):
    access = token(["ADMIN"])
    calls = fake_auth_service(monkeypatch, 200, {"accessToken": access, "refreshToken": "r2"})

    response = client.post("/api/auth/refresh", json={"refreshToken": "r1"})

    assert response.status_code == 200
    assert response.json()["refreshToken"] == "r2"
    assert calls == [("/auth/refresh", {"refreshToken": "r1"})]


def test_an_expired_refresh_token_returns_401(monkeypatch):
    fake_auth_service(monkeypatch, 401, None)
    response = client.post("/api/auth/refresh", json={"refreshToken": "old"})
    assert response.status_code == 401
