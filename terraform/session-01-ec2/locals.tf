locals {
  common_tags = {
    env             = var.env
    project         = "pjA"
    managed_by      = "terraform"
    release_version = var.release_version
    session         = "01-ec2"
  }
}
