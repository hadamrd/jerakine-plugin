package io.jenkins.plugins.jerakin.deployment.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for the DeploymentJob model class. */
class DeploymentJobTest {

  @Test
  @DisplayName("Constructor sets all fields correctly")
  void constructorSetsFields() {
    List<JobParameter> params = List.of(new JobParameter("env", "prod"));
    DeploymentJob job = new DeploymentJob("job-1", "Deploy App", "web", "standard-deploy", params);

    assertEquals("job-1", job.getId());
    assertEquals("Deploy App", job.getName());
    assertEquals("web", job.getCategory());
    assertEquals("standard-deploy", job.getTemplateName());
    assertEquals(1, job.getParams().size());
  }

  @Test
  @DisplayName("Constructor handles null params list")
  void constructorHandlesNullParams() {
    DeploymentJob job = new DeploymentJob("job-1", "Deploy", "web", "template", null);
    assertNotNull(job.getParams());
    assertTrue(job.getParams().isEmpty());
  }

  @Nested
  @DisplayName("getParamsAsMap")
  class ParamsAsMap {

    @Test
    @DisplayName("Converts params list to map")
    void convertsToMap() {
      List<JobParameter> params =
          Arrays.asList(
              new JobParameter("environment", "prod"),
              new JobParameter("version", "1.2.3"),
              new JobParameter("dryRun", "false"));

      DeploymentJob job = new DeploymentJob("id", "name", "cat", "tmpl", params);
      Map<String, String> map = job.getParamsAsMap();

      assertEquals(3, map.size());
      assertEquals("prod", map.get("environment"));
      assertEquals("1.2.3", map.get("version"));
      assertEquals("false", map.get("dryRun"));
    }

    @Test
    @DisplayName("Returns empty map for no params")
    void emptyParamsYieldEmptyMap() {
      DeploymentJob job = new DeploymentJob("id", "name", "cat", "tmpl", null);
      Map<String, String> map = job.getParamsAsMap();
      assertTrue(map.isEmpty());
    }

    @Test
    @DisplayName("Last param wins on duplicate keys")
    void duplicateKeysLastWins() {
      List<JobParameter> params =
          Arrays.asList(new JobParameter("key", "first"), new JobParameter("key", "second"));

      DeploymentJob job = new DeploymentJob("id", "name", "cat", "tmpl", params);
      Map<String, String> map = job.getParamsAsMap();

      assertEquals("second", map.get("key"));
    }
  }

  @Test
  @DisplayName("Implements Serializable")
  void isSerializable() {
    DeploymentJob job = new DeploymentJob("id", "name", "cat", "tmpl", List.of());
    assertInstanceOf(java.io.Serializable.class, job);
  }
}
