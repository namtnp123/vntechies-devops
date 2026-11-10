variable "groups" {
  description = "Map of IAM group name to the list of IAM user names that belong to it"
  type        = map(list(string))
  default = {
    grp1 = ["user1", "user2", "user3", "user4", "user5", "user6", "user7", "user8", "user9", "user10"]
    grp2 = ["user11", "user12", "user13", "user14", "user15", "user16", "user17", "user18", "user19", "user20"]
  }
}

variable "group_policy_arns" {
  description = "Map of IAM group name to list of managed policy ARNs to attach"
  type        = map(list(string))
  default = {
    grp1 = ["arn:aws:iam::aws:policy/AccountManagementFromVercel"]
    grp2 = ["arn:aws:iam::aws:policy/AdministratorAccess-Amplify"]
  }
}
