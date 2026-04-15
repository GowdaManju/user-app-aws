# ─── CodeBuild Module (≈ CDK CodeBuildStack) ───
# Supports both manual trigger and CodePipeline trigger

variable "project_name" {
  type = string
}

variable "service_name" {
  type = string
}

variable "ecr_repo_arn" {
  type = string
}

variable "ecr_repo_uri" {
  type = string
}

variable "github_owner" {
  type = string
}

variable "github_repo" {
  type = string
}

variable "github_branch" {
  type    = string
  default = "main"
}

variable "buildspec_path" {
  description = "Path to buildspec.yml inside the source repo"
  type        = string
}

variable "aws_account_id" {
  type = string
}

variable "aws_region" {
  type = string
}

# ── IAM Role ──
resource "aws_iam_role" "codebuild" {
  name = "${var.project_name}-${var.service_name}-codebuild-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "codebuild.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecr_power_user" {
  role       = aws_iam_role.codebuild.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryPowerUser"
}

resource "aws_iam_role_policy" "codebuild_logs" {
  name = "${var.service_name}-codebuild-logs"
  role = aws_iam_role.codebuild.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "ecr:GetAuthorizationToken"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:PutImage",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload",
          "ecr:DescribeImages",
          "ecr:ListImages"
        ]
        Resource = var.ecr_repo_arn
      }
    ]
  })
}

# ── CodeBuild Project (Manual / Direct build with GitHub source) ──
resource "aws_codebuild_project" "build" {
  name          = "${var.project_name}-${var.service_name}-build"
  description   = "Build and push ${var.service_name} Docker image to ECR"
  service_role  = aws_iam_role.codebuild.arn
  build_timeout = 30 # minutes

  # ── Source: GitHub (supports manual trigger via console/CLI) ──
  source {
    type            = "GITHUB"
    location        = "https://github.com/${var.github_owner}/${var.github_repo}.git"
    git_clone_depth = 1
    buildspec       = var.buildspec_path

    git_submodules_config {
      fetch_submodules = false
    }
  }

  source_version = var.github_branch

  # ── Environment ──
  environment {
    compute_type                = "BUILD_GENERAL1_SMALL"
    image                       = "aws/codebuild/standard:7.0"
    type                        = "LINUX_CONTAINER"
    privileged_mode             = true # Required for Docker builds
    image_pull_credentials_type = "CODEBUILD"

    environment_variable {
      name  = "AWS_ACCOUNT_ID"
      value = var.aws_account_id
    }

    environment_variable {
      name  = "AWS_DEFAULT_REGION"
      value = var.aws_region
    }

    environment_variable {
      name  = "ECR_REPO_URI"
      value = var.ecr_repo_uri
    }

    environment_variable {
      name  = "SERVICE_NAME"
      value = var.service_name
    }
  }

  # ── Artifacts: NONE for manual builds ──
  artifacts {
    type = "NO_ARTIFACTS"
  }

  # ── Logs ──
  logs_config {
    cloudwatch_logs {
      group_name  = "/codebuild/${var.project_name}/${var.service_name}"
      stream_name = "build"
    }
  }

  tags = {
    Service = var.service_name
  }
}

# ─── Outputs ───
output "project_name" {
  value = aws_codebuild_project.build.name
}

output "project_arn" {
  value = aws_codebuild_project.build.arn
}

