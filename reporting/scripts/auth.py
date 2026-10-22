"""Who may use the dashboard: checks the login token that auth-service gives out.

How it works
------------
Users log in to auth-service (through POST /api/auth/login below). It answers with an
"access token": a JWT that says which user this is and their roles, signed with
JWT_SHARED_SECRET. The browser sends it back on every request as
    Authorization: Bearer <token>
and we check the signature with the same secret, so nobody can make up their own token.
The token lasts 15 minutes; the browser then swaps its refresh token for a new pair.

- No token, a forged token or an expired one: 401 (log in again).
- A real token, but without a role the page needs: 403.

Roles come from the roles table. Only ANALYST and ADMIN logins can use the dashboard,
and auth-service's public sign-up always gives CLIENT, so those two only come from the seed data.
"""
import json
import os
import urllib.error
import urllib.request

import jwt
from fastapi import Depends, Header, HTTPException

STAFF_ROLES = ("ANALYST", "ADMIN")

# Read when this file is imported (after database_connection.py has loaded .env), so a missing secret
# stops the server at start-up instead of failing on the first request.
SECRET = os.environ["JWT_SHARED_SECRET"]


def read_token(token):
    """The token's claims, e.g. {"sub": "42", "roles": ["ANALYST"], "exp": ...}, or None if it isn't valid."""
    try:
        # only HS256, the algorithm auth-service signs with; never trust the algorithm the token names
        return jwt.decode(token, SECRET, algorithms=["HS256"], options={"require": ["sub", "exp"]})
    except jwt.InvalidTokenError:
        return None


def require_role(*allowed):
    """A FastAPI dependency that lets a request through only if its token has one of the `allowed` roles.

    Used as `dependencies=[ANALYST_OR_ADMIN]` on an endpoint, so it runs before the endpoint does.
    """
    def check(authorization: str | None = Header(default=None)):
        scheme, _, token = (authorization or "").partition(" ")
        claims = read_token(token) if scheme.lower() == "bearer" and token else None
        if claims is None:
            raise HTTPException(401, "Log in to use the dashboard", headers={"WWW-Authenticate": "Bearer"})
        if not set(claims.get("roles") or []) & set(allowed):
            raise HTTPException(403, "Your login doesn't have access to this page")
        return claims

    return check


ANALYST_OR_ADMIN = Depends(require_role("ANALYST", "ADMIN"))
ADMIN = Depends(require_role("ADMIN"))


def call_auth_service(path, body):
    """POST `body` as JSON to auth-service and return (HTTP status, JSON answer).

    The dashboard passes login and refresh through to auth-service, so the browser only ever talks to
    this server (no second address, no CORS). AUTH_SERVICE_URL is e.g. http://10.18.72.182:8084.
    """
    request = urllib.request.Request(
        os.environ["AUTH_SERVICE_URL"].rstrip("/") + path,
        data=json.dumps(body).encode(),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as error:
        # 4xx/5xx from auth-service, e.g. 401 for a wrong password
        return error.code, None
    except (urllib.error.URLError, TimeoutError) as error:
        raise HTTPException(502, "The login service isn't reachable right now") from error


def staff_tokens(status, tokens, rejected):
    """auth-service's answer to a login or refresh, checked before it goes back to the browser.

    `rejected` is the message for a 401, e.g. a wrong password. A correct password for a CLIENT login
    still gets 403 here: it would only fail on every page anyway.
    """
    if status in (400, 401):
        raise HTTPException(401, rejected)
    if status != 200 or not tokens:
        raise HTTPException(502, "The login service had a problem; try again")
    claims = read_token(tokens.get("accessToken", ""))
    if claims is None:
        # signed with a different secret: auth-service and the dashboard must share JWT_SHARED_SECRET
        raise HTTPException(502, "The login service gave a token this dashboard can't check")
    if not set(claims.get("roles") or []) & set(STAFF_ROLES):
        raise HTTPException(403, "This login doesn't have access to the dashboard")
    return {"accessToken": tokens["accessToken"], "refreshToken": tokens["refreshToken"], "roles": claims["roles"]}
