# Runs the reporting (Python) tests. Used by the Reporting-Tests stage in the Jenkinsfile,
# so the Jenkins agent only needs Docker, not Python.
#
# Build and run from the repo root:
#   docker build -f reporting/tests.Dockerfile -t reporting-tests .
#   docker run --rm reporting-tests
#
# The full python image (not -slim) is used because it already has the C compiler and
# Postgres headers that psycopg2 needs to install.
FROM python:3.14

WORKDIR /app

# Install packages first, so Docker can reuse this layer when only the code changes.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY reporting ./reporting
WORKDIR /app/reporting

# The tests use fakes instead of a database, so nothing else needs to be running.
# --junitxml writes the results in the format Jenkins' junit step reads.
CMD ["python", "-m", "pytest", "-v", "--junitxml=pytest-results.xml"]
