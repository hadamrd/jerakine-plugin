# Enhanced Makefile for Jerakin Plugin Distribution
PLUGIN_VERSION = $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
PLUGIN_NAME = jerakin
RELEASE_DIR = releases
HPI_FILE = target/$(PLUGIN_NAME)-plugin.hpi

build:
	@echo "Building jerakin plugin version $(PLUGIN_VERSION)..."
	mvn clean package -DskipTests

test:
	@echo "Running tests..."
	mvn test

verify:
	@echo "Running full verification (tests + spotless + spotbugs)..."
	mvn verify -Dspotless.check.skip=false -Dspotbugs.skip=false

# Release builds
release-build:
	@echo "Building release version $(PLUGIN_VERSION)..."
	mvn clean package -Dchangelist= -DskipTests -Dspotbugs.skip=true
	mkdir -p $(RELEASE_DIR)
	cp target/*.hpi $(RELEASE_DIR)/$(PLUGIN_NAME)-plugin-$(PLUGIN_VERSION).hpi
	@echo "Release artifact: $(RELEASE_DIR)/$(PLUGIN_NAME)-plugin-$(PLUGIN_VERSION).hpi"

github-release: release-build
	gh release create v$(PLUGIN_VERSION) $(RELEASE_DIR)/$(PLUGIN_NAME)-plugin-$(PLUGIN_VERSION).hpi \
		--title "Jerakin Plugin v$(PLUGIN_VERSION)" \
		--notes "Configuration-driven Jenkins deployment framework with job templating and environment access control."

release: verify release-build github-release
	@echo "Jerakin plugin $(PLUGIN_VERSION) released!"

run:
	@echo "Starting Jenkins in development mode..."
	export MAVEN_OPTS="-Dhost=0.0.0.0 -Dport=8080 --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.util.concurrent=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED"
	mvn clean compile hpi:run

# Version management
version-bump:
	@read -p "Enter new version (current: $(PLUGIN_VERSION)): " version; \
	mvn versions:set -DnewVersion=$$version -DgenerateBackupPoms=false

help:
	@echo "Development commands:"
	@echo "  make build            - Build plugin (skip tests)"
	@echo "  make test             - Run unit tests"
	@echo "  make verify           - Full verification (tests + spotless + spotbugs)"
	@echo "  make run              - Start Jenkins in dev mode"
	@echo ""
	@echo "Release commands:"
	@echo "  make release-build    - Build release version"
	@echo "  make github-release   - Create GitHub release"
	@echo "  make release          - Complete release workflow"
	@echo "  make version-bump     - Update version number"
