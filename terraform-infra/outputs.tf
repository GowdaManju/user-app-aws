# ─── Root Outputs ───

output "vpc_id" {
  description = "VPC ID"
  value       = module.network.vpc_id
}

output "ecs_cluster_name" {
  description = "ECS Cluster name"
  value       = module.ecs_cluster.cluster_name
}

output "ecr_repositories" {
  description = "ECR repository URIs per service"
  value       = { for k, v in module.ecr : k => v.repository_uri }
}

output "service_urls" {
  description = "ALB DNS endpoints per service"
  value       = { for k, v in module.ecs_service : k => v.alb_dns }
}

output "codebuild_projects" {
  description = "CodeBuild project names per service"
  value       = { for k, v in module.codebuild : k => v.project_name }
}

