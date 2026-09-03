# k8s-manifest

GitOps source directory watched by ArgoCD. Every file in this directory is automatically applied to the `default` namespace of the `dev-eks` cluster. Changes pushed to `main` are synced within seconds; deleted resources are pruned automatically.

## Files

| File | What it deploys |
|---|---|
| [workloads.yaml](workloads.yaml) | Deployments for all five Fleetman microservices |
| [services.yaml](services.yaml) | Kubernetes Services (LoadBalancer, NodePort, ClusterIP) for each microservice |
| [mongo-stack.yaml](mongo-stack.yaml) | MongoDB Deployment + ClusterIP Service |
| [storage-aws.yaml](storage-aws.yaml) | `cloud-ssd` StorageClass (EBS gp3 via CSI) + `mongo-pvc` PersistentVolumeClaim |
| [argocd-ingress.yaml](argocd-ingress.yaml) | ALB Ingress that exposes the ArgoCD UI publicly |

## Microservices

```
                    ┌─────────────────────┐
internet ──── ALB ──► fleetman-webapp      │  port 80  (LoadBalancer)
                    └──────────┬──────────┘
                               │ HTTP
                    ┌──────────▼──────────┐
                    │  fleetman-api-gateway│  port 8080 (NodePort :30020)
                    └──────────┬──────────┘
             ┌─────────────────┼──────────────────┐
             │                 │                  │
  ┌──────────▼──────┐ ┌────────▼──────────┐ ┌────▼──────────────┐
  │ position-tracker│ │ position-simulator│ │ queue             │
  │ port 8080       │ │ (publishes GPS)   │ │ port 8161 / 61616 │
  └──────────┬──────┘ └───────────────────┘ └───────────────────┘
             │
  ┌──────────▼──────┐
  │ mongodb          │  port 27017 (ClusterIP)
  │ PVC: mongo-pvc   │  7 Gi EBS gp3
  └─────────────────┘
```

## ArgoCD sync settings

- **Repo:** `https://github.com/namtnp123/vntechies-devops`
- **Branch:** `main`
- **Path:** `k8s-manifest/`
- **Auto-sync:** enabled — prune + self-heal

## Prerequisites on the cluster

These must exist before ArgoCD can sync successfully:

| Prerequisite | How it is installed |
|---|---|
| EBS CSI driver | EKS managed addon (`session-03-eks` Terraform) |
| `cloud-ssd` StorageClass | Created by `storage-aws.yaml` itself (first sync) |
| AWS Load Balancer Controller | Helmfile (`helm/argocd/helmfile.yaml`) |
| ArgoCD | Helmfile (`helm/argocd/helmfile.yaml`) |

## Manual bootstrap (first time only)

```bash
# 1. Install infrastructure (LB controller + ArgoCD)
cd helm/argocd
helmfile sync

# 2. Register this repo with ArgoCD (one-time)
kubectl apply -f helm/argocd/argocd-app.yaml

# 3. (Optional) Private repo credentials
#    Edit helm/argocd/argocd-github-secret.yaml, then:
kubectl apply -f helm/argocd/argocd-github-secret.yaml

# 4. Get the ArgoCD UI URL (ready after ~1-2 min)
kubectl get ingress argocd-server -n argocd

# 5. Get the initial admin password
kubectl get secret argocd-initial-admin-secret -n argocd \
  -o jsonpath="{.data.password}" | base64 -d
```

After step 2, ArgoCD manages everything in this directory — including `argocd-ingress.yaml` — with no further manual intervention.
