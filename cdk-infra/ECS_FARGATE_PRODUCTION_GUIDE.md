# ECS Fargate Production Configuration Guide

A comprehensive checklist of everything you need to configure for an **AWS ECS Fargate** service to meet **production quality**.

---

## Table of Contents

1. [Networking](#1-networking)
2. [ECS Cluster](#2-ecs-cluster)
3. [Task Definition](#3-task-definition)
4. [Service Configuration](#4-service-configuration)
5. [Load Balancer](#5-load-balancer)
6. [Auto Scaling](#6-auto-scaling)
7. [Logging & Monitoring](#7-logging--monitoring)
8. [Security](#8-security)
9. [CI/CD & Deployment Strategy](#9-cicd--deployment-strategy)
10. [Cost Optimization](#10-cost-optimization)

---

## 1. Networking

| Configuration | Description | Recommended Value |
|---|---|---|
| **VPC** | Isolated network for your services | Multi-AZ (at least 2 AZs) |
| **Subnets** | Public subnets for ALB, private subnets for tasks | Use private subnets + NAT Gateway for production |
| **NAT Gateway** | Allows private subnet tasks to access internet (ECR, CloudWatch, etc.) | At least 1 per AZ for HA |
| **VPC Endpoints** | Private connectivity to AWS services without NAT | ECR, CloudWatch Logs, S3, Secrets Manager |
| **Security Groups** | Firewall rules for tasks and ALB | Least-privilege (ALB → tasks only on container port) |

### Cost-Saving Alternative (Non-Production)
- Use **public subnets** + `assignPublicIp: true` + `natGateways: 0` to avoid NAT Gateway costs (~$32/month per gateway)

---

## 2. ECS Cluster

| Configuration | Description | Recommended Value |
|---|---|---|
| **Container Insights** | Detailed CPU, memory, network, storage metrics | `containerInsights: ENABLED` |
| **Capacity Providers** | Fargate and Fargate Spot mix | `FARGATE` for critical, `FARGATE_SPOT` for cost savings |
| **Execute Command** | SSH-like access to running containers for debugging | Enable for non-prod, disable for prod |

---

## 3. Task Definition

### Container Configuration

| Configuration | Description | Recommended Value |
|---|---|---|
| **CPU** | vCPU units for the task | 256 (0.25 vCPU) to 4096 (4 vCPU) |
| **Memory** | Memory in MiB | 512 MiB to 30720 MiB |
| **Container Port** | Port your application listens on | Match your app (e.g., 8080, 80) |
| **Image Tag** | Docker image version | Never use `latest` — use specific tags or SHA |
| **Read-Only Root FS** | Immutable container filesystem | `true` (mount writable `/tmp` if needed) |
| **Essential** | Mark container as essential | `true` |

### Health Check (Container Level)

| Configuration | Description | Recommended Value |
|---|---|---|
| **Command** | Health check command | `CMD-SHELL, curl -f http://localhost:8080/actuator/health || exit 1` |
| **Interval** | Time between checks | `30s` |
| **Timeout** | Time to wait for response | `5s` |
| **Retries** | Failures before unhealthy | `3` |
| **Start Period** | Grace period for startup | `60s` (increase for slow-starting apps) |

### Environment & Secrets

| Configuration | Description | Example |
|---|---|---|
| **Environment Variables** | Non-sensitive app config | `SPRING_PROFILES_ACTIVE=prod`, `SERVER_PORT=8080` |
| **Secrets (SSM Parameter Store)** | Sensitive values from SSM | `DB_PASSWORD` → `arn:aws:ssm:region:account:parameter/db-password` |
| **Secrets (Secrets Manager)** | Sensitive values from Secrets Manager | `API_KEY` → `arn:aws:secretsmanager:region:account:secret:api-key` |

### IAM Roles

| Role | Purpose | Permissions Needed |
|---|---|---|
| **Task Execution Role** | Used by ECS Agent to pull images, push logs | `ecr:GetAuthorizationToken`, `logs:CreateLogStream`, `logs:PutLogEvents`, `ssm:GetParameters`, `secretsmanager:GetSecretValue` |
| **Task Role** | Used by your application code | Only what your app needs (e.g., `s3:GetObject`, `sqs:SendMessage`, `dynamodb:PutItem`) — **least privilege** |

---

## 4. Service Configuration

| Configuration | Description | Recommended Value |
|---|---|---|
| **Desired Count** | Number of running tasks | `≥ 2` (for high availability) |
| **Min Healthy Percent** | Min tasks during deployment | `100%` (zero downtime) |
| **Max Healthy Percent** | Max tasks during deployment | `200%` (allows rolling update) |
| **Platform Version** | Fargate platform version | `LATEST` (currently 1.4.0) |
| **Enable Execute Command** | Debug running containers | `false` in production |
| **Propagate Tags** | Propagate tags to tasks | `SERVICE` |

### Circuit Breaker (Auto-Rollback Failed Deployments)

```
circuitBreaker:
  enable: true
  rollback: true
```

If a deployment fails (tasks keep crashing), ECS automatically rolls back to the last stable version.

### Deployment Configuration

| Configuration | Description | Recommended Value |
|---|---|---|
| **Deployment Type** | How tasks are replaced | `ROLLING` (default) or `BLUE_GREEN` (with CodeDeploy) |
| **Force New Deployment** | Force redeployment even without changes | `false` (use only when needed) |

---

## 5. Load Balancer

### Application Load Balancer (ALB)

| Configuration | Description | Recommended Value |
|---|---|---|
| **Scheme** | Internet-facing or internal | `internet-facing` for public APIs, `internal` for microservices |
| **HTTPS Listener** | TLS termination at ALB | Port 443 with ACM certificate |
| **HTTP → HTTPS Redirect** | Redirect HTTP to HTTPS | Always enable |
| **Idle Timeout** | Connection idle timeout | `60s` (increase for WebSocket/long-polling) |
| **WAF** | Web Application Firewall | Enable for DDoS, bot, SQL injection protection |

### Target Group Health Check

| Configuration | Description | Recommended Value |
|---|---|---|
| **Path** | Health check endpoint | `/actuator/health` (Spring Boot) or `/health` |
| **Interval** | Time between health checks | `30s` |
| **Timeout** | Time to wait for response | `10s` |
| **Healthy Threshold** | Consecutive successes to be healthy | `2` |
| **Unhealthy Threshold** | Consecutive failures to be unhealthy | `3` |
| **Success Codes** | HTTP codes considered healthy | `200` |
| **Deregistration Delay** | Time to drain connections before removing task | `30s` (match your app's graceful shutdown time) |

### Stickiness (Optional)

| Configuration | Description | When to Use |
|---|---|---|
| **Sticky Sessions** | Route same client to same task | Only if your app is NOT stateless |
| **Duration** | Cookie expiry time | `3600s` |

---

## 6. Auto Scaling

### Scaling Policy

| Configuration | Description | Recommended Value |
|---|---|---|
| **Min Capacity** | Minimum tasks running | `2` (HA across AZs) |
| **Max Capacity** | Maximum tasks allowed | Based on expected peak load (e.g., `10`) |
| **Scale-Out Cooldown** | Wait time after scaling out | `60s` (react fast) |
| **Scale-In Cooldown** | Wait time after scaling in | `300s` (avoid flapping) |

### Scaling Targets

| Metric | Target | Description |
|---|---|---|
| **CPU Utilization** | `70%` | Scale when average CPU > 70% |
| **Memory Utilization** | `70%` | Scale when average memory > 70% |
| **ALB Request Count** | `1000 per target` | Scale based on incoming requests |
| **Custom Metric** | App-specific | e.g., queue depth, active connections |

### Scheduled Scaling (Optional)

For predictable traffic patterns:
- Scale up before business hours (e.g., 8 AM → 5 tasks)
- Scale down after hours (e.g., 8 PM → 2 tasks)

---

## 7. Logging & Monitoring

### CloudWatch Logs

| Configuration | Description | Recommended Value |
|---|---|---|
| **Log Driver** | Container log driver | `awslogs` |
| **Log Group** | CloudWatch log group | `/ecs/<service-name>` |
| **Log Retention** | How long to keep logs | `14 days` (dev), `90 days` (prod) |
| **Stream Prefix** | Log stream prefix | Service name |

### CloudWatch Alarms

| Alarm | Threshold | Description |
|---|---|---|
| **High CPU** | `> 85%` for 3 periods | Tasks are CPU-starved |
| **High Memory** | `> 85%` for 3 periods | Tasks are memory-starved, risk of OOM kill |
| **ALB 5xx Errors** | `> 10` in 5 min | Server errors — app is failing |
| **ALB 4xx Errors** | `> 100` in 5 min | Client errors — possible misuse or bugs |
| **Unhealthy Host Count** | `> 0` for 2 periods | Tasks failing health checks |
| **Running Task Count** | `< desired count` for 2 periods | Tasks are crashing |

### Alarm Actions

| Action | Service | Use Case |
|---|---|---|
| **SNS Notification** | SNS → Email/Slack/PagerDuty | Alert the on-call team |
| **Auto Scaling** | Application Auto Scaling | Automatically add more tasks |

### Observability (Advanced)

| Tool | Purpose |
|---|---|
| **AWS X-Ray** | Distributed tracing across microservices |
| **CloudWatch Container Insights** | Detailed ECS metrics dashboard |
| **CloudWatch Contributor Insights** | Top-N contributors to metrics (e.g., top IPs, top error paths) |
| **AWS Distro for OpenTelemetry** | Vendor-neutral tracing & metrics |

---

## 8. Security

### Network Security

| Configuration | Description | Recommended Value |
|---|---|---|
| **ALB Security Group** | Inbound rules for ALB | Allow `80/443` from `0.0.0.0/0` (or specific CIDRs) |
| **Task Security Group** | Inbound rules for tasks | Allow **only** from ALB security group on container port |
| **Outbound Rules** | Egress rules | Allow `443` to AWS services, restrict everything else |

### Container Security

| Configuration | Description | Recommended Value |
|---|---|---|
| **Non-Root User** | Run container as non-root | `USER 1000` in Dockerfile |
| **Read-Only Root FS** | Prevent writes to container FS | `readonlyRootFilesystem: true` |
| **No Privileged Mode** | Don't run in privileged mode | `privileged: false` (Fargate default) |
| **Image Scanning** | Scan images for vulnerabilities | Enable ECR image scanning |
| **Image Signing** | Verify image integrity | Use AWS Signer |

### Data Security

| Configuration | Description | Recommended Value |
|---|---|---|
| **Encryption in Transit** | HTTPS between client ↔ ALB | ACM certificate on ALB |
| **Encryption at Rest** | Encrypt EFS/EBS volumes | AWS-managed KMS key |
| **Secrets Management** | Store sensitive config | AWS Secrets Manager or SSM Parameter Store |

---

## 9. CI/CD & Deployment Strategy

### Deployment Options

| Strategy | Description | Best For |
|---|---|---|
| **Rolling Update** | Replace tasks one batch at a time | Most use cases (simple, zero-downtime) |
| **Blue/Green (CodeDeploy)** | Run new version alongside old, switch traffic | Critical services needing instant rollback |
| **Canary** | Route small % of traffic to new version first | Gradual rollout with traffic shifting |

### CI/CD Pipeline

```
Source (GitHub/CodeCommit)
  → Build (CodeBuild / GitHub Actions)
    → Push Image to ECR
      → Update Task Definition
        → Deploy to ECS (Rolling / Blue-Green)
          → Health Check Gate
            → Rollback on failure
```

### ECR Best Practices

| Practice | Description |
|---|---|
| **Image Tagging** | Use git SHA or semver (never `latest`) |
| **Lifecycle Policy** | Auto-delete untagged/old images (keep last 10) |
| **Image Scanning** | Scan on push for CVEs |
| **Cross-Region Replication** | Replicate images for DR |

---

## 10. Cost Optimization

| Strategy | Description | Savings |
|---|---|---|
| **Right-Size Tasks** | Monitor actual CPU/memory usage, reduce if over-provisioned | 20-50% |
| **Fargate Spot** | Use for non-critical/fault-tolerant workloads | Up to 70% |
| **Scheduled Scaling** | Scale down during off-peak hours | 30-50% |
| **Savings Plans** | Commit to 1 or 3 year usage | Up to 50% |
| **VPC Endpoints** | Reduce NAT Gateway data transfer costs | Variable |
| **Public Subnets + Public IP** | Avoid NAT Gateway entirely (non-prod) | ~$32/month per gateway |

---

## Quick Reference: Minimum Production Config

```
✅ VPC with 2+ AZs
✅ Private subnets for tasks + NAT Gateway (or public subnet + public IP)
✅ ALB with HTTPS (ACM certificate)
✅ Health checks (ALB + container level)
✅ Circuit breaker with rollback
✅ Auto scaling (CPU + Memory based)
✅ 2+ desired tasks (high availability)
✅ CloudWatch Logs with retention policy
✅ CloudWatch Alarms (CPU, Memory, 5xx)
✅ IAM least-privilege roles (Task Role + Execution Role)
✅ Security groups (ALB → Task only)
✅ Secrets via SSM/Secrets Manager (not env vars)
✅ Container Insights enabled
✅ Tags for cost allocation
✅ CI/CD pipeline with auto-rollback
```

---

## Architecture Diagram

```
                    Internet
                       │
                       ▼
               ┌───────────────┐
               │     WAF       │  (optional)
               └───────┬───────┘
                       ▼
               ┌───────────────┐
               │      ALB      │  (Public Subnet, HTTPS)
               │  Port 443/80  │
               └───┬───────┬───┘
                   │       │
          ┌────────▼──┐ ┌──▼────────┐
          │  Target   │ │  Target   │
          │  Group    │ │  Group    │
          └────┬──────┘ └──────┬────┘
               │               │
       ┌───────▼───┐   ┌──────▼────┐
       │  Fargate  │   │  Fargate  │    (Private/Public Subnet)
       │  Task 1   │   │  Task 2   │
       │  (AZ-a)   │   │  (AZ-b)   │
       └─────┬─────┘   └─────┬─────┘
             │                │
    ┌────────▼────────────────▼────────┐
    │        AWS Services              │
    │  CloudWatch │ ECR │ S3 │ RDS     │
    │  Secrets Mgr│ SQS │ DynamoDB     │
    └──────────────────────────────────┘
```

---

## CDK Implementation

All the above configurations are implemented in this project:

| File | What It Configures |
|---|---|
| `config/ServiceConfig.java` | Builder pattern with all production config fields |
| `construct/FargateServiceConstruct.java` | ALB, health check, auto scaling, circuit breaker, alarms, logging, tags |
| `infra/NetworkStack.java` | VPC, subnets, NAT/public IP |
| `infra/ClusterStack.java` | ECS Cluster with Container Insights |
| `service/OrderServiceStack.java` | Example service using the construct |
| `app/CdkInfraApp.java` | App entry point wiring all stacks |

---

> **Note:** For HTTPS/SSL, you need a registered domain name and an ACM certificate. For WAF, attach it to the ALB using `WebAcl`.

