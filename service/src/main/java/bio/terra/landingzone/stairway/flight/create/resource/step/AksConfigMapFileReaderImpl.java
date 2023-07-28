package bio.terra.landingzone.stairway.flight.create.resource.step;

import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.util.Yaml;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class AksConfigMapFileReaderImpl implements AksConfigMapReader {
  private final String filePath;

  public AksConfigMapFileReaderImpl(String filePath) {
    this.filePath = filePath;
  }

  @Override
  public V1ConfigMap read() throws AksConfigMapReaderException {
    var inputStream = getFileFromResourceAsStream();
    try {
      Object value =
          Yaml.load(new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)));
      if (value instanceof V1ConfigMap v1ConfigMap) {
        return v1ConfigMap;
      } else {
        throw new AksConfigMapReaderException(
            String.format("File '%s' doesn't contain ConfigMap definition.", filePath));
      }
    } catch (IOException ex) {
      throw new AksConfigMapReaderException(
          String.format(
              "Failed to initialize ConfigMap from resource file. File name: '%s'.", filePath),
          ex);
    }
  }

  private InputStream getFileFromResourceAsStream() throws AksConfigMapReaderException {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(filePath);
    if (inputStream == null) {
      throw new AksConfigMapReaderException(
          String.format("File '%s' not found or access is denied.", filePath));
    }
    return inputStream;
  }
}
