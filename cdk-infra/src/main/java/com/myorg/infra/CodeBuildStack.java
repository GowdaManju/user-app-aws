package com.myorg.infra;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.codebuild.*;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.ecr.Repository;
import software.constructs.Construct;

import java.util.Map;
import java.util.List;

public class CodeBuildStack extends Stack {

    public CodeBuildStack(final Construct scope,
                          final String id,
                          final String namespace,
                          Map<String, Repository> repos,
                          String artifactType) {

        super(scope, id, (StackProps) null);

        for (Map.Entry<String, Repository> entry : repos.entrySet()) {

            String serviceName = entry.getKey();
            Repository repo = entry.getValue();

            // IAM Role
            Role codeBuildRole = Role.Builder.create(this, serviceName + "CodeBuildRole")
                    .assumedBy(new ServicePrincipal("codebuild.amazonaws.com"))
                    .managedPolicies(List.of(
                            ManagedPolicy.fromAwsManagedPolicyName("AmazonEC2ContainerRegistryPowerUser")
                    ))
                    .build();

            // 🔹 COMMON build environment
            BuildEnvironment env = BuildEnvironment.builder()
                    .buildImage(LinuxBuildImage.STANDARD_7_0)
                    .privileged(true)
                    .build();

            // =========================
            // ✅ 1. CODEPIPELINE FLOW
            // =========================
            if ("codepipeline".equalsIgnoreCase(artifactType)) {

                // ❗ IMPORTANT: No GitHub source here
                PipelineProject pipelineProject = PipelineProject.Builder.create(this, serviceName + "PipelineBuild")
                        .projectName(namespace + "-" + serviceName + "-build")
                        .environment(env)
                        .role(codeBuildRole)
                        .buildSpec(BuildSpec.fromSourceFilename(
                                "service/" + serviceName + "/buildspec.yml"
                        ))
                        .build();

                // 👉 Source will come from CodePipeline (not here)

            } else {

                // =========================
                // ✅ 2. MANUAL / DIRECT BUILD
                // =========================

                ISource gitHubSource = Source.gitHub(GitHubSourceProps.builder()
                        .owner("gowdamanju")
                        .repo("user-app-aws")
                        .branchOrRef("main")
                        .build());

                Project project = Project.Builder.create(this, serviceName + "CodeBuild")
                        .projectName(namespace + "-" + serviceName + "-build")
                        .source(gitHubSource)
                        .environment(env)
                        .role(codeBuildRole)
                        .buildSpec(BuildSpec.fromSourceFilename(
                                "User-Service/cdk-infra/src/main/java/com/myorg/"+"service/" + serviceName + "/buildspec.yml"
                        ))
                        .build();
            }

            // ECR permissions
            repo.grantPullPush(codeBuildRole);
        }
    }
}