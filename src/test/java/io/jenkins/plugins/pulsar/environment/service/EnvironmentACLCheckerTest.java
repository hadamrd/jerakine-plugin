package io.jenkins.plugins.pulsar.environment.service;

import static org.junit.jupiter.api.Assertions.*;

import io.jenkins.plugins.pulsar.environment.model.ACLRule;
import io.jenkins.plugins.pulsar.environment.model.EnvironmentGroup;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the ACL evaluation engine. These tests exercise the core access control logic
 * without requiring a running Jenkins instance, by using the package-private overload that accepts
 * rules and environment groups directly.
 */
class EnvironmentACLCheckerTest {

  private List<EnvironmentGroup> environmentGroups;

  @BeforeEach
  void setUp() {
    environmentGroups = new ArrayList<>();

    EnvironmentGroup devGroup = new EnvironmentGroup("development");
    devGroup.setEnvironments(Arrays.asList("dev", "test"));
    devGroup.setTags(Arrays.asList("non-production"));

    EnvironmentGroup prodGroup = new EnvironmentGroup("production");
    prodGroup.setEnvironments(Arrays.asList("staging", "prod"));
    prodGroup.setTags(Arrays.asList("production", "critical"));

    environmentGroups.add(devGroup);
    environmentGroups.add(prodGroup);
  }

  // ── Default deny ──────────────────────────────────────────────

  @Test
  @DisplayName("Default deny: no rules means no access")
  void defaultDeny_noRules() {
    assertFalse(
        EnvironmentACLChecker.hasAccess(
            Collections.emptyList(), environmentGroups, "alice", List.of("devs"), "my-job", "dev"));
  }

  @Test
  @DisplayName("Default deny: rules that don't match yield no access")
  void defaultDeny_noMatchingRules() {
    ACLRule rule = allowRule("allow-bob", List.of("bob"), List.of(), List.of("*"), "other-job");
    assertFalse(
        EnvironmentACLChecker.hasAccess(
            List.of(rule), environmentGroups, "alice", List.of("devs"), "my-job", "dev"));
  }

  // ── Basic allow ───────────────────────────────────────────────

  @Nested
  @DisplayName("Allow rules")
  class AllowRules {

    @Test
    @DisplayName("Allow by exact user and exact environment")
    void allowByUserAndEnvironment() {
      ACLRule rule = allowRule("allow-alice", List.of("alice"), List.of(), List.of("dev"), "*");
      assertTrue(
          EnvironmentACLChecker.hasAccess(
              List.of(rule), environmentGroups, "alice", List.of(), "my-job", "dev"));
    }

    @Test
    @DisplayName("Allow by wildcard user")
    void allowByWildcardUser() {
      ACLRule rule = allowRule("allow-all", List.of("*"), List.of(), List.of("dev"), "*");
      assertTrue(
          EnvironmentACLChecker.hasAccess(
              List.of(rule), environmentGroups, "anyone", List.of(), "my-job", "dev"));
    }

    @Test
    @DisplayName("Allow by group membership")
    void allowByGroup() {
      ACLRule rule = allowRule("allow-devs", List.of(), List.of("developers"), List.of("dev"), "*");
      assertTrue(
          EnvironmentACLChecker.hasAccess(
              List.of(rule),
              environmentGroups,
              "alice",
              List.of("developers"),
              "my-job",
              "dev"));
    }

    @Test
    @DisplayName("Allow by wildcard group")
    void allowByWildcardGroup() {
      ACLRule rule = allowRule("allow-any-group", List.of(), List.of("*"), List.of("dev"), "*");
      assertTrue(
          EnvironmentACLChecker.hasAccess(
              List.of(rule),
              environmentGroups,
              "alice",
              List.of("some-group"),
              "my-job",
              "dev"));
    }

    @Test
    @DisplayName("Allow by wildcard environment")
    void allowByWildcardEnvironment() {
      ACLRule rule = allowRule("allow-all-envs", List.of("alice"), List.of(), List.of("*"), "*");
      assertTrue(
          EnvironmentACLChecker.hasAccess(
              List.of(rule), environmentGroups, "alice", List.of(), "my-job", "prod"));
    }
  }

