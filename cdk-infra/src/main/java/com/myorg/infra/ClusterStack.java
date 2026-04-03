
package com.myorg.infra;

import software.amazon.awscdk.Stack;
import software.constructs.Construct;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ec2.Vpc;

public class ClusterStack extends Stack {
    public final Cluster cluster;

    public ClusterStack(final Construct scope, final String id, Vpc vpc) {
        super(scope, id);

        cluster = Cluster.Builder.create(this, "Cluster")
                .vpc(vpc)
                .build();
    }
}