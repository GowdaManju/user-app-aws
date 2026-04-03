package com.myorg.infra;


/**
 * Configurable properties for ECR repository creation.
 * Use defaults() for production-grade settings.
 */
public record EcrRepositoryProps(
        boolean immutableTags,
        int untaggedRetentionDays,
        int maxTaggedImageCount
) {
    /**
     * Production defaults:
     * - Immutable tags (prevents overwriting pushed images)
     * - Remove untagged images after 7 days
     * - Keep last 20 tagged images
     */
    public static EcrRepositoryProps defaults() {
        return new EcrRepositoryProps(true, 7, 20);
    }

    public static EcrRepositoryProps mutableTags() {
        return new EcrRepositoryProps(false, 7, 20);
    }
}
