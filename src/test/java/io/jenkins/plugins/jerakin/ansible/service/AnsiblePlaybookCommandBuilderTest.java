package io.jenkins.plugins.jerakin.ansible.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AnsiblePlaybookCommandBuilder, covering command construction, the builder pattern,
 * input validation, and shell injection prevention.
 */
class AnsiblePlaybookCommandBuilderTest {

  private AnsiblePlaybookCommandBuilder builder;

  @BeforeEach
  void setUp() {
    builder = new AnsiblePlaybookCommandBuilder();
  }

  // ── Basic command building ────────────────────────────────────

  @Nested
  @DisplayName("Basic command building")
  class BasicCommandBuilding {

    @Test
    @DisplayName("Minimal command with only playbook")
    void minimalCommand() {
      String cmd = builder.playbook("site.yml").buildAnsibleCommand();

      assertTrue(cmd.startsWith("ansible-playbook site.yml"));
      assertTrue(cmd.contains("-u ansible")); // default user
      assertTrue(cmd.contains("-e 'running_from_jenkins=true'"));
    }

    @Test
    @DisplayName("Full command with all options")
    void fullCommand() {
      Map<String, Object> vars = new LinkedHashMap<>();
      vars.put("version", "1.2.3");

      String cmd =
          builder
              .playbook("deploy.yml")
              .user("deploy")
              .inventory("inventory/prod")
              .extraVars(vars)
              .buildAnsibleCommand();

      assertTrue(cmd.contains("ansible-playbook deploy.yml"));
      assertTrue(cmd.contains("-u deploy"));
      assertTrue(cmd.contains("-i 'inventory/prod'"));
      assertTrue(cmd.contains("-e 'version=1.2.3'"));
    }

    @Test
    @DisplayName("buildCmd includes set -e and cd")
    void buildCmdWithProjectRoot() {
      String cmd = builder.playbook("site.yml").projectRoot("/opt/ansible").buildCmd();

      assertTrue(cmd.startsWith("set -e && cd /opt/ansible && ansible-playbook"));
    }

    @Test
    @DisplayName("buildCmd without projectRoot omits cd")
    void buildCmdWithoutProjectRoot() {
      String cmd = builder.playbook("site.yml").buildCmd();

      assertTrue(cmd.startsWith("set -e && ansible-playbook"));
      assertFalse(cmd.contains("cd "));
    }

    @Test
    @DisplayName("Null user defaults to root")
    void nullUserDefaultsToRoot() {
      String cmd = builder.playbook("site.yml").user(null).buildAnsibleCommand();
      assertTrue(cmd.contains("-u root"));
    }
  }

  // ── Builder pattern ───────────────────────────────────────────

  @Nested
  @DisplayName("Builder pattern")
  class BuilderPattern {

    @Test
    @DisplayName("All setters return this for chaining")
    void chainingWorks() {
      AnsiblePlaybookCommandBuilder result =
          builder
              .playbook("site.yml")
              .user("deploy")
              .inventory("inv")
              .projectRoot("/opt")
              .extraVars(Map.of("k", "v"))
              .options("--check");

      assertSame(builder, result);
    }

    @Test
    @DisplayName("buildCommandParts returns separate parts")
    void commandParts() {
      var parts = builder.playbook("site.yml").projectRoot("/opt/ansible").buildCommandParts();

      assertEquals(2, parts.size());
      assertEquals("cd /opt/ansible", parts.get(0));
      assertTrue(parts.get(1).startsWith("ansible-playbook"));
    }
  }

  // ── Validation ────────────────────────────────────────────────

  @Nested
  @DisplayName("Validation")
  class Validation {

    @Test
    @DisplayName("Throws when playbook is null")
    void nullPlaybook() {
      assertThrows(IllegalArgumentException.class, () -> builder.validate());
    }

    @Test
    @DisplayName("Throws when playbook is empty")
    void emptyPlaybook() {
      assertThrows(IllegalArgumentException.class, () -> builder.playbook("  ").validate());
    }