  // ── Deny rules ────────────────────────────────────────────────

  @Nested
  @DisplayName("Deny rules")
  class DenyRules {

    @Test
    @DisplayName("Deny overrides allow for same user and environment")
    void denyOverridesAllow() {
      ACLRule allow = allowRule("allow-all", List.of("*"), List.of(), List.of("*"), "*");
      ACLRule deny = denyRule("deny-prod", List.of("alice"), List.of(), List.of("prod"), "*");

      List<ACLRule> rules = Arrays.asList(allow, deny);

      // Alice can access dev
      assertTrue(
          EnvironmentACLChecker.hasAccess(
              rules, environmentGroups, "alice", List.of(), "my-job", "dev"));
      // Alice cannot access prod
      assertFalse(
          EnvironmentACLChecker.hasAccess(
              rules, environmentGroups, "alice", List.of(), "my-job", "prod"));
    }

    @Test
    @DisplayName("Deny by group blocks access")
    void denyByGroup() {
      ACLRule allow = allowRule("allow-all", List.of("*"), List.of(), List.of("*"), "*");
      ACLRule deny = denyRule("deny-interns", List.of(), List.of("interns"), List.of("prod"), "*");

      List<ACLRule> rules = Arrays.asList(allow, deny);

      assertFalse(
          EnvironmentACLChecker.hasAccess(
              rules, environmentGroups, "bob", List.of("interns"), "my-job", "prod"));
    }

    @Test
    @DisplayName("Deny always wins over allow regardless of rule order")
    void denyAlwaysWins() {
      // Allow has higher priority than deny
      ACLRule allow = allowRule("allow-all", List.of("*"), List.of(), List.of("*"), "*");
      allow.setPriority(100);
      ACLRule deny = denyRule("deny-prod", List.of("alice"), List.of(), List.of("prod"), "*");
      deny.setPriority(1);

      // Deny-first evaluation means deny always wins regardless of priority
      assertFalse(
          EnvironmentACLChecker.hasAccess(
              Arrays.asList(allow, deny),
              environmentGroups,
              "alice",
              List.of(),
              "my-job",
              "prod"));
    }
  }

  // ── Priority ordering ─────────────────────────────────────────

  @Nested
  @DisplayName("Priority ordering")
  class PriorityOrdering {

    @Test
    @DisplayName("Higher priority deny rules are evaluated first")
    void higherPriorityDenyFirst() {
      // Low priority deny on specific env
      ACLRule denySpecific = denyRule("deny-dev", List.of("alice"), List.of(), List.of("dev"), "*");
      denySpecific.setPriority(10);

      // High priority allow on all
      ACLRule allowAll = allowRule("allow-all", List.of("*"), List.of(), List.of("*"), "*");
      allowAll.setPriority(100);

      // Despite allow having higher priority, deny-first evaluation blocks access
      assertFalse(
          EnvironmentACLChecker.hasAccess(
              Arrays.asList(denySpecific, allowAll),
              environmentGroups,
              "alice",
              List.of(),
              "my-job",
              "dev"));
    }
  }

  // ── Environment group matching ────────────────────────────────

  @Nested
  @DisplayName("Environment group matching")
  class EnvironmentGroupMatching {

    @Test
    @DisplayName("Allow by environment group name")
    void allowByEnvironmentGroup() {
      ACLRule rule = new ACLRule("allow-dev-group", "allow");
      rule.setUsers(List.of("alice"));
      rule.setJobs(List.of("*"));
      rule.setEnvironments(List.of()); // No direct env match
      rule.setEnvironmentGroups(List.of("development"));

      assertTrue(
          EnvironmentACLChecker.hasAccess(
              List.of(rule), environmentGroups, "alice", List.of(), "my-job", "dev"));
      assertTrue(
          EnvironmentACLChecker.hasAccess(
              List.of(rule), environmentGroups, "alice", List.of(), "my-job", "test"));
      // prod is not in development group
      assertFalse(
          EnvironmentACLChecker.hasAccess(
              List.of(rule), environmentGroups, "alice", List.of(), "my-job", "prod"));
    }

