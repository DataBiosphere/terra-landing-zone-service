# Landing Zone Service

Note: Currently, all core logic lives in library module. This module is where all endpoints will be located.
Potentially all library stuff can be moved into service later. Now we have library module for backward compatibility
to supply library to WSM. All active development is still in library module.

When the service is locally running, the Swagger interface can be accessed at http://localhost:8080/swagger-ui.html.

To run the service locally:
```shell
./scripts/setup 
./scripts/run local
```

To run or debug a locally running service using IntelliJ, after executing `./scripts/setup`, start a run/debug
configuration pointing at `bio.terra.lz.futureservice.app.LandingZoneApplication`.
