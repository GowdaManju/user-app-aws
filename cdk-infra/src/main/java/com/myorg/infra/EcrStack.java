package com.myorg.infra;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecr.TagMutability;
import software.amazon.awscdk.services.ecr.LifecycleRule;
import software.amazon.awscdk.services.ecr.TagStatus;
import software.constructs.Construct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EcrStack extends Stack {

    private final String namespace;
    private final Map<String, Repository> repositories = new HashMap<>();

    public EcrStack(final Construct scope, final String id, final String namespace) {
        this(scope, id, namespace, null);
    }

    public EcrStack(final Construct scope, final String id, final String namespace, final StackProps props) {
        super(scope, id, props);
        this.namespace = namespace;
    }

    /**
     * Creates an ECR repository under the namespace with production-grade defaults:
     * - Image scanning on push
     * - Immutable tags (prevents overwriting production images)
     * - Lifecycle rules to limit untagged and old image retention
     * - RETAIN removal policy (prevents accidental deletion via CDK destroy)
     */
    public Repository addRepository(final String serviceName) {
        return addRepository(serviceName, EcrRepositoryProps.defaults());
    }

    public Repository addRepository(final String serviceName, final EcrRepositoryProps repoProps) {
        String fullRepoName = namespace + "/" + serviceName;

        List<LifecycleRule> lifecycleRules = new ArrayList<>();

        // Remove untagged images after N days
        lifecycleRules.add(LifecycleRule.builder()
                .rulePriority(1)
                .description("Remove untagged images after " + repoProps.untaggedRetentionDays() + " days")
                .tagStatus(TagStatus.UNTAGGED)
                .maxImageAge(software.amazon.awscdk.Duration.days(repoProps.untaggedRetentionDays()))
                .build());

        // Keep only last N tagged images
        lifecycleRules.add(LifecycleRule.builder()
                .rulePriority(2)
                .description("Keep last " + repoProps.maxTaggedImageCount() + " tagged images")
                .tagStatus(TagStatus.ANY)
                .maxImageCount(repoProps.maxTaggedImageCount())
                .build());

        Repository repo = Repository.Builder.create(this, serviceName + "Repo")
                .repositoryName(fullRepoName)
                .imageScanOnPush(true)
                .imageTagMutability(repoProps.immutableTags() ? TagMutability.IMMUTABLE : TagMutability.MUTABLE)
                .lifecycleRules(lifecycleRules)
                .removalPolicy(RemovalPolicy.RETAIN)
                .build();

        // Export repository URI for use in CodeBuild / other stacks
        CfnOutput.Builder.create(this, serviceName + "RepoUri")
                .exportName(namespace + "-" + serviceName + "-repo-uri")
                .value(repo.getRepositoryUri())
                .description("ECR repository URI for " + serviceName)
                .build();

        CfnOutput.Builder.create(this, serviceName + "RepoArn")
                .exportName(namespace + "-" + serviceName + "-repo-arn")
                .value(repo.getRepositoryArn())
                .description("ECR repository ARN for " + serviceName)
                .build();

        repositories.put(serviceName, repo);
        return repo;
    }

    public Repository getRepository(final String serviceName) {
        return repositories.get(serviceName);
    }

    public Map<String, Repository> getRepositories() {
        return Map.copyOf(repositories);
    }

    public String getNamespace() {
        return namespace;
    }
}