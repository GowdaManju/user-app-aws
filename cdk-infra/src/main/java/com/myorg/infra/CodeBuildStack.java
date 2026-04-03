package com.myorg.infra;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.codebuild.*;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.ecr.Repository;
import software.constructs.Construct;

import java.util.Map;

public class CodeBuildStack extends Stack {
    public CodeBuildStack(final Construct scope, final String id, final String namespace, Map<String, Repository> repos, String artifactType) {
        super(scope, id, null);

        for (Map.Entry<String, Repository> entry : repos.entrySet()) {
            String serviceName = entry.getKey();
            Repository repo = entry.getValue();

            Role codeBuildRole = Role.Builder.create(this, serviceName + "CodeBuildRole")
                    .assumedBy(new ServicePrincipal("codebuild.amazonaws.com"))
                    .managedPolicies(java.util.List.of(
                            ManagedPolicy.fromAwsManagedPolicyName("AmazonEC2ContainerRegistryPowerUser")
                    ))
                    .build();

            if ("codepipeline".equalsIgnoreCase(artifactType)) {
                PipelineProject.Builder.create(this, serviceName + "CodeBuild")
                        .projectName(namespace + "-" + serviceName + "-build")
                        .environment(BuildEnvironment.builder()
                                .buildImage(LinuxBuildImage.STANDARD_7_0)
                                .privileged(true)
                                .build())
                        .role(codeBuildRole)
                        .buildSpec(BuildSpec.fromSourceFilename("service/" + serviceName + "/buildspec.yml"))
                        .build();
            } else {
                Project.Builder.create(this, serviceName + "CodeBuild")
                        .projectName(namespace + "-" + serviceName + "-build")
                        .environment(BuildEnvironment.builder()
                                .buildImage(LinuxBuildImage.STANDARD_7_0)
                                .privileged(true)
                                .build())
                        .role(codeBuildRole)
                        .buildSpec(BuildSpec.fromSourceFilename("service/" + serviceName + "/buildspec.yml"))
                        .build();
            }

            repo.grantPullPush(codeBuildRole);
        }
    }
}
