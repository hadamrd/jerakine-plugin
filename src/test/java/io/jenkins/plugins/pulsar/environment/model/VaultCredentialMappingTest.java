package io.jenkins.plugins.pulsar.environment.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for the VaultCredentialMapping model class. */
class VaultCredentialMappingTest {

  @Test
  @DisplayName("Constructor sets vaultId and credentialId")
  void constructorSetsFields() {
    VaultCredentialMapping mapping = new VaultCredentialMapping("vault-main", "cred-abc123");
    assertEquals("vault-main", mapping.getVaultId());
    assertEquals("cred-abc123", mapping.getCredentialId());
  }

  @Test
  @DisplayName("Description is optional (null by default)")
  void descriptionOptional() {
    VaultCredentialMapping mapping = new VaultCredentialMapping("v", "c");
    assertNull(mapping.getDescription());
  }

  @Test
  @DisplayName("Description setter works")
  void descriptionSetter() {
    VaultCredentialMapping mapping = new VaultCredentialMapping("v", "c");
    mapping.setDescription("Main vault for secrets");
    assertEquals("Main vault for secrets", mapping.getDescription());
  }

  @Test
  @DisplayName("Implements Serializable")
  void isSerializable() {
    VaultCredentialMapping mapping = new VaultCredentialMapping("v", "c");
    assertInstanceOf(java.io.Serializable.class, mapping);
  }
}
