# ─── General ───
variable "aws_region" {
  description = "AWS region to deploy resources"
  type        = string
  default     = "us-east-1"
}

variable "aws_account_id" {
  description = "AWS Account ID"
  type        = string
}

variable "project_name" {
  description = "Project name used for tagging and naming"
  type        = string
  default     = "practice-repo"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "prod"
}

# ─── Network ───
variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "max_azs" {
  description = "Maximum number of availability zones"
  type        = number
  default     = 2
}

# ─── Services ───
variable "services" {
  description = "Map of microservices to deploy"
  type = map(object({
    image          = string
    cpu            = number
    memory         = number
    container_port = number
    desired_count  = number
    health_check_path = string
    github_owner   = string
    github_repo    = string
    github_branch  = string
    buildspec_path = string
  }))
}

