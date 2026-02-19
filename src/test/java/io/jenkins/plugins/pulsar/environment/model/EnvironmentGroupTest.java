package io.jenkins.plugins.pulsar.environment.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for the EnvironmentGroup model class. */
class EnvironmentGroupTest {

  @Test
  @DisplayName("Constructor sets name and initializes empty collections")
  void constructorDefaults() {
    EnvironmentGroup group = new EnvironmentGroup("production");

    assertEquals("production", group.getName());
    assertNull(group.getDescription());
    assertNull(group.getSshCredentialId());
    assertNotNull(group.getEnvironments());
    assertTrue(group.getEnvironments().isEmpty());
    assertNotNull(group.getTags());
    assertTrue(group.getTags().isEmpty());
    assertNotNull(group.getVaultCredentials());
    assertTrue(group.getVaultCredentials().isEmpty());
    assertNotNull(group.getNodeLabels());
    assertTrue(group.getNodeLabels().isEmpty());
  }

  @Test
  @DisplayName("Setters accept and store values")
  void settersWork() {
    EnvironmentGroup group = new EnvironmentGroup("dev");
    group.setDescription("Development environments");
    group.setEnvironments(List.of("dev1", "dev2"));
    group.setTags(List.of("non-production"));
    group.setSshCredentialId("ssh-dev-key");
    group.setNodeLabels(List.of("linux", "docker"));

    assertEquals("Development environments", group.getDescription());
    assertEquals(Arrays.asList("dev1", "dev2"), group.getEnvironments());
    assertEquals(List.of("non-production"), group.getTags());
    assertEquals("ssh-dev-key", group.getSshCredentialId());
    assertEquals(Arrays.asList("linux", "docker"), group.getNodeLabels());
  }

  @Test
  @DisplayName("Null-safe getters return empty lists")
  void nullSafeGetters() {
    EnvironmentGroup group = new EnvironmentGroup("test");
    group.setEnvironments(null);
    group.setTags(null);
    group.setVaultCredentials(null);
    group.setNodeLabels(null);

    assertNotNull(group.getEnvironments());
    assertNotNull(group.getTags());
    assertNotNull(group.getVaultCredentials());
    assertNotNull(group.getNodeLabels());
  }

  @Nested
  @DisplayName("getVaultCredentialId")
  class VaultCredentialLookup {

    @Test
    @DisplayName("Returns credential ID for matching vault ID")
    void findsMatch() {
      EnvironmentGroup group = new EnvironmentGroup("prod");
      VaultCredentialMapping mapping = new VaultCredentialMapping("vault-main", "cred-123");
      group.setVaultCredentials(List.of(mapping));

      assertEquals("cred-123", group.getVaultCredentialId("vault-main"));
    }

    @Test
    @DisplayName("Returns null for non-existent vault ID")
    void returnsNullForMissing() {
      EnvironmentGroup group = new EnvironmentGroup("prod");
      VaultCredentialMapping mapping = new VaultCredentialMapping("vault-main", "cred-123");
      group.setVaultCredentials(List.of(mapping));

      assertNull(group.getVaultCredentialId("vault-other"));
    }

    @Test
    @DisplayName("Returns first match when multiple exist")
    void returnsFirstMatch() {
      EnvironmentGroup group = new EnvironmentGroup("prod");
      group.setVaultCredentials(
          List.of(
              new VaultCredentialMapping("vault-main", "cred-first"),
              new VaultCredentialMapping("vault-main", "cred-second")));

      assertEquals("cred-first", group.getVaultCredentialId("vault-main"));
    }

    @Test
    @DisplayName("Works with empty vault credentials list")
    void worksWithEmptyList() {
      EnvironmentGroup group = new EnvironmentGroup("prod");
      assertNull(group.getVaultCredentialId("anything"));
    }
  }

  @Nested
  @DisplayName("getNodeLabelsAsString")
  class NodeLabelsAsString {

    @Test
    @DisplayName("Returns 'master' when no labels configured")
    void defaultFallback() {
      EnvironmentGroup group = new EnvironmentGroup("test");
      assertEquals("master", group.getNodeLabelsAsString());
    }

    @Test
    @DisplayName("Returns single label as-is")
    void singleLabel() {
      EnvironmentGroup group = new EnvironmentGroup("test");
      group.setNodeLabels(List.of("linux"));
      assertEquals("linux", group.getNodeLabelsAsString());
    }

    @Test
    @DisplayName("Joins multiple labels with Jenkins && operator")
    void multipleLabels() {
      EnvironmentGroup group = new EnvironmentGroup("test");
      group.setNodeLabels(List.of("linux", "docker", "x86_64"));
      assertEquals("linux && docker && x86_64", group.getNodeLabelsAsString());
    }
  }

  @Test
  @DisplayName("Implements Serializable")
  void isSerializable() {
    EnvironmentGroup group = new EnvironmentGroup("test");
    assertInstanceOf(java.io.Serializable.class, group);
  }
}
