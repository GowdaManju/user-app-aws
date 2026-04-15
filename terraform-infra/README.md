# Terraform Infrastructure

This is the Terraform equivalent of the CDK infrastructure (`cdk-infra/`).

## Directory Structure

```
terraform-infra/
├── main.tf                  # Root module — wires everything together
├── variables.tf             # Input variables
├── outputs.tf               # Root outputs
├── providers.tf             # AWS provider & backend config
├── terraform.tfvars         # Default variable values (DO NOT commit secrets)
├── modules/
│   ├── network/             # VPC, subnets (≈ NetworkStack)
│   ├── ecr/                 # ECR repositories (≈ EcrStack)
│   ├── ecs_cluster/         # ECS cluster (≈ ClusterStack)
│   ├── ecs_service/         # Fargate service + ALB (≈ FargateServiceConstruct)
│   └── codebuild/           # CodeBuild project (≈ CodeBuildStack)
└── services/
    └── user-service.tf      # Per-service config (image, port, buildspec path)
```

## Prerequisites

- [Terraform CLI](https://developer.hashicorp.com/terraform/downloads) >= 1.5
- AWS CLI configured (`aws configure`)
- Docker (for local builds)

## Quick Start

```bash
cd terraform-infra

# Initialize
terraform init

# Preview
terraform plan

# Deploy
terraform apply

# Destroy (careful!)
terraform destroy
```

## Adding a New Microservice

1. Add an entry to `services` map in `terraform.tfvars`.
2. Run `terraform plan` → `terraform apply`.

That's it — ECR repo, CodeBuild project, and ECS Fargate service are all created automatically.

