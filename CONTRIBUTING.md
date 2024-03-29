# CONTRIBUTING

> **_NOTE:_**
> For compliance reasons, all pull requests must be submitted with a Jira ID as a part of the pull
> request.
>
> You should include the Jira ID near the beginning of the title for better readability.
>
> For example:
> `[XX-1234]: add statement to CONTRIBUTING.md about including Jira IDs in PR titles`
>
> If there is more than one relevant ticket, include all associated Jira IDs.
>
> For example:
> `[WOR-1997] [WOR-2002] [WOR-2005]: fix for many bugs with the same root cause`
>

## Requirements

- Java 17
- Make sure [git-secrets](https://github.com/awslabs/git-secrets) installed. This tool prevents developers from committing passwords and secrets to git.

## Testing

The Landing Zone Service contains unit and integration tests. These tests are run as part of the CI pipeline during the PR process,
as well as on merge to `main`.

### Local Testing

```sh
# Run setup to set the environment, including postgres running in docker:
./scripts/setup

# Unit tests (currently library, service, and testharness tests):
./scripts/run test

# integration tests
./scripts/run integration
```

#### Setup for Local Integration Testing
Running integration tests locally requires:
* A credential capable of connecting to the subscription and tenant configured [here](https://github.com/DataBiosphere/terra-landing-zone-service/blob/main/service/src/test/java/bio/terra/landingzone/library/landingzones/AzureIntegrationUtils.java#L27).
  In CI, we have a federated identity configured which logs in and sets the appropriate environment variables. For local testing,
  the Azure CLI is the best way to get the needed environment variables set via an invocation of `az login`. For more information,
  see the related Azure [documentation](https://learn.microsoft.com/en-us/java/api/overview/azure/identity-readme?view=azure-java-stable#defaultazurecredential).

* A running postgres:
```sh
./scripts/setup
# or
./scripts/run-db start|stop
```

### Smoke tests

For information on executing smoke tests to check that the Landing Zone service is operational in a given environment, see the smoke test [README.md](./smoke_tests/README.md).


## SourceClear

[SourceClear](https://srcclr.github.io) is a static analysis tool that scans a project's Java
dependencies for known vulnerabilities. If you are working on addressing dependency vulnerabilities
in response to a SourceClear finding, you may want to run a scan off of a feature branch and/or local code.

### Github Action

You can trigger LZS's SCA scan on demand via its
[Github Action](https://github.com/broadinstitute/dsp-appsec-sourceclear-github-actions/actions/workflows/z-manual-terra-landing-zone-service.yml),
and optionally specify a Github ref (branch, tag, or SHA) to check out from the repo to scan.  By default,
the scan is run off of LZS's `main` branch.

High-level results are outputted in the Github Actions run.

### Running Locally

You will need to get the API token from Vault before running the Gradle `srcclr` task.

```sh
export SRCCLR_API_TOKEN=$(vault read -field=api_token secret/secops/ci/srcclr/gradle-agent)
./gradlew srcclr
```

High-level results are outputted to the terminal.

### Veracode

Full results including dependency graphs are uploaded to
[Veracode](https://sca.analysiscenter.veracode.com/workspaces/jppForw/projects/554814/issues)
(if running off of a feature branch, navigate to Project Details > Selected Branch > Change to select your feature branch).
You can request a Veracode account to view full results from #dsp-infosec-champions.
