package com.myorg.service.order_service;

import com.myorg.config.ServiceConfig;
import software.amazon.awscdk.Stack;
import software.constructs.Construct;
import software.amazon.awscdk.services.ecs.Cluster;
import com.myorg.construct.FargateServiceConstruct;

public class OrderServiceStack extends Stack {

    public OrderServiceStack(final Construct scope,
                             final String id,
                             Cluster cluster) {
        super(scope, id);

        ServiceConfig orderConfig = new ServiceConfig(
                "OrderService",
                "621541294877.dkr.ecr.us-east-1.amazonaws.com/practice-repo/user-service:user-service_latest",
                256,
                512,
                8080
        );

        new FargateServiceConstruct(this, "OrderService", cluster, orderConfig);
    }
}