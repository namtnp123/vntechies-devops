# Karpenter Kustomize Structure

This directory contains Karpenter manifests organized with Kustomize for multi-environment support.

## Structure

- `base/` - Common Karpenter resources (EC2NodeClass, NodePool, Demo Deployment)
- `overlays/dev/` - Development environment overrides (replicas: 2)
- `overlays/prod/` - Production environment overrides (replicas: 5)

## Building Manifests

```bash
# Build dev environment
kustomize build karpenter/overlays/dev

# Build prod environment
kustomize build karpenter/overlays/prod

# Apply to cluster
kubectl apply -k karpenter/overlays/dev
```

## Environment Differences

| Aspect | Dev | Prod |
|--------|-----|------|
| Demo Replicas | 2 | 5 |
| EC2NodeClass Role | dev-eks-karpenter-node | prod-eks-karpenter-node |
| Discovery Tags | dev-eks | prod-eks |
| NodePool CPU Limit | 8 | 16 |
| NodePool Memory Limit | 16Gi | 32Gi |

## Customization

Each overlay applies strategic merge patches to the base resources via `patches.yaml`.
