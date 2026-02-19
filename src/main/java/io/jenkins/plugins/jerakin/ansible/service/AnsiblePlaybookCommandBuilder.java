package io.jenkins.plugins.jerakin.ansible.service;

import io.jenkins.plugins.jerakin.ansible.model.AnsibleVault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Builder for ansible-playbook commands. Handles command construction with proper escaping and
 * parameter formatting.
 */
public class AnsiblePlaybookCommandBuilder {

    /**
     * Pattern for safe shell identifiers: alphanumeric, hyphens, underscores, dots, and forward
     * slashes (for paths). Rejects shell metacharacters like ;, $, `, |, &, etc.
     */
    static final Pattern SAFE_IDENTIFIER = Pattern.compile("[a-zA-Z0-9_./@:-]+");

    private String playbook;
    private String user = "ansible";
    private String inventory;
    private String projectRoot;
    private Map<String, Object> extraVars;
    private String options;
    private VaultManager vaultManager;

    public AnsiblePlaybookCommandBuilder playbook(String playbook) {
        this.playbook = playbook;
        return this;
    }

    public AnsiblePlaybookCommandBuilder vaultManager(VaultManager vaultManager) {
        this.vaultManager = vaultManager;
        return this;
    }

    public AnsiblePlaybookCommandBuilder user(String user) {
        this.user = user != null ? user : "root";
        return this;
    }

    public AnsiblePlaybookCommandBuilder inventory(String inventory) {
        this.inventory = inventory;
        return this;
    }

    public AnsiblePlaybookCommandBuilder projectRoot(String projectRoot) {
        this.projectRoot = projectRoot;
        return this;
    }

    public AnsiblePlaybookCommandBuilder extraVars(Map<String, Object> extraVars) {
        this.extraVars = extraVars;
        return this;
    }

    public AnsiblePlaybookCommandBuilder options(String options) {
        this.options = options;
        return this;
    }

    /** Build the complete command including directory change */
    public String buildCmd() {
        validate();

        List<String> commandParts = new ArrayList<>();

        commandParts.add("set -e");

        // Change to project directory
        if (projectRoot != null) {
            requireSafeIdentifier(projectRoot, "projectRoot");
            commandParts.add("cd " + projectRoot);
        }

        // Build ansible-playbook command
        commandParts.add(buildAnsibleCommand());

        return String.join(" && ", commandParts);
    }

    /** Build just the ansible-playbook command (without cd) */
    public String buildAnsibleCommand() {
        validate();

        List<String> cmd = new ArrayList<>();

        // Base command — playbook path is validated by validate()
        cmd.add("ansible-playbook " + playbook);

        // User — validated by validate()
        cmd.add("-u " + user);

        // Inventory
        if (inventory != null) {
            requireSafeIdentifier(inventory, "inventory");
            cmd.add("-i '" + inventory + "'");
        }

        // Vault IDs - handle internally
        if (vaultManager != null && vaultManager.hasVaults()) {
            for (AnsibleVault vault : vaultManager.getSetupVaults()) {
                String passwordFile = vaultManager.getVaultFilePath(vault);
                cmd.add("--vault-id " + vault.getId() + "@" + passwordFile);
            }
        }

        // Standard extra vars
        cmd.add("-e 'running_from_jenkins=true'");

        // Custom extra vars
        if (extraVars != null) {
            for (Map.Entry<String, Object> var : extraVars.entrySet()) {
                String escapedValue = escapeValue(var.getValue().toString());
                cmd.add("-e '" + var.getKey() + "=" + escapedValue + "'");
            }
        }

        // Additional options
        if (options != null && !options.trim().isEmpty()) {
            cmd.add(options.trim());
        }

        return String.join(" ", cmd);
    }

    /** Get command as separate parts for easier testing/debugging */
    public List<String> buildCommandParts() {
        List<String> parts = new ArrayList<>();

        if (projectRoot != null) {
            parts.add("cd " + projectRoot);
        }

        parts.add(buildAnsibleCommand());

        return parts;
    }

    /**
     * Escape a value for safe embedding inside single-quoted shell strings. In POSIX shell, nothing
     * is interpreted inside single quotes except the closing single quote itself. The standard
     * idiom to embed a literal single quote is: end the current single-quoted string, add an
     * escaped single quote, and start a new single-quoted string: {@code 'it'\''s safe'}
     */
    static String escapeValue(String value) {
        if (value == null) {
            return "";
        }

        // Inside single quotes, only ' needs escaping. The escape idiom is: '\''
        return value.replace("'", "'\\''");
    }

    /**
     * Validate that a string contains only safe characters for use as a shell identifier (path,
     * username, etc.). Rejects shell metacharacters that could enable command injection.
     */
    static void requireSafeIdentifier(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        if (!SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    fieldName + " contains unsafe characters: " + value
                            + " (only alphanumeric, hyphens, underscores, dots, slashes allowed)");
        }
    }

    /** Create a summary of the command for logging */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Playbook: ").append(playbook);
        summary.append(", User: ").append(user);

        if (inventory != null) {
            summary.append(", Inventory: ").append(inventory);
        }

        if (extraVars != null && !extraVars.isEmpty()) {
            summary.append(", Extra vars: ").append(extraVars.keySet());
        }

        if (options != null && !options.trim().isEmpty()) {
            summary.append(", Options: ").append(options);
        }

        return summary.toString();
    }

    /** Validate that all required parameters are set and safe */
    public void validate() {
        if (playbook == null || playbook.trim().isEmpty()) {
            throw new IllegalArgumentException("Playbook is required");
        }
        requireSafeIdentifier(playbook, "playbook");

        if (user == null || user.trim().isEmpty()) {
            throw new IllegalArgumentException("User is required");
        }
        requireSafeIdentifier(user, "user");

        if (extraVars != null) {
            for (String key : extraVars.keySet()) {
                requireSafeIdentifier(key, "extra var key");
            }
        }
    }
}
