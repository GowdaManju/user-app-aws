aws_account_id = "621541294877"
aws_region     = "us-east-1"
project_name   = "practice-repo"
environment    = "prod"

vpc_cidr = "10.0.0.0/16"
max_azs  = 2

# ─── Microservices ───
# Add more entries here to onboard new services.
# Each key becomes: ECR repo, CodeBuild project, ECS Fargate service.
services = {
  "user-service" = {
    image             = "621541294877.dkr.ecr.us-east-1.amazonaws.com/practice-repo/user-service:user-service_latest"
    cpu               = 256
    memory            = 512
    container_port    = 8080
    desired_count     = 1
    health_check_path = "/actuator/health"
    github_owner      = "gowdamanju"
    github_repo       = "user-app-aws"
    github_branch     = "main"
    buildspec_path    = "cdk-infra/src/main/java/com/myorg/service/user_service/buildspec.yml"
  }

  # Example: Add another service
  # "order-service" = {
  #   image             = "621541294877.dkr.ecr.us-east-1.amazonaws.com/practice-repo/order-service:order-service_latest"
  #   cpu               = 256
  #   memory            = 512
  #   container_port    = 8081
  #   desired_count     = 1
  #   health_check_path = "/actuator/health"
  #   github_owner      = "gowdamanju"
  #   github_repo       = "user-app-aws"
  #   github_branch     = "main"
  #   buildspec_path    = "cdk-infra/src/main/java/com/myorg/service/order_service/buildspec.yml"
  # }
}

