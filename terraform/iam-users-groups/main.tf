# resource "aws_iam_user" "this" {
#   for_each = toset(local.all_users)

#   name = each.value
# }

resource "aws_iam_user" "user1" {
  name = "user1"
  tags = {
    Name = "user1"
    Manager = "trannam"
    Team = "devops"
    Office = "Hanoi"
  }
}



# resource "aws_iam_group" "this" {
#   for_each = var.groups

#   name = each.key
# }

# resource "aws_iam_group_membership" "this" {
#   for_each = var.groups

#   name  = "${each.key}-membership"
#   group = aws_iam_group.this[each.key].name
#   users = [for user in each.value : aws_iam_user.this[user].name]
# }

# resource "aws_iam_group_policy_attachment" "this" {
#   for_each = local.group_policy_attachments

#   group      = aws_iam_group.this[each.value.group].name
#   policy_arn = each.value.policy_arn
# }
