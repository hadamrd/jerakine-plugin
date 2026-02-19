package io.jenkins.plugins.pulsar.ssh.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for the SshConfig model class. */
class SshConfigTest {

  @Test
  @DisplayName("Default constructor sets sensible defaults")
  void defaultValues() {
    SshConfig config = new SshConfig();

    assertEquals(30000, config.getConnectTimeout());
    assertEquals(60, config.getServerAliveInterval());
    assertEquals(3, config.getServerAliveCountMax());
    assertFalse(config.isStrictHostKeyChecking());
    assertEquals("publickey,keyboard-interactive,password", config.getPreferredAuthentications());
    assertTrue(config.isCompressionEnabled());
    assertEquals(6, config.getCompressionLevel());
    assertNotNull(config.getCipherList());
    assertNotNull(config.getMacList());
    assertNotNull(config.getKexList());
  }

  // ── Bounds validation ─────────────────────────────────────────

  @Nested
  @DisplayName("Setter bounds validation")
  class BoundsValidation {

    @Test
    @DisplayName("connectTimeout is clamped to [5000, 300000]")
    void connectTimeoutBounds() {
      SshConfig config = new SshConfig();

      config.setConnectTimeout(1); // Below minimum
      assertEquals(5000, config.getConnectTimeout());

      config.setConnectTimeout(999999); // Above maximum
      assertEquals(300000, config.getConnectTimeout());

      config.setConnectTimeout(15000); // Within range
      assertEquals(15000, config.getConnectTimeout());
    }

    @Test
    @DisplayName("serverAliveInterval is clamped to [0, 3600]")
    void serverAliveIntervalBounds() {
      SshConfig config = new SshConfig();

      config.setServerAliveInterval(-1);
      assertEquals(0, config.getServerAliveInterval());

      config.setServerAliveInterval(7200);
      assertEquals(3600, config.getServerAliveInterval());

      config.setServerAliveInterval(120);
      assertEquals(120, config.getServerAliveInterval());
    }

    @Test
    @DisplayName("serverAliveCountMax is clamped to [1, 10]")
    void serverAliveCountMaxBounds() {
      SshConfig config = new SshConfig();

      config.setServerAliveCountMax(0);
      assertEquals(1, config.getServerAliveCountMax());

      config.setServerAliveCountMax(20);
      assertEquals(10, config.getServerAliveCountMax());

      config.setServerAliveCountMax(5);
      assertEquals(5, config.getServerAliveCountMax());
    }

    @Test
    @DisplayName("compressionLevel is clamped to [1, 9]")
    void compressionLevelBounds() {
      SshConfig config = new SshConfig();

      config.setCompressionLevel(0);
      assertEquals(1, config.getCompressionLevel());

      config.setCompressionLevel(15);
      assertEquals(9, config.getCompressionLevel());

      config.setCompressionLevel(4);
      assertEquals(4, config.getCompressionLevel());
    }
  }

  // ── JSch properties generation ────────────────────────────────

  @Nested
  @DisplayName("toJSchProperties")
  class JSchProperties {

    @Test
    @DisplayName("Generates correct properties with defaults")
    void defaultProperties() {
      SshConfig config = new SshConfig();
      Properties props = config.toJSchProperties();

      assertEquals("30000", props.getProperty("ConnectTimeout"));
      assertEquals("60", props.getProperty("ServerAliveInterval"));
      assertEquals("3", props.getProperty("ServerAliveCountMax"));
      assertEquals("no", props.getProperty("StrictHostKeyChecking"));
      assertEquals(
          "publickey,keyboard-interactive,password",
          props.getProperty("PreferredAuthentications"));
    }

    @Test
    @DisplayName("Compression enabled generates zlib properties")
    void compressionEnabled() {
      SshConfig config = new SshConfig();
      config.setCompressionEnabled(true);
      Properties props = config.toJSchProperties();

      assertEquals("zlib@openssh.com,zlib,none", props.getProperty("compression.s2c"));
      assertEquals("zlib@openssh.com,zlib,none", props.getProperty("compression.c2s"));
      assertEquals("6", props.getProperty("compression_level"));
    }

    @Test
    @DisplayName("Compression disabled generates none")
    void compressionDisabled() {
      SshConfig config = new SshConfig();
      config.setCompressionEnabled(false);
      Properties props = config.toJSchProperties();

      assertEquals("none", props.getProperty("compression.s2c"));
      assertEquals("none", props.getProperty("compression.c2s"));
    }

    @Test
    @DisplayName("StrictHostKeyChecking set to 'yes' when enabled")
    void strictHostKeyCheckingEnabled() {
      SshConfig config = new SshConfig();
      config.setStrictHostKeyChecking(true);
      Properties props = config.toJSchProperties();

      assertEquals("yes", props.getProperty("StrictHostKeyChecking"));
    }

    @Test
    @DisplayName("Cipher, MAC, and KEX lists are symmetric (s2c = c2s)")
    void symmetricCryptoSettings() {
      SshConfig config = new SshConfig();
      Properties props = config.toJSchProperties();

      assertEquals(props.getProperty("cipher.s2c"), props.getProperty("cipher.c2s"));
      assertEquals(props.getProperty("mac.s2c"), props.getProperty("mac.c2s"));
    }

    @Test
    @DisplayName("ServerAliveInterval=0 omits keepalive properties")
    void noKeepaliveWhenIntervalZero() {
      SshConfig config = new SshConfig();
      config.setServerAliveInterval(0);
      Properties props = config.toJSchProperties();

      assertNull(props.getProperty("ServerAliveInterval"));
      assertNull(props.getProperty("ServerAliveCountMax"));
    }
  }

  // ── Factory methods ───────────────────────────────────────────

  @Nested
  @DisplayName("Factory methods")
  class FactoryMethods {

    @Test
    @DisplayName("createProductionConfig has strict settings")
    void productionConfig() {
      SshConfig config = SshConfig.createProductionConfig();

      assertTrue(config.isStrictHostKeyChecking());
      assertEquals(10000, config.getConnectTimeout());
      assertEquals("publickey", config.getPreferredAuthentications());
      assertFalse(config.isCompressionEnabled());
    }

    @Test
    @DisplayName("createDevelopmentConfig has relaxed settings")
    void developmentConfig() {
      SshConfig config = SshConfig.createDevelopmentConfig();

      assertFalse(config.isStrictHostKeyChecking());
      assertEquals(30000, config.getConnectTimeout());
      assertTrue(config.isCompressionEnabled());
    }
  }

  // ── Display / toString ────────────────────────────────────────

  @Test
  @DisplayName("getSummary returns human-readable string")
  void summary() {
    SshConfig config = new SshConfig();
    String summary = config.getSummary();

    assertTrue(summary.contains("timeout=30000ms"));
    assertTrue(summary.contains("keepalive=60s"));
    assertTrue(summary.contains("compression=on"));
    assertTrue(summary.contains("strict_host_checking=off"));
  }

  @Test
  @DisplayName("toString returns descriptive string")
  void toStringOutput() {
    SshConfig config = new SshConfig();
    String str = config.toString();

    assertTrue(str.contains("SshConfig{"));
    assertTrue(str.contains("connectTimeout=30000"));
  }
}
