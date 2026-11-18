-- Topics and the first book list (same books as the store design)

INSERT INTO topics (key, name, description, sort_order) VALUES
    ('Cloud', 'Cloud platforms', 'Run the app on AWS, Azure, GCP or DigitalOcean.', 1),
    ('VCS', 'Version control', 'Git and where your code lives.', 2),
    ('CI/CD', 'CI/CD and GitOps', 'From commit to live, automatically.', 3),
    ('Containers', 'Containers and orchestration', 'Package it. Orchestrate it. Route it.', 4),
    ('IaC', 'Infrastructure and configuration', 'Servers from files, not clicks.', 5),
    ('Monitoring', 'Monitoring, logging and tracing', 'Know before users do.', 6),
    ('Security', 'DevSecOps', 'Scan, secure and keep secrets safe.', 7),
    ('Build', 'Build and artifacts', 'Compile, package and store releases.', 8),
    ('Testing', 'Testing and planning', 'Plan the work and prove it works.', 9),
    ('Scripting', 'Linux and scripting', 'The foundation of every pipeline.', 10),
    ('Data', 'Databases and messaging', 'The data layer behind the app.', 11);

INSERT INTO books (id, title, subtitle, topic_key, logo_key, color1, color2, mrp_paise, price_paise, stock, pages, highlights) VALUES
    (1, 'AWS for DevOps', 'EKS, RDS and CloudWatch', 'Cloud', 'aws', '#37475A', '#0b1018', 109900, 79900, 14, 420, 'VPC, EKS and RDS wired correctly
Cost control from day one
IAM least-privilege patterns'),
    (2, 'Azure for DevOps', 'AKS with Azure Pipelines', 'Cloud', 'azure', '#0078D4', '#00264d', 109900, 79900, 9, 400, 'AKS with Azure Pipelines
Managed MySQL done right
Key Vault for secrets'),
    (3, 'GCP for DevOps', 'GKE, Cloud Build, Atlas', 'Cloud', 'gcp', '#34A853', '#0a3a1a', 109900, 79900, 4, 390, 'GKE with Cloud Build
MongoDB Atlas integration
Workload identity, no key files'),
    (4, 'Jenkins CI/CD', 'Controller, Agents, Production', 'CI/CD', 'jenkins', '#D33833', '#4a0a08', 89900, 54900, 18, 360, 'Pipeline-as-code with a Jenkinsfile
Scale builds with agents
Automatic rollback when checks fail'),
    (5, 'GitHub Actions', 'Workflows that ship code', 'CI/CD', 'gha', '#24292F', '#0b0d10', 79900, 49900, 21, 300, 'Reusable workflows and matrix builds
Secrets and OIDC to the cloud
Deploy on every merge'),
    (6, 'GitLab CI/CD', 'Pipelines, runners, environments', 'CI/CD', 'gitlab', '#FC6D26', '#5a1f00', 79900, 49900, 11, 310, 'Stages and artifacts explained
Self-hosted runners at scale
Review apps for every merge request'),
    (7, 'GitOps with Argo CD', 'Git as the source of truth', 'CI/CD', 'argo', '#EF7B4D', '#3d1606', 89900, 59900, 6, 340, 'Declarative deployments from Git
Automatic drift detection
Rollback by reverting a commit'),
    (8, 'Docker in Production', 'Ship small, safe images', 'Containers', 'docker', '#2496ED', '#0B3B66', 89900, 54900, 25, 380, 'Shrink images with multi-stage builds
Debug a crashing container
Harden images: non-root, scanned layers'),
    (9, 'Kubernetes Field Guide', 'Run it. Scale it. Fix it.', 'Containers', 'k8s', '#326CE5', '#12206b', 119900, 69900, 16, 480, 'Zero-downtime rolling deployments
Read pod events like a detective
Ingress and Services that scale'),
    (10, 'Helm Charts', 'Package once, deploy anywhere', 'Containers', 'helm', '#0F9D9A', '#053b3a', 69900, 44900, 13, 280, 'Versioned, reusable charts
Values per environment
Safe upgrades and rollbacks'),
    (11, 'Prometheus Monitoring', 'Metrics and alerts that matter', 'Monitoring', 'prom', '#E6522C', '#4a1305', 79900, 49900, 10, 320, 'PromQL from basics to advanced
Alertmanager routing
Monitor Kubernetes and Spring Boot'),
    (12, 'Grafana Dashboards', 'See your system think', 'Monitoring', 'graf', '#F46800', '#4a2100', 74900, 44900, 8, 290, 'Dashboards your team will use
Loki logs next to metrics
Alert panels with context'),
    (13, 'Terraform Infrastructure', 'Cloud as code, safely', 'IaC', 'tf', '#7B42BC', '#2a1250', 99900, 64900, 12, 410, 'Review every change with plan
Remote state with locking
Reusable modules across clouds'),
    (14, 'Ansible Automation', 'Configure servers at scale', 'IaC', 'ans', '#1A1A1A', '#7a0d0d', 79900, 49900, 7, 300, 'Idempotent playbooks
Roles and inventories
Agentless configuration over SSH'),
    (15, 'Bash Scripting for DevOps', 'Automate the boring parts', 'Scripting', 'bash', '#2D3748', '#0b0f16', 59900, 34900, 30, 240, 'Safe scripts: set -euo pipefail
Log parsing and cron jobs
Deploy helpers you actually reuse'),
    (16, 'Python for DevOps', 'Automation, APIs and tooling', 'Scripting', 'py', '#3776AB', '#0f2a47', 79900, 44900, 19, 350, 'Automate cloud with boto3
Build small internal CLIs
Test your automation'),
    (17, 'DigitalOcean for DevOps', 'Droplets, DOKS, simple scaling', 'Cloud', 'do', '#0080FF', '#003161', 74900, 49900, 7, 461, 'Fast setup for small teams
Managed Kubernetes (DOKS)
Predictable pricing'),
    (18, 'Git Mastery', 'Branching, rebasing, recovery', 'VCS', 'git', '#F05032', '#5b1e13', 59900, 34900, 14, 474, 'Branching strategies for teams
Fix mistakes with reflog
Clean history with rebase'),
    (19, 'Collaboration with GitHub', 'Pull requests and protections', 'VCS', 'github', '#2B3137', '#101315', 64900, 39900, 21, 247, 'Pull request reviews that work
Branch protection rules
Automate with Actions'),
    (20, 'Bitbucket Pipelines', 'CI inside Bitbucket', 'VCS', 'bitbucket', '#2684FF', '#0e3261', 59900, 34900, 5, 260, 'Pipelines in one YAML file
Jira links for traceability
Deployment environments'),
    (21, 'CircleCI Workflows', 'Orbs, caching, parallelism', 'CI/CD', 'circleci', '#343434', '#141414', 74900, 44900, 12, 273, 'Orbs and reusable config
Parallelism for fast tests
Caching that saves minutes'),
    (22, 'Azure DevOps Services', 'Boards, Repos, Pipelines', 'CI/CD', 'azdo', '#0078D7', '#002e52', 99900, 59900, 19, 286, 'Boards, Repos and Pipelines together
YAML multi-stage pipelines
Release gates and approvals'),
    (23, 'TeamCity Build Server', 'Build chains and agents', 'CI/CD', 'teamcity', '#2B2F3A', '#101216', 84900, 54900, 3, 299, 'Build chains and snapshot dependencies
Kotlin DSL configs
Flaky test detection'),
    (24, 'Tekton Pipelines', 'Cloud-native CI on Kubernetes', 'CI/CD', 'tekton', '#FD495C', '#601c23', 94900, 59900, 10, 312, 'Pipelines as Kubernetes CRDs
Reusable Tasks
Runs on any cluster'),
    (25, 'GitOps with Flux', 'Pull-based delivery', 'CI/CD', 'flux', '#5468FF', '#202861', 94900, 59900, 17, 325, 'Pull-based continuous delivery
Kustomize and Helm support
Image automation'),
    (26, 'Podman Containers', 'Daemonless and rootless', 'Containers', 'podman', '#892CA0', '#34113d', 74900, 44900, 24, 338, 'Daemonless, rootless containers
Pods like Kubernetes
Drop-in Docker alternative'),
    (27, 'Istio Service Mesh', 'Traffic, security, tracing', 'Containers', 'istio', '#466BB0', '#1b2943', 119900, 69900, 8, 351, 'mTLS between all services
Canary traffic splitting
Tracing out of the box'),
    (28, 'NGINX Essentials', 'Proxy, TLS, load balancing', 'Containers', 'nginx', '#009639', '#003916', 69900, 39900, 15, 364, 'Reverse proxy and load balancing
TLS and caching
Ingress controller basics'),
    (29, 'Pulumi with Code', 'Infrastructure in real languages', 'IaC', 'pulumi', '#8A3391', '#341337', 94900, 59900, 22, 377, 'Infrastructure in real languages
Reuse loops and functions
State and secrets handled'),
    (30, 'Packer Machine Images', 'Golden images for any cloud', 'IaC', 'packer', '#02A8EF', '#01405b', 74900, 44900, 6, 390, 'Golden images for every cloud
Pairs with Ansible
Faster, safer instance boots'),
    (31, 'Vagrant Dev Environments', 'Same dev box for everyone', 'IaC', 'vagrant', '#1868F2', '#09285c', 59900, 34900, 13, 403, 'Same dev box for everyone
Boxes and provisioners
Test infra locally'),
    (32, 'Chef Infrastructure', 'Cookbooks and policy', 'IaC', 'chef', '#F09820', '#5b3a0c', 89900, 54900, 20, 416, 'Cookbooks and recipes
Test with Test Kitchen
Manage fleets with policy'),
    (33, 'Puppet Configuration', 'Manifests and modules', 'IaC', 'puppet', '#FFAE1A', '#61420a', 89900, 54900, 4, 429, 'Declarative manifests
Modules from Puppet Forge
Drift correction at scale'),
    (34, 'Elasticsearch and ELK', 'Search and log analytics', 'Monitoring', 'elastic', '#00BFB3', '#004944', 119900, 69900, 11, 442, 'Index and search logs fast
Mappings and analyzers
Scale shards safely'),
    (35, 'Kibana Dashboards', 'Explore and visualize logs', 'Monitoring', 'kibana', '#F04E98', '#5b1e3a', 79900, 49900, 18, 455, 'Discover and Lens made simple
Alert on log patterns
Dashboards for on-call'),
    (36, 'Logstash Pipelines', 'Parse, enrich, ship', 'Monitoring', 'logstash', '#FEC514', '#614b08', 74900, 44900, 25, 468, 'Input, filter, output explained
Grok patterns
Reliable delivery with queues'),
    (37, 'Fluentd Log Collection', 'The unified logging layer', 'Monitoring', 'fluentd', '#0E83C8', '#05324c', 74900, 44900, 9, 241, 'Unified logging layer
Plugins for every sink
Kubernetes logging patterns'),
    (38, 'Datadog Observability', 'APM, logs, metrics, SLOs', 'Monitoring', 'datadog', '#632CA6', '#26113f', 119900, 69900, 16, 254, 'APM, logs and metrics together
Monitors and SLOs
Tag strategy that scales'),
    (39, 'New Relic Monitoring', 'Full-stack observability', 'Monitoring', 'newrelic', '#008C99', '#00353a', 99900, 59900, 23, 267, 'Full-stack observability
NRQL queries
Alerts tied to SLOs'),
    (40, 'Splunk for Ops', 'Search machine data', 'Monitoring', 'splunk', '#2F3A4A', '#12161c', 134900, 79900, 7, 280, 'Search processing language
Dashboards and alerts
Ops and security use cases'),
    (41, 'Tracing with Jaeger', 'Follow a request end to end', 'Monitoring', 'jaeger', '#1C9AB5', '#0b3b45', 89900, 54900, 14, 293, 'Trace requests across services
Find slow spans
OpenTelemetry integration'),
    (42, 'SonarQube Code Quality', 'Quality gates for every PR', 'Security', 'sonar', '#4E9BCD', '#1e3b4e', 79900, 49900, 21, 306, 'Quality gates for every PR
Spot bugs and smells early
Security hotspots'),
    (43, 'HashiCorp Vault', 'Secrets without static keys', 'Security', 'vault', '#2D2A1E', '#11100b', 119900, 69900, 5, 319, 'Dynamic secrets, no static keys
Encryption as a service
Policies and audit logs'),
    (44, 'Trivy Security Scanning', 'Images, repos and IaC', 'Security', 'trivy', '#1904DA', '#0a0253', 64900, 39900, 12, 332, 'Scan images, repos and IaC
Fail builds on critical CVEs
SBOM generation'),
    (45, 'Snyk Developer Security', 'Fix issues in the PR', 'Security', 'snyk', '#4C4A73', '#1d1c2c', 84900, 49900, 19, 345, 'Dependency and code scanning
Fix PRs automatically
Container and IaC checks'),
    (46, 'Maven for Java Builds', 'Lifecycle and dependencies', 'Build', 'maven', '#C71A36', '#4c0a15', 74900, 44900, 3, 358, 'Lifecycle and dependencies
Multi-module projects
Reproducible builds'),
    (47, 'Gradle Build Automation', 'Fast incremental builds', 'Build', 'gradle', '#1B6B7A', '#0a292e', 74900, 44900, 10, 371, 'Fast incremental builds
Build cache
Kotlin DSL scripts'),
    (48, 'JFrog Artifactory', 'Universal artifact repository', 'Build', 'jfrog', '#40BE46', '#18481b', 99900, 59900, 17, 384, 'Universal artifact repository
Promote builds across stages
Docker and Maven registries'),
    (49, 'Sonatype Nexus Repository', 'Private repos for every format', 'Build', 'nexus', '#2B2D5C', '#101123', 89900, 54900, 24, 397, 'Private Maven, npm, Docker repos
Proxy and cache central repos
Cleanup policies'),
    (50, 'Linux for DevOps', 'Processes, files, systemd', 'Scripting', 'linux', '#3A3A3A', '#161616', 94900, 54900, 8, 410, 'Processes, files and permissions
systemd and journald
Troubleshoot a slow server'),
    (51, 'Go for DevOps', 'Fast CLI tools and services', 'Scripting', 'go', '#00ADD8', '#004252', 84900, 49900, 15, 423, 'Build fast CLI tools
Concurrency made simple
Single static binaries'),
    (52, 'PostgreSQL in Production', 'Indexes, backups, replication', 'Data', 'postgres', '#336791', '#132737', 104900, 64900, 22, 436, 'Indexes and query plans
Backups and replication
Tuning for real traffic'),
    (53, 'MySQL for Engineers', 'InnoDB, replication, tuning', 'Data', 'mysql', '#00758F', '#002c36', 89900, 54900, 6, 449, 'InnoDB and indexing
Replication and failover
Slow query tuning'),
    (54, 'MongoDB Atlas Guide', 'Documents that scale', 'Data', 'mongo', '#47A248', '#1b3e1b', 99900, 59900, 13, 462, 'Document modelling that scales
Aggregation pipelines
Atlas security and backups'),
    (55, 'Redis Caching', 'Cache patterns that work', 'Data', 'redis', '#DC382D', '#541511', 74900, 44900, 20, 475, 'Cache patterns that work
Expiry and eviction
Pub/Sub and queues'),
    (56, 'Apache Kafka Streaming', 'Events at scale', 'Data', 'kafka', '#3A3A3A', '#161616', 124900, 74900, 4, 248, 'Topics, partitions and offsets
Consumer groups
Event-driven microservices'),
    (57, 'RabbitMQ Messaging', 'Queues, exchanges, retries', 'Data', 'rabbit', '#FF6600', '#612700', 74900, 44900, 11, 261, 'Exchanges, queues and bindings
Retries and dead letters
Reliable async work'),
    (58, 'AWS CloudFormation', 'Stacks, change sets, rollbacks', 'IaC', 'aws', '#8C4A00', '#351c00', 114900, 69900, 18, 274, 'Describe AWS resources in YAML
Preview updates with change sets
Automatic rollback on failure'),
    (59, 'Azure Bicep and ARM', 'Declarative Azure deployments', 'IaC', 'azure', '#0F5FA8', '#062440', 99900, 59900, 25, 287, 'Readable syntax that compiles to ARM
Preview with what-if
Modules for reuse'),
    (60, 'Docker Compose', 'Multi-container apps, one file', 'Containers', 'docker', '#1D63A8', '#0b2640', 74900, 44900, 9, 300, 'Run a full stack locally
Networks and volumes
Env files per environment'),
    (61, 'Kustomize', 'Overlay Kubernetes YAML', 'Containers', 'k8s', '#2A5BD7', '#102352', 64900, 39900, 16, 313, 'Overlays per environment
Patches without templates
Built into kubectl'),
    (62, 'Rancher', 'Manage many Kubernetes clusters', 'Containers', 'rancher', '#0075A8', '#002c40', 89900, 54900, 23, 326, 'One UI for many clusters
Access control across clusters
Provision and upgrade clusters'),
    (63, 'Spinnaker', 'Multi-cloud continuous delivery', 'CI/CD', 'spinnaker', '#139BB4', '#073b44', 119900, 69900, 7, 339, 'Canary and blue/green strategies
Pipelines across clouds
Manual judgment stages'),
    (64, 'Octopus Deploy', 'Release management and runbooks', 'CI/CD', 'octopus', '#2F93E0', '#123855', 99900, 59900, 14, 352, 'Releases promoted across environments
Variables scoped per environment
Runbooks for operations'),
    (65, 'Jira for Engineering Teams', 'Plan work, link code', 'Testing', 'jira', '#0052CC', '#001f4e', 64900, 39900, 21, 365, 'Boards, sprints and workflows
Link commits and pull requests
Automate status changes'),
    (66, 'Selenium Test Automation', 'Browser tests that run in CI', 'Testing', 'selenium', '#43B02A', '#194310', 74900, 44900, 5, 378, 'Write end-to-end browser tests
Run in parallel with Grid
Plug tests into CI'),
    (67, 'Postman API Testing', 'Collections, tests, Newman', 'Testing', 'postman', '#FF6C37', '#612915', 64900, 39900, 12, 391, 'Collections and environments
Automated API tests
Run collections in CI with Newman'),
    (68, 'JMeter Load Testing', 'Find limits before users do', 'Testing', 'jmeter', '#D22128', '#500d0f', 74900, 44900, 19, 404, 'Build load test plans
Read throughput and latency
Run headless in CI'),
    (69, 'OpenTelemetry', 'One standard for traces, metrics, logs', 'Monitoring', 'otel', '#425CC7', '#19234c', 99900, 59900, 3, 417, 'Vendor-neutral instrumentation
Collector pipelines
Correlate traces with logs'),
    (70, 'Sentry Error Tracking', 'Catch bugs with context', 'Monitoring', 'sentry', '#362D59', '#151122', 74900, 44900, 10, 430, 'Capture exceptions with context
Releases and source maps
Alert rules'),
    (71, 'PagerDuty On-Call', 'Incident response that works', 'Monitoring', 'pagerduty', '#06AC38', '#024115', 74900, 44900, 17, 443, 'Escalation policies
On-call schedules
Blameless postmortems'),
    (72, 'Traefik Proxy', 'Cloud-native reverse proxy', 'Containers', 'traefik', '#24A1C1', '#0e3d49', 74900, 44900, 24, 456, 'Automatic service discovery
Free TLS with Let''s Encrypt
Middlewares for auth and rate limits'),
    (73, 'Consul Service Networking', 'Discovery, health, mesh', 'Containers', 'consul', '#E03875', '#55152c', 99900, 59900, 8, 469, 'Service registry and health checks
Key/value store
Service mesh with mTLS'),
    (74, 'Nomad Orchestration', 'Simple workload scheduling', 'Containers', 'nomad', '#00CA8E', '#004d36', 89900, 54900, 15, 242, 'Run containers and plain binaries
One binary scheduler
Pairs with Consul and Vault'),
    (75, 'Linkerd Service Mesh', 'Lightweight and secure', 'Containers', 'linkerd', '#1AA37A', '#0a3e2e', 89900, 54900, 22, 255, 'Automatic mTLS
Golden metrics for every service
Small, fast proxy'),
    (76, 'Falco Runtime Security', 'Detect threats in containers', 'Security', 'falco', '#00AEC7', '#00424c', 99900, 59900, 6, 268, 'Detect suspicious behaviour at runtime
Write custom rules
Alerts to Slack or a SIEM'),
    (77, 'OWASP Security Testing', 'Top 10 and ZAP scanning', 'Security', 'owasp', '#2B2F3A', '#101216', 89900, 54900, 13, 281, 'Understand the OWASP Top 10
Scan with ZAP in CI
Fix common web flaws'),
    (78, 'Spring Boot for DevOps', 'Actuator, probes, containers', 'Build', 'spring', '#4C8F2A', '#1d3610', 89900, 54900, 20, 294, 'Actuator health and metrics
Liveness and readiness probes
Small container images'),
    (79, 'PowerShell for DevOps', 'Automation on Windows and beyond', 'Scripting', 'pwsh', '#2671BE', '#0e2b48', 74900, 44900, 4, 307, 'Object pipeline basics
Automate Azure and Windows
Cross-platform PowerShell 7');

-- New books added by the admin continue after the seeded ids
SELECT setval('books_id_seq', (SELECT max(id) FROM books));
