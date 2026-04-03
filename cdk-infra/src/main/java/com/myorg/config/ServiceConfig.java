package com.myorg.config;


public class ServiceConfig {

    public final String serviceName;
    public final String image;
    public final int cpu;
    public final int memory;
    public final int containerPort;

    public ServiceConfig(String serviceName,
                         String image,
                         int cpu,
                         int memory,
                         int containerPort) {
        this.serviceName = serviceName;
        this.image = image;
        this.cpu = cpu;
        this.memory = memory;
        this.containerPort = containerPort;
    }
}