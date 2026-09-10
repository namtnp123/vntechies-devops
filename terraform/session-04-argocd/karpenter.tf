# ── Karpenter via Helm ────────────────────────────────────────────────────────
# IAM role and Pod Identity association live in session-03-eks (karpenter.tf).
# The Karpenter service account is named "karpenter" in kube-system, matching
# the Pod Identity association created in session-03-eks/pod-identity.tf.

locals {
  # Derive these from the cluster name so no extra variables are needed.
  karpenter_node_role_name        = "${data.aws_eks_cluster.main.name}-karpenter-node"
  karpenter_interruption_queue    = "${data.aws_eks_cluster.main.name}-karpenter"
}

resource "helm_release" "karpenter" {
  name       = "karpenter"
  namespace  = "kube-system"
  repository = "oci://public.ecr.aws/karpenter"
  chart      = "karpenter"
  version    = var.karpenter_chart_version

  values = [
    yamlencode({
      settings = {
        clusterName       = data.aws_eks_cluster.main.name
        clusterEndpoint   = data.aws_eks_cluster.main.endpoint
        interruptionQueue = local.karpenter_interruption_queue
      }
      serviceAccount = {
        create = true
        name   = "karpenter"
      }
      controller = {
        resources = {
          requests = { cpu = "100m", memory = "256Mi" }
          limits   = { cpu = "1",    memory = "1Gi"   }
        }
      }
      # Run Karpenter itself on the system node group, not on Karpenter-launched nodes.
      tolerations = [
        { key = "CriticalAddonsOnly", operator = "Exists" }
      ]
      nodeSelector = {
        "node.kubernetes.io/purpose" = "system"
      }
    })
  ]

  depends_on = [helm_release.argocd]
}
