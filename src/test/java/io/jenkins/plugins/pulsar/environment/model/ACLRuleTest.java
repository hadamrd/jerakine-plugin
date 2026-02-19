package io.jenkins.plugins.pulsar.environment.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for the ACLRule model class. */
class ACLRuleTest {

  @Test
  @DisplayName("Constructor sets name and type, initializes empty lists")
  void constructorDefaults() {
    ACLRule rule = new ACLRule("test-rule", "allow");

    assertEquals("test-rule", rule.getName());
    assertEquals("allow", rule.getType());
    assertEquals(0, rule.getPriority());
    assertNotNull(rule.getJobs());
    assertTrue(rule.getJobs().isEmpty());
    assertNotNull(rule.getEnvironments());
    assertTrue(rule.getEnvironments().isEmpty());
    assertNotNull(rule.getEnvironmentGroups());
    assertTrue(rule.getEnvironmentGroups().isEmpty());
    assertNotNull(rule.getEnvironmentTags());
    assertTrue(rule.getEnvironmentTags().isEmpty());
    assertNotNull(rule.getUsers());
    assertTrue(rule.getUsers().isEmpty());
    assertNotNull(rule.getGroups());
    assertTrue(rule.getGroups().isEmpty());
  }

  @Test
  @DisplayName("Setters accept and store values")
  void settersWork() {
    ACLRule rule = new ACLRule("rule", "deny");
    rule.setPriority(42);
    rule.setJobs(List.of("job1", "job2"));
    rule.setEnvironments(List.of("dev", "test"));
    rule.setEnvironmentGroups(List.of("nonprod"));
    rule.setEnvironmentTags(List.of("critical"));
    rule.setUsers(List.of("alice"));
    rule.setGroups(List.of("devs"));

    assertEquals(42, rule.getPriority());
    assertEquals(Arrays.asList("job1", "job2"), rule.getJobs());
    assertEquals(Arrays.asList("dev", "test"), rule.getEnvironments());
    assertEquals(List.of("nonprod"), rule.getEnvironmentGroups());
    assertEquals(List.of("critical"), rule.getEnvironmentTags());
    assertEquals(List.of("alice"), rule.getUsers());
    assertEquals(List.of("devs"), rule.getGroups());
  }

  @Test
  @DisplayName("Null-safe getters return empty lists instead of null")
  void nullSafeGetters() {
    ACLRule rule = new ACLRule("rule", "allow");
    rule.setJobs(null);
    rule.setEnvironments(null);
    rule.setEnvironmentGroups(null);
    rule.setEnvironmentTags(null);
    rule.setUsers(null);
    rule.setGroups(null);

    assertNotNull(rule.getJobs());
    assertNotNull(rule.getEnvironments());
    assertNotNull(rule.getEnvironmentGroups());
    assertNotNull(rule.getEnvironmentTags());
    assertNotNull(rule.getUsers());
    assertNotNull(rule.getGroups());
  }

  @Test
  @DisplayName("Type field supports both allow and deny")
  void typeField() {
    ACLRule allow = new ACLRule("a", "allow");
    ACLRule deny = new ACLRule("d", "deny");

    assertEquals("allow", allow.getType());
    assertEquals("deny", deny.getType());
  }

  @Test
  @DisplayName("Rule implements Serializable")
  void isSerializable() {
    ACLRule rule = new ACLRule("rule", "allow");
    assertInstanceOf(java.io.Serializable.class, rule);
  }
}
