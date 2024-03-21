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
