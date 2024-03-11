# Smoke Tests for Landing Zone Service

## Purpose
To provide a low overhead way to check that Landing Zone service is basically operational in a given environment.

These tests are intended to be run either as the final step in the deployment check list for releasing the service independently, or on an ad-hoc basis if needed.
They are currently set up to be run manually, but could be automated in the future.

## Scope
Verifying _basic_ functionality, based on arguments passed:
* The status endpoint returns 200, and all subsystems specified by the service are 'OK'
* The version endpoint returns 200, and version payload exists
* If a user access token is passed, the following endpoints will be called:
  * `GET /api/landingzones/definitions/v1/azure` to verify available landing zone definitions, and checked that it returns 200.
  * `GET api/landingzones/v1/azure`  to get all landing zones available to user, and checked that it returns 200.
    This provides basic verification that the database connection is intact, even if the user has no landing zones available.


## Setup
* From the smoke_tests directory
* Install [poetry](https://python-poetry.org) or `brew install poetry` on a mac
* Install dependencies: `poetry install`
* Get a shell in the created venv to run the scripts: `poetry shell`
* Run the test: `python smoke_tests.py <args>`
* For full usage information: `python smoke_tests.py -h`


## Running as part of post-deployment steps:
* Do setup as described above
* Get a user access token. Eg: `gcloud auth print-access-token`
* Run script in poetry shell: `python smoke_tests.py "${LZ_HOST}" "${USER_ACCESS_TOKEN}"`