    @Test
    @DisplayName("Allow by wildcard environment group")
    void allowByWildcardEnvironmentGroup() {
      ACLRule rule = new ACLRule("allow-all-groups", "allow");
      rule.setUsers(List.of("alice"));
      rule.setJobs(List.of("*"));
      rule.setEnvironments(List.of());
      rule.setEnvironmentGroups(List.of("*"));

      assertTrue(
          EnvironmentACLChecker.hasAccess(
              List.of(rule), environmentGroups, "alice", List.of(), "my-job", "prod"));
    }
  }

  // ── Environment tag matching ──────────────────────────────────

  @Nested
  @DisplayName("Environment tag matching")
  class EnvironmentTagMatching {

    @Test
    @DisplayName("Allow by environment tag")
    void allowByTag() {
      ACLRule rule = new ACLRule("allow-nonprod", "allow");
      rule.setUsers(List.of("alice"));
      rule.setJobs(List.of("*"));
      rule.setEnvironments(List.of());
      rule.setEnvironmentTags(List.of("non-production"));

      // dev is in development group which has "non-production" tag
      assertTrue(
          EnvironmentACLChecker.hasAccess(
              List.of(rule), environmentGroups, "alice", List.of(), "my-job", "dev"));
      // prod is in production group which does NOT have "non-production" tag
      assertFalse(
          EnvironmentACLChecker.hasAccess(
              List.of(rule), environmentGroups, "alice", List.of(), "my-job", "prod"));
    }

    @Test
    @DisplayName("Deny by tag blocks critical environments")
    void denyByTag() {
      ACLRule allow = allowRule("allow-all", List.of("*"), List.of(), List.of("*"), "*");
      ACLRule deny = new ACLRule("deny-critical", "deny");
      deny.setUsers(List.of("*"));
      deny.setJobs(List.of("*"));
      deny.setEnvironments(List.of());
      deny.setEnvironmentTags(List.of("critical"));

      List<ACLRule> rules = Arrays.asList(allow, deny);

      // prod has "critical" tag — blocked
      assertFalse(
          EnvironmentACLChecker.hasAccess(
              rules, environmentGroups, "alice", List.of(), "my-job", "prod"));
      // dev does NOT have "critical" tag — allowed
      assertTrue(
          EnvironmentACLChecker.hasAccess(
              rules, environmentGroups, "alice", List.of(), "my-job", "dev"));
    }

    @Test
    @DisplayName("Wildcard tag matches all tagged environments")
    void wildcardTag() {
      ACLRule rule = new ACLRule("allow-any-tag", "allow");
      rule.setUsers(List.of("alice"));
      rule.setJobs(List.of("*"));
      rule.setEnvironments(List.of());
      rule.setEnvironmentTags(List.of("*"));

      assertTrue(
          EnvironmentACLChecker.hasAccess(
              List.of(rule), environmentGroups, "alice", List.of(), "my-job", "dev"));
      assertTrue(
          EnvironmentACLChecker.hasAccess(
              List.of(rule), environmentGroups, "alice", List.of(), "my-job", "prod"));
    }
  }

  // ── Job pattern matching ──────────────────────────────────────

  @Nested
  @DisplayName("Job pattern matching")
  class JobPatternMatching {

    @Test
    @DisplayName("Exact job name match")
    void exactJobMatch() {
      ACLRule rule = allowRule("allow-job", List.of("alice"), List.of(), List.of("*"), "deploy-app");
      assertTrue(
          EnvironmentACLChecker.hasAccess(
              List.of(rule), environmentGroups, "alice", List.of(), "deploy-app", "dev"));
      assertFalse(
          EnvironmentACLChecker.hasAccess(
              List.of(rule), environmentGroups, "alice", List.of(), "other-job", "dev"));
    }

    @Test
    @DisplayName("Regex job pattern match")
    void regexJobMatch() {
      ACLRule rule =
          allowRule("allow-deploy", List.of("alice"), List.of(), List.of("*"), "deploy-.*");
      assertTrue(
          EnvironmentACLChecker.hasAccess(
              List.of(rule), environmentGroups, "alice", List.of(), "deploy-app", "dev"));
      assertTrue(
          EnvironmentACLChecker.hasAccess(
              List.of(rule), environmentGroups, "alice", List.of(), "deploy-service", "dev"));
      assertFalse(
          EnvironmentACLChecker.hasAccess(
              List.of(rule), environmentGroups, "alice", List.of(), "build-app", "dev"));
    }

