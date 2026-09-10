output "cluster_name" {
  value = aws_eks_cluster.main.name
}

output "cluster_endpoint" {
  value = aws_eks_cluster.main.endpoint
}

output "cluster_version" {
  value = aws_eks_cluster.main.version
}

output "kubeconfig_command" {
  value       = "aws eks update-kubeconfig --region ap-southeast-1 --name ${aws_eks_cluster.main.name}"
  description = "Run this command to configure kubectl for this cluster."
}

output "aws_lb_controller_role_arn" {
  value       = aws_iam_role.aws_lb_controller.arn
  description = "IAM role ARN for the AWS Load Balancer Controller (Pod Identity)."
}

output "karpenter_node_instance_profile_name" {
  value       = aws_iam_instance_profile.karpenter_node.name
  description = "Instance profile name for Karpenter-launched nodes. Referenced by EC2NodeClass."
}

output "karpenter_interruption_queue_name" {
  value       = aws_sqs_queue.karpenter_interruption.name
  description = "SQS queue name for Karpenter spot interruption handling."
}

output "karpenter_node_role_name" {
  value       = aws_iam_role.karpenter_node.name
  description = "IAM role name for Karpenter-launched nodes. Referenced by EC2NodeClass."
}
