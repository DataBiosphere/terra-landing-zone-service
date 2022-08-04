package bio.terra.landingzone.testutils;

import bio.terra.landingzone.app.Main;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Tag("library")
@ActiveProfiles({"library-test", "human-readable-logging"})
@ContextConfiguration(classes = Main.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest()
public class LibraryTestBase {}