    @Test
    @DisplayName("Wildcard job match")
    void wildcardJobMatch() {
      ACLRule rule = allowRule("allow-all-jobs", List.of("alice"), List.of(), List.of("*"), "*");
      assertTrue(
          EnvironmentACLChecker.hasAccess(
              List.of(rule), environmentGroups, "alice", List.of(), "anything", "dev"));
    }

    @Test
    @DisplayName("Invalid regex falls back to exact match")
    void invalidRegexFallsBackToExact() {
      ACLRule rule =
          allowRule("allow-bracket", List.of("alice"), List.of(), List.of("*"), "[invalid");
      // Invalid regex, falls back to exact string match
      assertTrue(
          EnvironmentACLChecker.hasAccess(
              List.of(rule), environmentGroups, "alice", List.of(), "[invalid", "dev"));
      assertFalse(
          EnvironmentACLChecker.hasAccess(
              List.of(rule), environmentGroups, "alice", List.of(), "other", "dev"));
    }
  }

  // ── Complex scenarios ─────────────────────────────────────────

  @Nested
  @DisplayName("Complex real-world scenarios")
  class ComplexScenarios {

    @Test
    @DisplayName("Devs can deploy to dev/test, only ops can deploy to prod")
    void realisticOrgPolicy() {
      ACLRule allowDevs =
          allowRule(
              "allow-devs-nonprod", List.of(), List.of("developers"), List.of("dev", "test"), "*");
      ACLRule allowOps =
          allowRule("allow-ops-all", List.of(), List.of("ops"), List.of("*"), "*");
      ACLRule denyDevsProd =
          denyRule("deny-devs-prod", List.of(), List.of("developers"), List.of("prod"), "*");

      List<ACLRule> rules = Arrays.asList(allowDevs, allowOps, denyDevsProd);

      // Developer can access dev
      assertTrue(
          EnvironmentACLChecker.hasAccess(
              rules, environmentGroups, "dev1", List.of("developers"), "deploy", "dev"));
      // Developer cannot access prod (deny rule)
      assertFalse(
          EnvironmentACLChecker.hasAccess(
              rules, environmentGroups, "dev1", List.of("developers"), "deploy", "prod"));
      // Ops can access prod
      assertTrue(
          EnvironmentACLChecker.hasAccess(
              rules, environmentGroups, "ops1", List.of("ops"), "deploy", "prod"));
      // Ops can also access dev
      assertTrue(
          EnvironmentACLChecker.hasAccess(
              rules, environmentGroups, "ops1", List.of("ops"), "deploy", "dev"));
      // Unknown user has no access
      assertFalse(
          EnvironmentACLChecker.hasAccess(
              rules, environmentGroups, "stranger", List.of("unknown"), "deploy", "dev"));
    }

    @Test
    @DisplayName("Multiple group membership grants combined access")
    void multipleGroupMembership() {
      ACLRule allowDevs =
          allowRule("allow-devs-dev", List.of(), List.of("developers"), List.of("dev"), "*");
      ACLRule allowQA =
          allowRule("allow-qa-test", List.of(), List.of("qa"), List.of("test"), "*");

      List<ACLRule> rules = Arrays.asList(allowDevs, allowQA);

      // User in both groups can access both environments
      assertTrue(
          EnvironmentACLChecker.hasAccess(
              rules,
              environmentGroups,
              "alice",
              List.of("developers", "qa"),
              "my-job",
              "dev"));
      assertTrue(
          EnvironmentACLChecker.hasAccess(
              rules,
              environmentGroups,
              "alice",
              List.of("developers", "qa"),
              "my-job",
              "test"));
    }

    @Test
    @DisplayName("Environment not in any group gets no group/tag matches")
    void unknownEnvironment() {
      ACLRule rule = new ACLRule("allow-group", "allow");
      rule.setUsers(List.of("alice"));
      rule.setJobs(List.of("*"));
      rule.setEnvironments(List.of());
      rule.setEnvironmentGroups(List.of("development"));

      // "unknown-env" is not in any group
      assertFalse(
          EnvironmentACLChecker.hasAccess(
              List.of(rule), environmentGroups, "alice", List.of(), "my-job", "unknown-env"));
    }
  }

