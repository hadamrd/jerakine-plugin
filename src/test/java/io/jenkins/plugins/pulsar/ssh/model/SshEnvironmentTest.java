package io.jenkins.plugins.pulsar.ssh.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for the SshEnvironment model class. */
class SshEnvironmentTest {

  @Test
  @DisplayName("Constructor sets name and hosts with defaults")
  void constructorDefaults() {
    SshEnvironment env = new SshEnvironment("staging", List.of("host1.example.com"));

    assertEquals("staging", env.getName());
    assertEquals(List.of("host1.example.com"), env.getHosts());
    assertEquals("root", env.getUsername());
    assertEquals(22, env.getPort());
    assertNotNull(env.getSshConfig());
    assertNull(env.getSshCredentialId());
  }

  @Test
  @DisplayName("Constructor handles null hosts")
  void constructorNullHosts() {
    SshEnvironment env = new SshEnvironment("test", null);
    assertNotNull(env.getHosts());
    assertTrue(env.getHosts().isEmpty());
  }

  @Test
  @DisplayName("Setters work correctly")
  void settersWork() {
    SshEnvironment env = new SshEnvironment("prod", List.of("host1"));
    env.setUsername("deploy");
    env.setPort(2222);
    env.setSshCredentialId("ssh-prod");

    assertEquals("deploy", env.getUsername());
    assertEquals(2222, env.getPort());
    assertEquals("ssh-prod", env.getSshCredentialId());
  }

  // ── Validation ────────────────────────────────────────────────

  @Nested
  @DisplayName("isValid")
  class Validation {

    @Test
    @DisplayName("Valid environment with all required fields")
    void validEnvironment() {
      SshEnvironment env = new SshEnvironment("prod", List.of("host1"));
      env.setSshCredentialId("ssh-key");
      assertTrue(env.isValid());
    }

    @Test
    @DisplayName("Invalid: null name")
    void invalidNullName() {
      SshEnvironment env = new SshEnvironment(null, List.of("host1"));
      env.setSshCredentialId("ssh-key");
      assertFalse(env.isValid());
    }

    @Test
    @DisplayName("Invalid: empty name")
    void invalidEmptyName() {
      SshEnvironment env = new SshEnvironment("  ", List.of("host1"));
      env.setSshCredentialId("ssh-key");
      assertFalse(env.isValid());
    }

    @Test
    @DisplayName("Invalid: no hosts")
    void invalidNoHosts() {
      SshEnvironment env = new SshEnvironment("prod", List.of());
      env.setSshCredentialId("ssh-key");
      assertFalse(env.isValid());
    }

    @Test
    @DisplayName("Invalid: no credential ID")
    void invalidNoCredential() {
      SshEnvironment env = new SshEnvironment("prod", List.of("host1"));
      assertFalse(env.isValid());
    }

    @Test
    @DisplayName("Invalid: empty credential ID")
    void invalidEmptyCredential() {
      SshEnvironment env = new SshEnvironment("prod", List.of("host1"));
      env.setSshCredentialId("  ");
      assertFalse(env.isValid());
    }
  }

  // ── Display ───────────────────────────────────────────────────

  @Test
  @DisplayName("getDisplayName shows name and host count")
  void displayName() {
    SshEnvironment env =
        new SshEnvironment("production", Arrays.asList("host1", "host2", "host3"));
    assertEquals("production (3 hosts)", env.getDisplayName());
  }

  @Test
  @DisplayName("getConfigSummary includes all config details")
  void configSummary() {
    SshEnvironment env = new SshEnvironment("staging", List.of("host1", "host2"));
    env.setUsername("deploy");
    env.setPort(2222);
    env.setSshCredentialId("ssh-staging");

    String summary = env.getConfigSummary();
    assertTrue(summary.contains("staging"));
    assertTrue(summary.contains("2 hosts"));
    assertTrue(summary.contains("deploy"));
    assertTrue(summary.contains("2222"));
    assertTrue(summary.contains("ssh-staging"));
  }

  @Test
  @DisplayName("toString includes all fields")
  void toStringOutput() {
    SshEnvironment env = new SshEnvironment("dev", List.of("localhost"));
    String str = env.toString();
    assertTrue(str.contains("SshEnvironment{"));
    assertTrue(str.contains("name='dev'"));
    assertTrue(str.contains("localhost"));
  }

  @Test
  @DisplayName("getSshConfig returns default when null")
  void sshConfigDefaultWhenNull() {
    SshEnvironment env = new SshEnvironment("test", List.of("host"));
    env.setSshConfig(null);
    assertNotNull(env.getSshConfig());
  }

  @Test
  @DisplayName("Implements Serializable")
  void isSerializable() {
    SshEnvironment env = new SshEnvironment("test", List.of("host"));
    assertInstanceOf(java.io.Serializable.class, env);
  }
}
