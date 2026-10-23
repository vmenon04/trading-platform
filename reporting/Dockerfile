# syntax=docker/dockerfile:1
# Reporting: one image, run by compose as three services (reporting-api, reporting-sync, reporting-consumer).
# Built from ./reporting; requirements.txt comes from the repo root through the "project" build context.
FROM python:3.13-slim

WORKDIR /app

# psycopg2 is built from source, so it needs a compiler and libpq's headers
RUN apt-get update \
    && apt-get install -y --no-install-recommends gcc libc6-dev libpq-dev \
    && rm -rf /var/lib/apt/lists/*

COPY --from=project requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY analytics_schema.sql queries.sql ./
COPY scripts ./scripts

# the scripts import each other by name, and read their settings from the environment (set by compose)
WORKDIR /app/scripts
CMD ["uvicorn", "api:app", "--host", "0.0.0.0", "--port", "8050"]