  // ── matchesJob unit tests ─────────────────────────────────────

  @Nested
  @DisplayName("matchesJob")
  class MatchesJobTests {

    @Test
    void wildcardMatchesEverything() {
      ACLRule rule = new ACLRule("test", "allow");
      rule.setJobs(List.of("*"));
      assertTrue(EnvironmentACLChecker.matchesJob(rule, "anything"));
    }

    @Test
    void emptyJobListMatchesNothing() {
      ACLRule rule = new ACLRule("test", "allow");
      rule.setJobs(List.of());
      assertFalse(EnvironmentACLChecker.matchesJob(rule, "anything"));
    }

    @Test
    void multiplePatterns_anyMatch() {
      ACLRule rule = new ACLRule("test", "allow");
      rule.setJobs(List.of("deploy-.*", "build-.*"));
      assertTrue(EnvironmentACLChecker.matchesJob(rule, "deploy-app"));
      assertTrue(EnvironmentACLChecker.matchesJob(rule, "build-app"));
      assertFalse(EnvironmentACLChecker.matchesJob(rule, "test-app"));
    }

    @Test
    void folderPathRegex() {
      ACLRule rule = new ACLRule("test", "allow");
      rule.setJobs(List.of("projects/web/.*"));
      assertTrue(EnvironmentACLChecker.matchesJob(rule, "projects/web/deploy"));
      assertFalse(EnvironmentACLChecker.matchesJob(rule, "projects/api/deploy"));
    }
  }

  // ── matchesUserOrGroup unit tests ─────────────────────────────

  @Nested
  @DisplayName("matchesUserOrGroup")
  class MatchesUserOrGroupTests {

    @Test
    void exactUserMatch() {
      ACLRule rule = new ACLRule("test", "allow");
      rule.setUsers(List.of("alice"));
      rule.setGroups(List.of());
      assertTrue(EnvironmentACLChecker.matchesUserOrGroup(rule, "alice", List.of()));
      assertFalse(EnvironmentACLChecker.matchesUserOrGroup(rule, "bob", List.of()));
    }

    @Test
    void wildcardUserMatchesAnyone() {
      ACLRule rule = new ACLRule("test", "allow");
      rule.setUsers(List.of("*"));
      rule.setGroups(List.of());
      assertTrue(EnvironmentACLChecker.matchesUserOrGroup(rule, "anyone", List.of()));
    }

    @Test
    void groupMatch() {
      ACLRule rule = new ACLRule("test", "allow");
      rule.setUsers(List.of());
      rule.setGroups(List.of("developers"));
      assertTrue(
          EnvironmentACLChecker.matchesUserOrGroup(rule, "alice", List.of("developers")));
      assertFalse(
          EnvironmentACLChecker.matchesUserOrGroup(rule, "alice", List.of("marketing")));
    }

    @Test
    void noUsersAndNoGroupsMatchesNothing() {
      ACLRule rule = new ACLRule("test", "allow");
      rule.setUsers(List.of());
      rule.setGroups(List.of());
      assertFalse(EnvironmentACLChecker.matchesUserOrGroup(rule, "alice", List.of("devs")));
    }
  }

  // ── Helper methods ────────────────────────────────────────────

  private ACLRule allowRule(
      String name,
      List<String> users,
      List<String> groups,
      List<String> environments,
      String jobPattern) {
    ACLRule rule = new ACLRule(name, "allow");
    rule.setUsers(users);
    rule.setGroups(groups);
    rule.setEnvironments(environments);
    rule.setJobs(List.of(jobPattern));
    return rule;
  }

  private ACLRule denyRule(
      String name,
      List<String> users,
      List<String> groups,
      List<String> environments,
      String jobPattern) {
    ACLRule rule = new ACLRule(name, "deny");
    rule.setUsers(users);
    rule.setGroups(groups);
    rule.setEnvironments(environments);
    rule.setJobs(List.of(jobPattern));
    return rule;
  }
}
