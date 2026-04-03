package com.myorg.construct;

import com.myorg.config.ServiceConfig;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ecs.LogDrivers;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;

public class FargateServiceConstruct extends Construct {

    private final ApplicationLoadBalancedFargateService service;

    public FargateServiceConstruct(Construct scope, String id,
                                   Cluster cluster,
                                   ServiceConfig config) {
        super(scope, id);

        LogGroup logGroup = LogGroup.Builder.create(this, config.serviceName + "LogGroup")
                .logGroupName("/ecs/" + config.serviceName)
                .retention(RetentionDays.ONE_WEEK)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // Resolve container image — use ECR integration for automatic IAM permissions
        ContainerImage containerImage;
        if (config.image.contains(".dkr.ecr.")) {
            // Parse ECR URI: <account>.dkr.ecr.<region>.amazonaws.com/<repo-name>:<tag>
            String withoutRegistry = config.image.substring(config.image.indexOf("/") + 1); // "practice-repo/user-service:latest"
            String tag = withoutRegistry.contains(":") ? withoutRegistry.substring(withoutRegistry.lastIndexOf(":") + 1) : "latest";
            String repoName = withoutRegistry.contains(":") ? withoutRegistry.substring(0, withoutRegistry.lastIndexOf(":")) : withoutRegistry;
            IRepository ecrRepo = Repository.fromRepositoryName(this, config.serviceName + "EcrRepo", repoName);
            containerImage = ContainerImage.fromEcrRepository(ecrRepo, tag);
        } else {
            containerImage = ContainerImage.fromRegistry(config.image);
        }

        this.service = ApplicationLoadBalancedFargateService.Builder.create(this, config.serviceName)
                .cluster(cluster)
                .cpu(config.cpu)
                .memoryLimitMiB(config.memory)
                .desiredCount(1)
                .minHealthyPercent(100)
                .maxHealthyPercent(200)
                .publicLoadBalancer(true)
                .assignPublicIp(true)
                .taskSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PUBLIC)
                        .build())
                .taskImageOptions(
                        ApplicationLoadBalancedTaskImageOptions.builder()
                                .image(containerImage)
                                .containerPort(config.containerPort)
                                .logDriver(LogDrivers.awsLogs(
                                        AwsLogDriverProps.builder()
                                                .logGroup(logGroup)
                                                .streamPrefix(config.serviceName)
                                                .build()
                                ))
                                .build()
                )
                .build();

        // Health check
        service.getTargetGroup().configureHealthCheck(
                software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck.builder()
                        .path("/")
                        .healthyHttpCodes("200")
                        .interval(software.amazon.awscdk.Duration.seconds(30))
                        .timeout(software.amazon.awscdk.Duration.seconds(5))
                        .healthyThresholdCount(2)
                        .unhealthyThresholdCount(3)
                        .build()
        );

        // Output ALB URL
        CfnOutput.Builder.create(this, config.serviceName + "URL")
                .value("http://" + service.getLoadBalancer().getLoadBalancerDnsName())
                .description("ALB endpoint for " + config.serviceName)
                .build();
    }

    public ApplicationLoadBalancedFargateService getService() {
        return this.service;
    }
}