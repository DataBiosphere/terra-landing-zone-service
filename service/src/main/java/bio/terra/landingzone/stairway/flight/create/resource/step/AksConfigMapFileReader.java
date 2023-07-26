package bio.terra.landingzone.stairway.flight.create.resource.step;

import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.util.Yaml;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class AksConfigMapFileReader implements AksConfigMapReader {
  private final String filePath;

  public AksConfigMapFileReader(String filePath) {
    this.filePath = filePath;
  }

  @Override
  public V1ConfigMap read() throws AksConfigMapReaderException {
    File file = getFileFromResource();
    try {
      Object value = Yaml.load(file);
      if (value instanceof V1ConfigMap v1ConfigMap) {
        return v1ConfigMap;
      } else {
        throw new AksConfigMapReaderException(
            String.format("File '%s' doesn't contain ConfigMap definition.", filePath));
      }
    } catch (IOException ex) {
      throw new AksConfigMapReaderException(
          String.format(
              "Failed to initialize config map from resource file. File name: '%s'", filePath),
          ex);
    }
  }

  private File getFileFromResource() throws AksConfigMapReaderException {
    ClassLoader classLoader = getClass().getClassLoader();
    URL resource = classLoader.getResource(filePath);
    if (resource == null) {
      throw new AksConfigMapReaderException(String.format("File '%s' not found.", filePath));
    }
    File file;
    try {
      file = new File(resource.toURI());
    } catch (URISyntaxException e) {
      throw new AksConfigMapReaderException(
          String.format(
              "Failed to initialize file from resource. Resource file name: '%s'", filePath));
    }
    return file;
  }
}
