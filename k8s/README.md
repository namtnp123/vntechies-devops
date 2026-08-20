# Kubernetes Fundamentals Lab

Local hands-on lab using [kind](https://kind.sigs.k8s.io/) (Kubernetes in Docker).

## Prerequisites

Install the required tools before starting.

### kind
```bash
# macOS
brew install kind

# Linux (AMD64)
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.30.0/kind-linux-amd64
chmod +x ./kind && sudo mv ./kind /usr/local/bin/kind
```

### kubectl
```bash
# macOS
brew install kubectl

# Linux
sudo apt-get update && sudo apt-get install -y kubectl
```

### k9s (optional — terminal UI for cluster browsing)
```bash
brew install derailed/k9s/k9s
```

---

## Step 1 — Create the Cluster

```bash
kind create cluster --config=cluster/kind-config.yaml
```

Verify the cluster is up:
```bash
kubectl cluster-info
kubectl get nodes
```

Expected output: 1 control-plane node + 2 worker nodes with status `Ready`.

---

## Step 2 — Namespaces

```bash
kubectl apply -f 01-namespaces/namespaces.yaml
```

Inspect the result:
```bash
kubectl get namespaces --show-labels
kubectl describe namespace dev-environment
```

Switch context to a namespace and back:
```bash
kubectl config set-context --current --namespace=dev-environment
kubectl config set-context --current --namespace=default
```

---

## Step 3 — Pods

### Create pods
```bash
kubectl apply -f 02-pods/nginx-pod.yaml
kubectl apply -f 02-pods/redis-pod.yaml
```

### Inspect pods
```bash
kubectl get pods --show-labels
kubectl describe pod nginx-pod
kubectl logs nginx-pod
```

### Execute into a running pod
```bash
# Shell into nginx
kubectl exec -it nginx-pod -- bash

# Redis CLI
kubectl exec -it redis-pod -- redis-cli
# Inside redis-cli:
# SET greeting "Hello from Kubernetes"
# GET greeting
# EXIT
```

### Port-forward to access locally
```bash
kubectl port-forward pod/nginx-pod 8080:80
# Open http://localhost:8080 in your browser
```

---

## Step 4 — Deployments

```bash
kubectl apply -f 03-deployments/nginx-deployment.yaml
```

Inspect the deployment:
```bash
kubectl get deployments
kubectl get pods -l app=nginx
kubectl describe deployment nginx-deployment
```

### Scale the deployment
```bash
kubectl scale deployment nginx-deployment --replicas=5
kubectl get pods -l app=nginx -w   # watch pods come up
```

### Roll out a new image
```bash
kubectl set image deployment/nginx-deployment nginx=nginx:1.25
kubectl rollout status deployment/nginx-deployment
kubectl rollout history deployment/nginx-deployment
```

### Roll back
```bash
kubectl rollout undo deployment/nginx-deployment
kubectl rollout status deployment/nginx-deployment
```

---

## Step 5 — Services

### ClusterIP (internal cluster access)
```bash
kubectl apply -f 04-services/nginx-clusterip.yaml

kubectl get services
kubectl describe service nginx-service
kubectl get endpoints nginx-service
```

Test from inside the cluster:
```bash
kubectl run test-pod --image=busybox --rm -it --restart=Never -- sh
# Inside the pod:
# wget -qO- http://nginx-service
# exit
```

### NodePort (access from your laptop)
```bash
kubectl apply -f 04-services/nginx-nodeport.yaml

kubectl get service nginx-nodeport
```

Access via port-forward (works with kind):
```bash
kubectl port-forward service/nginx-nodeport 8080:80
# Open http://localhost:8080
```

---

## Step 6 — ConfigMaps & Secrets

### Apply all config resources
```bash
kubectl apply -f 05-configmaps-secrets/app-configmap.yaml
kubectl apply -f 05-configmaps-secrets/nginx-configmap.yaml
kubectl apply -f 05-configmaps-secrets/db-secret.yaml
kubectl apply -f 05-configmaps-secrets/app-with-config.yaml
```

### Inspect ConfigMaps and Secrets
```bash
kubectl get configmaps
kubectl describe configmap app-config
kubectl describe configmap nginx-config

kubectl get secrets
kubectl describe secret db-secret
# View base64-encoded values:
kubectl get secret db-secret -o yaml
# Decode a value:
kubectl get secret db-secret -o jsonpath='{.data.password}' | base64 --decode
```

### Verify env vars inside the pod
```bash
kubectl logs app-with-config | grep -E "APP_NAME|APP_VERSION|DB_"
```

### Test nginx serving response from ConfigMap
```bash
kubectl port-forward pod/nginx-with-configmap 8081:80
# In another terminal:
curl http://localhost:8081
# Expected: Hello from ConfigMap!
```

---

## Step 7 — Troubleshooting

### Create a broken pod
```bash
kubectl apply -f 06-troubleshooting/broken-pod.yaml
kubectl get pods
# Status will show: ErrImagePull or ImagePullBackOff
```

### Debug the pod
```bash
# See the events that explain the failure
kubectl describe pod broken-pod

# List events sorted by time
kubectl get events --sort-by=.metadata.creationTimestamp | grep broken-pod
```

### Fix it by editing inline
```bash
kubectl edit pod broken-pod
# Change: image: nginx:nonexistent-tag
# To:     image: nginx:1.21
# Save and exit — pod will restart automatically
```

### Simulate a CrashLoopBackOff
```bash
kubectl run crash-pod --image=nginx -- /bin/sh -c "exit 1"
kubectl get pods -w
kubectl logs crash-pod
kubectl logs crash-pod --previous
kubectl delete pod crash-pod
```

### Useful debugging commands
```bash
kubectl get all
kubectl get events --sort-by=.metadata.creationTimestamp
kubectl top nodes
kubectl top pods
```

---

## Cleanup

```bash
# Delete all lab resources
kubectl delete -f 05-configmaps-secrets/
kubectl delete -f 04-services/
kubectl delete -f 03-deployments/
kubectl delete -f 02-pods/
kubectl delete -f 01-namespaces/
kubectl delete pod broken-pod --ignore-not-found

# Verify everything is gone
kubectl get all
kubectl get namespaces

# Tear down the cluster
kind delete cluster --name devops-cluster
```

---

## Folder Structure

```
k8s/
├── cluster/
│   └── kind-config.yaml          # kind cluster: 1 control-plane + 2 workers
├── 01-namespaces/
│   └── namespaces.yaml
├── 02-pods/
│   ├── nginx-pod.yaml
│   └── redis-pod.yaml
├── 03-deployments/
│   └── nginx-deployment.yaml
├── 04-services/
│   ├── nginx-clusterip.yaml
│   └── nginx-nodeport.yaml
├── 05-configmaps-secrets/
│   ├── app-configmap.yaml
│   ├── nginx-configmap.yaml
│   ├── db-secret.yaml
│   └── app-with-config.yaml
└── 06-troubleshooting/
    └── broken-pod.yaml
```