    @Test
    @DisplayName("Throws when user is empty")
    void emptyUser() {
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.playbook("site.yml").user("").validate());
    }

    @Test
    @DisplayName("Valid playbook and user pass validation")
    void validConfig() {
      assertDoesNotThrow(() -> builder.playbook("playbooks/deploy.yml").user("ansible").validate());
    }
  }

  // ── Shell injection prevention ────────────────────────────────

  @Nested
  @DisplayName("Shell injection prevention")
  class ShellInjectionPrevention {

    @Test
    @DisplayName("escapeValue handles single quotes safely")
    void singleQuoteEscaping() {
      // The proper POSIX way to embed ' in a single-quoted string is: '\''
      String escaped = AnsiblePlaybookCommandBuilder.escapeValue("it's dangerous");
      assertEquals("it'\\''s dangerous", escaped);
    }

    @Test
    @DisplayName("escapeValue handles null")
    void nullValue() {
      assertEquals("", AnsiblePlaybookCommandBuilder.escapeValue(null));
    }

    @Test
    @DisplayName("escapeValue preserves safe strings")
    void safeStringUnchanged() {
      assertEquals("hello-world_123", AnsiblePlaybookCommandBuilder.escapeValue("hello-world_123"));
    }

    @Test
    @DisplayName("escapeValue neutralizes command substitution in values")
    void commandSubstitutionInValues() {
      // $(cmd) inside single quotes is already safe in POSIX shell, but ' breaks out
      // The key case: a value like: foo'$(rm -rf /)
      String escaped = AnsiblePlaybookCommandBuilder.escapeValue("foo'$(rm -rf /)");
      // Should produce: foo'\''$(rm -rf /)
      // When embedded in single quotes: -e 'key=foo'\''$(rm -rf /)'
      // This correctly yields the literal string: key=foo'$(rm -rf /)
      assertEquals("foo'\\''$(rm -rf /)", escaped);
    }

    @Test
    @DisplayName("escapeValue handles backtick command substitution")
    void backtickSubstitution() {
      // Backticks are safe inside single quotes, but test that ' still escapes properly
      String escaped = AnsiblePlaybookCommandBuilder.escapeValue("val'`whoami`");
      assertEquals("val'\\''`whoami`", escaped);
    }

    @Test
    @DisplayName("Playbook path rejects shell metacharacters")
    void playbookRejectsMetachars() {
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.playbook("site.yml; rm -rf /").buildAnsibleCommand());
    }

    @Test
    @DisplayName("User rejects shell metacharacters")
    void userRejectsMetachars() {
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.playbook("site.yml").user("root$(whoami)").buildAnsibleCommand());
    }

    @Test
    @DisplayName("Inventory rejects shell metacharacters")
    void inventoryRejectsMetachars() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              builder
                  .playbook("site.yml")
                  .inventory("inv'; rm -rf /; echo '")
                  .buildAnsibleCommand());
    }

    @Test
    @DisplayName("projectRoot rejects shell metacharacters")
    void projectRootRejectsMetachars() {
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.playbook("site.yml").projectRoot("/opt; evil").buildCmd());
    }

    @Test
    @DisplayName("Extra var keys reject shell metacharacters")
    void extraVarKeysRejectMetachars() {
      Map<String, Object> vars = Map.of("key$(evil)", "value");
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.playbook("site.yml").extraVars(vars).buildAnsibleCommand());
    }

    @Test
    @DisplayName("Extra var values with quotes are properly escaped")
    void extraVarValuesEscaped() {
      Map<String, Object> vars = new LinkedHashMap<>();
      vars.put("msg", "it's a test");

      String cmd = builder.playbook("site.yml").extraVars(vars).buildAnsibleCommand();

      // The value should use the '\'' escape pattern
      assertTrue(cmd.contains("-e 'msg=it'\\''s a test'"));
    }
  }

  // ── requireSafeIdentifier ─────────────────────────────────────

  @Nested
  @DisplayName("requireSafeIdentifier")
  class SafeIdentifierValidation {

    @Test
    @DisplayName("Accepts valid paths")
    void validPaths() {
      assertDoesNotThrow(
          () ->
              AnsiblePlaybookCommandBuilder.requireSafeIdentifier(
                  "playbooks/deploy.yml", "test"));
      assertDoesNotThrow(
          () ->
              AnsiblePlaybookCommandBuilder.requireSafeIdentifier(
                  "inventory/prod/hosts", "test"));
      assertDoesNotThrow(
          () -> AnsiblePlaybookCommandBuilder.requireSafeIdentifier("/opt/ansible", "test"));
    }

    @Test
    @DisplayName("Accepts valid usernames")
    void validUsernames() {
      assertDoesNotThrow(
          () -> AnsiblePlaybookCommandBuilder.requireSafeIdentifier("ansible", "test"));
      assertDoesNotThrow(
          () -> AnsiblePlaybookCommandBuilder.requireSafeIdentifier("deploy-user", "test"));
      assertDoesNotThrow(
          () -> AnsiblePlaybookCommandBuilder.requireSafeIdentifier("root", "test"));
    }

    @Test
    @DisplayName("Rejects semicolons")
    void rejectsSemicolons() {
      assertThrows(
          IllegalArgumentException.class,
          () -> AnsiblePlaybookCommandBuilder.requireSafeIdentifier("a;b", "test"));
    }

    @Test
    @DisplayName("Rejects dollar sign")
    void rejectsDollar() {
      assertThrows(
          IllegalArgumentException.class,
          () -> AnsiblePlaybookCommandBuilder.requireSafeIdentifier("$(cmd)", "test"));
    }

    @Test
    @DisplayName("Rejects backticks")
    void rejectsBackticks() {
      assertThrows(
          IllegalArgumentException.class,
          () -> AnsiblePlaybookCommandBuilder.requireSafeIdentifier("`cmd`", "test"));
    }

    @Test
    @DisplayName("Rejects pipes")
    void rejectsPipes() {
      assertThrows(
          IllegalArgumentException.class,
          () -> AnsiblePlaybookCommandBuilder.requireSafeIdentifier("a|b", "test"));
    }

    @Test
    @DisplayName("Rejects spaces")
    void rejectsSpaces() {
      assertThrows(
          IllegalArgumentException.class,
          () -> AnsiblePlaybookCommandBuilder.requireSafeIdentifier("a b", "test"));
    }

    @Test
    @DisplayName("Rejects empty string")
    void rejectsEmpty() {
      assertThrows(
          IllegalArgumentException.class,
          () -> AnsiblePlaybookCommandBuilder.requireSafeIdentifier("", "test"));
    }

    @Test
    @DisplayName("Rejects null")
    void rejectsNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> AnsiblePlaybookCommandBuilder.requireSafeIdentifier(null, "test"));
    }
  }

  // ── Summary and display ───────────────────────────────────────

  @Nested
  @DisplayName("Summary")
  class Summary {

    @Test
    @DisplayName("getSummary includes all configured fields")
    void fullSummary() {
      String summary =
          builder
              .playbook("deploy.yml")
              .user("deploy")
              .inventory("inventory/prod")
              .extraVars(Map.of("version", "1.0"))
              .options("--check")
              .getSummary();

      assertTrue(summary.contains("Playbook: deploy.yml"));
      assertTrue(summary.contains("User: deploy"));
      assertTrue(summary.contains("Inventory: inventory/prod"));
      assertTrue(summary.contains("Extra vars:"));
      assertTrue(summary.contains("Options: --check"));
    }

    @Test
    @DisplayName("getSummary omits unset fields")
    void minimalSummary() {
      String summary = builder.playbook("site.yml").getSummary();

      assertTrue(summary.contains("Playbook: site.yml"));
      assertTrue(summary.contains("User: ansible"));
      assertFalse(summary.contains("Inventory:"));
      assertFalse(summary.contains("Extra vars:"));
      assertFalse(summary.contains("Options:"));
    }
  }
}
