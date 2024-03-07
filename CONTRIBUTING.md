# CONTRIBUTING

## Requirements

- Java 17
- Make sure [git-secrets](https://github.com/awslabs/git-secrets) installed. This tool prevents developers from committing passwords and secrets to git.

## Testing

The Landing Zone Service contains unit and integration tests. These tests are run as part of the CI pipeline during the PR process,
as well as on merge to `main`.

### Local Testing

```sh
# Unit tests
./gradlew :library:unitTest

# integration tests
./gradlew :library:integrationTest
```

#### Setup for Local Integration Testing
Running integration tests locally requires:
* A credential capable of connecting to the subscription and tenant configured [here](https://github.com/DataBiosphere/terra-landing-zone-service/blob/main/service/src/test/java/bio/terra/landingzone/library/landingzones/AzureIntegrationUtils.java#L27).
  In CI, we have a federated identity configured which logs in and sets the appropriate environment variables. For local testing,
  the Azure CLI is the best way to get the needed environment variables set via an invocation of `az login`. For more information,
  see the related Azure [documentation](https://learn.microsoft.com/en-us/java/api/overview/azure/identity-readme?view=azure-java-stable#defaultazurecredential).

* A running postgres:
```
 ./library/local-dev/run_postgres.sh start|stop
```

### Smoke tests

For information on executing smoke tests to check that the Landing Zone service is operational in a given environment, see the smoke test [README.md](./smoke_tests/README.md).
