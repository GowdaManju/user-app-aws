package com.myorg.app;

import com.myorg.service.user_service.UserServiceStack;
import software.amazon.awscdk.App;
import com.myorg.infra.NetworkStack;
import com.myorg.infra.ClusterStack;
import com.myorg.infra.EcrStack;
import com.myorg.infra.CodeBuildStack; // You need to implement this
import software.amazon.awscdk.services.ecr.Repository;
import java.util.*;

public class CdkInfraApp {
    public static void main(final String[] args) {
        App app = new App();

        // --- List of microservices ---
        List<String> serviceNames = Arrays.asList(
            "user-service"
            // Add more services here, e.g., "payment-service"
        );

        // --- ECR Repositories ---
        EcrStack ecrStack = new EcrStack(app, "EcrStack", "practice-repo");
        for (String service : serviceNames) {
            ecrStack.addRepository(service);
        }

        // --- Networking ---
        NetworkStack network = new NetworkStack(app, "NetworkStack");
        // --- ECS Cluster ---
        ClusterStack clusterStack = new ClusterStack(app, "ClusterStack", network.vpc);



        // --- Service Stacks ---
        // Example: create a stack per service (expand as needed)
        new UserServiceStack(app, "UserServiceStack", clusterStack.cluster);
        // new PaymentServiceStack(app, "PaymentServiceStack", clusterStack.cluster);




        // --- CodeBuild Stack for all services ---
        // You need to implement getRepositories() in EcrStack to return Map<String, Repository>
        Map<String, Repository> repos = ecrStack.getRepositories();
        new CodeBuildStack(app, "CodeBuildStack", "practice-repo", repos,"none");

        app.synth();
    }
}
