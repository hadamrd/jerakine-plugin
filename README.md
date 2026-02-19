# Jerakin - Configuration-Driven Deployment Framework for Jenkins

A Jenkins plugin that replaces manually configured jobs with **template-based, YAML-driven deployments**. Define deployment patterns once as templates, create job instances from them, and let Jerakin handle environment access control, credential injection, and infrastructure selection automatically.

## Quick Start

1. Install the `.hpi` file from [Releases](https://github.com/hadamrd/jerakine-plugin/releases)
2. Add this to your Jenkins Configuration as Code (JCasC):

```yaml
unclassified:
  # Define your environments and who can access them
  environmentACL:
    environmentGroups:
      - name: "production"
        environments: ["prod-eu", "prod-us"]
        nodeLabels: ["prod-agent"]
        sshCredentialId: "prod-ssh-key"
        tags: ["production", "critical"]
      - name: "development"
        environments: ["dev", "staging"]
        nodeLabels: ["dev-agent"]
        sshCredentialId: "dev-ssh-key"
    aclRules:
      - name: "ops-prod-access"
        type: "allow"
        priority: 300
        jobs: ["*"]
        environmentGroups: ["production"]
        groups: ["ops"]
      - name: "devs-dev-access"
        type: "allow"
        priority: 200
        jobs: ["*"]
        environmentGroups: ["development"]
        groups: ["developers"]

  # Define deployment templates and jobs
  jerakinDeployments:
    templates:
      - name: "ansible-deploy"
        params:
          - name: "environment"
            type: "environment"
          - name: "playbook"
            type: "string"
        script: |
          node(deployParams.nodeLabels) {
            ansibleProject(projectId: 'infra', ref: 'main') {
              ansiblePlaybook(playbook: deployParams.playbook, envName: deployParams.environment)
            }
          }
    jobs:
      - id: "deploy-web"
        name: "Deploy Web Servers"
        category: "Infrastructure"
        templateName: "ansible-deploy"
        params:
          - name: "playbook"
            value: "webserver.yml"  # Fixed — users only pick environment
```

3. Jerakin auto-generates Jenkins jobs under `projects/<category>/JerakinJob_<id>`

## How It Works

**Three concepts:**

| Concept | What it does |
|---|---|
| **Environment Groups** | Map environments to infrastructure: node labels, SSH keys, vault credentials, access tags |
| **Templates** | Reusable deployment patterns with typed parameters and a pipeline script |
| **Jobs** | Instances of templates with fixed parameter overrides |

**Parameter precedence** (highest wins):
1. Job-level fixed params (from YAML config)
2. Step config (from `resolveDeployParams()` call)
3. UI params (what the user fills in)

Parameters fixed by the job config are automatically hidden from the build UI.

## Generated Job Structure

```
projects/
├── Infrastructure/
│   ├── JerakinJob_deploy-web         # User only sees 'environment'
│   └── JerakinJob_deploy-databases   # User only sees 'environment'
└── Applications/
    └── JerakinJob_app-deploy         # User sees 'environment' + 'version'
```

## Pipeline Steps

### `resolveDeployParams`
Resolves parameters with precedence and adds infrastructure context:
```groovy
def deployParams = resolveDeployParams(jobId: 'deploy-web')
// → {environment: "prod-eu", playbook: "webserver.yml", nodeLabels: "prod-agent"}
```

### `ansibleProject`
Creates an isolated Ansible execution environment (Git clone + container):
```groovy
ansibleProject(projectId: 'infra', ref: deployParams.ref) {
    ansiblePlaybook(playbook: 'site.yml', envName: deployParams.environment)
}
```

### `checkEnvironmentACL`
Validates environment access and returns credential info:
```groovy
def acl = checkEnvironmentACL(deployParams.environment)
```

## Security Model

- **Deny-first ACL**: Users only see environments they're authorized for
- **Priority-based rules**: Higher priority rules evaluated first, deny always wins
- **Multiple matching**: Rules match by user, group, environment, environment group, or tag
- **Credential isolation**: SSH keys and vault passwords are per-environment-group
- **Infrastructure isolation**: Jobs run on environment-appropriate nodes

## Modules

| Module | Purpose |
|---|---|
| `deployment` | Template engine, job generation, parameter resolution |
| `environment` | ACL rules, environment groups, credential mapping |
| `ansible` | Ansible project registry, containerized playbook execution |
| `ssh` | SSH environment definitions, connection pooling |
| `container` | Docker container lifecycle with reference counting |

## Development

```bash
make build    # Build plugin (skip tests)
make test     # Run unit tests
make verify   # Full verification (tests + spotless + spotbugs)
make run      # Start Jenkins in dev mode on port 8080
```

Requires Java 21+ and Maven 3.9+.

## License

MIT
