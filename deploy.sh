#!/bin/bash
set -e

# ============================================================
# Configuration
# ============================================================
AWS_ACCOUNT_ID=621541294877
AWS_REGION=us-east-1
ECR_REPO_NAME=practice-repo/user-service
ECR_REPO="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO_NAME"
SERVICE_NAME=user-service

# Auto-increment build number by querying ECR for the latest tag
get_next_build_number() {
    # Check if any images exist in the repo
    IMAGE_COUNT=$(aws ecr describe-images \
        --repository-name "$ECR_REPO_NAME" \
        --region "$AWS_REGION" \
        --query "length(imageDetails)" \
        --output text 2>/dev/null || echo "0")

    if [ "$IMAGE_COUNT" = "0" ] || [ "$IMAGE_COUNT" = "None" ]; then
        echo "1"
        return
    fi

    # Find the highest user-service-XX tag number
    LAST_NUMBER=$(aws ecr list-images \
        --repository-name "$ECR_REPO_NAME" \
        --region "$AWS_REGION" \
        --query "imageIds[*].imageTag" \
        --output text 2>/dev/null | \
        tr '\t' '\n' | \
        grep -oE "$SERVICE_NAME-[0-9]+" | \
        grep -oE '[0-9]+$' | \
        sort -n | tail -1 || echo "0")

    echo $((LAST_NUMBER + 1))
}

BUILD_NUMBER=$(get_next_build_number)
IMAGE_TAG="$SERVICE_NAME-$(printf '%02d' $BUILD_NUMBER)"
LATEST_TAG="${SERVICE_NAME}_latest"
echo "📌 Next image tag: $IMAGE_TAG"
echo "📌 Latest tag:     $LATEST_TAG"

# ============================================================
# Functions
# ============================================================
build_jar() {
    echo "🔨 Building JAR..."
    ./gradlew bootJar -x test
    echo "✅ JAR built successfully"
}

ecr_login() {
    echo "🔐 Logging in to ECR..."
    aws ecr get-login-password --region "$AWS_REGION" | \
        docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
    echo "✅ ECR login successful"
}

build_image() {
    echo "🐳 Building Docker image for linux/amd64..."
    docker build --platform linux/amd64 -t "$ECR_REPO:$LATEST_TAG" .
    docker tag "$ECR_REPO:$LATEST_TAG" "$ECR_REPO:$IMAGE_TAG"
    echo "✅ Image built: $ECR_REPO:$LATEST_TAG"
    echo "✅ Image tagged: $ECR_REPO:$IMAGE_TAG"
}

push_image() {
    echo "🚀 Pushing image to ECR..."
    docker push "$ECR_REPO:$LATEST_TAG"
    docker push "$ECR_REPO:$IMAGE_TAG"
    echo "✅ Pushed: $ECR_REPO:$LATEST_TAG"
    echo "✅ Pushed: $ECR_REPO:$IMAGE_TAG"
}

deploy_all() {
    build_jar
    ecr_login
    build_image
    push_image
    echo ""
    echo "============================================"
    echo "🎉 Deployment complete!"
    echo "   Image: $ECR_REPO:$IMAGE_TAG"
    echo "============================================"
}

# ============================================================
# Command router
# ============================================================
usage() {
    echo ""
    echo "Usage: ./deploy.sh <command>"
    echo ""
    echo "Commands:"
    echo "  all       Build JAR, Docker image, and push to ECR (full pipeline)"
    echo "  build     Build JAR only"
    echo "  login     Login to ECR only"
    echo "  image     Build Docker image only (assumes JAR exists)"
    echo "  push      Push image to ECR only (assumes image exists)"
    echo "  quick     Skip JAR build — just login, build image, and push"
    echo ""
}

case "${1:-all}" in
    all)     deploy_all ;;
    build)   build_jar ;;
    login)   ecr_login ;;
    image)   build_image ;;
    push)    push_image ;;
    quick)   ecr_login && build_image && push_image ;;
    help|-h) usage ;;
    *)       echo "❌ Unknown command: $1" && usage && exit 1 ;;
esac

