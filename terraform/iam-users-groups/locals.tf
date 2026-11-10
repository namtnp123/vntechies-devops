locals {
  # Unique set of all users across every group, since the same user
  # could appear in more than one group's list.
  all_users = distinct(flatten(values(var.groups)))

  # Flatten group -> [policy_arn, ...] into one entry per (group, policy_arn) pair.
  group_policy_attachments = merge([
    for group, arns in var.group_policy_arns : {
      for arn in arns : "${group}-${arn}" => {
        group      = group
        policy_arn = arn
      }
    }
  ]...)
}
