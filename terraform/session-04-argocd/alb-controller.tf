# ── AWS Load Balancer Controller via Helm ────────────────────────────────────
# The IAM role and Pod Identity Association for the controller's service account
# are created in session-03-eks (pod-identity.tf). No annotation needed here —
# EKS Pod Identity injects credentials automatically.

resource "helm_release" "aws_lb_controller" {
  name       = "aws-load-balancer-controller"
  namespace  = "kube-system"
  repository = "https://aws.github.io/eks-charts"
  chart      = "aws-load-balancer-controller"
  version    = var.aws_lb_controller_chart_version

  values = [
    yamlencode({
      clusterName = data.aws_eks_cluster.main.name
      vpcId       = data.aws_eks_cluster.main.vpc_config[0].vpc_id
      region      = "ap-southeast-1"
      serviceAccount = {
        create = true
        name   = "aws-load-balancer-controller"
      }
    })
  ]
}
