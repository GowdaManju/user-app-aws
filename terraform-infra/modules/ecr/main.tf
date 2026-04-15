# ─── ECR Module (≈ CDK EcrStack) ───
# Production-grade ECR repository with scanning, lifecycle rules, and mutable tags

variable "namespace" {
  description = "ECR namespace prefix (e.g. practice-repo)"
  type        = string
}

variable "service" {
  description = "Service name (e.g. user-service)"
  type        = string
}

variable "image_tag_mutability" {
  description = "Tag mutability: MUTABLE or IMMUTABLE"
  type        = string
  default     = "MUTABLE" # Mutable so 'latest' tag can be updated
}

variable "untagged_retention_days" {
  description = "Days to keep untagged images"
  type        = number
  default     = 7
}

variable "max_tagged_image_count" {
  description = "Max number of tagged images to retain"
  type        = number
  default     = 20
}

resource "aws_ecr_repository" "repo" {
  name                 = "${var.namespace}/${var.service}"
  image_tag_mutability = var.image_tag_mutability

  image_scanning_configuration {
    scan_on_push = true
  }

  # force_delete = false → prevents accidental deletion (≈ CDK RemovalPolicy.RETAIN)
  force_delete = false

  tags = {
    Service = var.service
  }
}

resource "aws_ecr_lifecycle_policy" "policy" {
  repository = aws_ecr_repository.repo.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Remove untagged images after ${var.untagged_retention_days} days"
        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = var.untagged_retention_days
        }
        action = {
          type = "expire"
        }
      },
      {
        rulePriority = 2
        description  = "Keep last ${var.max_tagged_image_count} tagged images"
        selection = {
          tagStatus     = "any"
          countType     = "imageCountMoreThan"
          countNumber   = var.max_tagged_image_count
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

# ─── Outputs ───
output "repository_url" {
  value = aws_ecr_repository.repo.repository_url
}

output "repository_uri" {
  value = aws_ecr_repository.repo.repository_url
}

output "repository_arn" {
  value = aws_ecr_repository.repo.arn
}

output "repository_name" {
  value = aws_ecr_repository.repo.name
}

