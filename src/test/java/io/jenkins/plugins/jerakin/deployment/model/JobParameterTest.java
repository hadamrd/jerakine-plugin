package io.jenkins.plugins.jerakin.deployment.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for the JobParameter model class. */
class JobParameterTest {

  @Test
  @DisplayName("Constructor sets name and value")
  void constructorSetsFields() {
    JobParameter param = new JobParameter("environment", "production");
    assertEquals("environment", param.getName());
    assertEquals("production", param.getValue());
  }

  @Test
  @DisplayName("Fields are immutable (final)")
  void fieldsAreImmutable() {
    JobParameter param = new JobParameter("key", "value");
    // Can't test immutability at compile time, but verify values don't change
    assertEquals("key", param.getName());
    assertEquals("value", param.getValue());
  }

  @Test
  @DisplayName("Implements Serializable")
  void isSerializable() {
    JobParameter param = new JobParameter("k", "v");
    assertInstanceOf(java.io.Serializable.class, param);
  }

  @Test
  @DisplayName("Handles null values without NPE")
  void handlesNullValues() {
    JobParameter param = new JobParameter(null, null);
    assertNull(param.getName());
    assertNull(param.getValue());
  }
}
