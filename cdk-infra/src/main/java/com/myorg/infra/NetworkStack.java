package com.myorg.infra;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.constructs.Construct;
import software.amazon.awscdk.services.ec2.Vpc;

import java.util.List;

public class NetworkStack extends Stack {
    public final Vpc vpc;

    public NetworkStack(final Construct scope, final String id) {
        super(scope, id);

        vpc = Vpc.Builder.create(this, "Vpc")
                .maxAzs(2)
                .natGateways(0) // keep free-tier safe
                .subnetConfiguration(List.of(
                        SubnetConfiguration.builder()
                                .name("Public")
                                .subnetType(SubnetType.PUBLIC)
                                .cidrMask(24)
                                .build(),
                        SubnetConfiguration.builder()
                                .name("Private")
                                .subnetType(SubnetType.PRIVATE_ISOLATED)
                                .cidrMask(24)
                                .build()
                ))
                .build();

    }
}