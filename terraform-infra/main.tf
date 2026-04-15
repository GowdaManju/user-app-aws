# ─── Root Module: Wires all modules together ───

# ── 1. Network (≈ CDK NetworkStack) ──
module "network" {
  source   = "./modules/network"
  vpc_cidr = var.vpc_cidr
  max_azs  = var.max_azs
  project  = var.project_name
}

# ── 2. ECS Cluster (≈ CDK ClusterStack) ──
module "ecs_cluster" {
  source  = "./modules/ecs_cluster"
  vpc_id  = module.network.vpc_id
  project = var.project_name
}

# ── 3. ECR Repositories (≈ CDK EcrStack) ──
module "ecr" {
  source    = "./modules/ecr"
  for_each  = var.services
  namespace = var.project_name
  service   = each.key
}

# ── 4. CodeBuild Projects (≈ CDK CodeBuildStack) ──
module "codebuild" {
  source         = "./modules/codebuild"
  for_each       = var.services
  project_name   = var.project_name
  service_name   = each.key
  ecr_repo_arn   = module.ecr[each.key].repository_arn
  ecr_repo_uri   = module.ecr[each.key].repository_uri
  github_owner   = each.value.github_owner
  github_repo    = each.value.github_repo
  github_branch  = each.value.github_branch
  buildspec_path = each.value.buildspec_path
  aws_account_id = var.aws_account_id
  aws_region     = var.aws_region
}

# ── 5. ECS Fargate Services (≈ CDK FargateServiceConstruct per service) ──
module "ecs_service" {
  source            = "./modules/ecs_service"
  for_each          = var.services
  service_name      = each.key
  cluster_id        = module.ecs_cluster.cluster_id
  cluster_name      = module.ecs_cluster.cluster_name
  vpc_id            = module.network.vpc_id
  public_subnet_ids = module.network.public_subnet_ids
  image             = each.value.image
  cpu               = each.value.cpu
  memory            = each.value.memory
  container_port    = each.value.container_port
  desired_count     = each.value.desired_count
  health_check_path = each.value.health_check_path
  ecr_repo_arn      = module.ecr[each.key].repository_arn
  aws_region        = var.aws_region
}

