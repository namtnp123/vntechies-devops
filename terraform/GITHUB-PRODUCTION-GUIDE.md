# Terraform + GitHub in Production — Student Guide

This guide follows the live demo. It explains **how real teams run Terraform with GitHub**, then gives you **hands-on labs** to try in your own fork.

> **Before you start**
> - Fork the repo and work in **your own AWS account** (sandbox / free tier).
> - Every lab that creates AWS resources ends with a **Cleanup** step. Do it — EKS and NAT Gateways cost money by the hour.
> - Never commit AWS keys, `*.tfstate`, or `*.tfplan` files. Check `.gitignore` before your first commit.

---

## Table of Contents

1. [The big picture: GitOps for infrastructure](#1-the-big-picture-gitops-for-infrastructure)
2. [How this repo is organised](#2-how-this-repo-is-organised)
3. [Git & GitHub commands for production](#3-git--github-commands-for-production)
4. [Production checklist](#4-production-checklist)
5. [Labs](#5-labs)
   - [Lab 1 — State locking](#lab-1--state-locking-two-people-one-state-file)
   - [Lab 2 — OIDC: no more AWS keys in GitHub](#lab-2--oidc-no-more-aws-keys-in-github)
   - [Lab 3 — Policy-as-code blocks a bad PR](#lab-3--policy-as-code-blocks-a-bad-pr)
   - [Lab 4 — Cost guardrail with Infracost](#lab-4--cost-guardrail-with-infracost)
   - [Lab 5 — Destroy guard](#lab-5--destroy-guard)
   - [Lab 6 — Stale plan protection](#lab-6--stale-plan-protection)
   - [Lab 7 — Drift detection](#lab-7--drift-detection)
   - [Lab 8 — Protect the release tag](#lab-8--protect-the-release-tag)
   - [Lab 9 — Rollback has limits](#lab-9--rollback-has-limits)
6. [Challenge: find the bugs in our pipeline](#6-challenge-find-the-bugs-in-our-pipeline)
7. [Glossary](#7-glossary)
8. [Further reading](#8-further-reading)

---

## 1. The big picture: GitOps for infrastructure

**Rule #1: Git is the single source of truth.** Nobody runs `terraform apply` against prod from a laptop. Every change goes through a Pull Request, and every prod deploy is tied to a git tag.

```
 feature branch ──► Pull Request ──► review + automated checks ──► merge to main
                        │                                              │
                        │  fmt / validate / lint / security scan        ▼
                        │  plan dev + plan prod (posted as PR comment)  auto-deploy to DEV
                        │  cost estimate                                │
                                                                        ▼
                                                    git tag v1.2.3 ──► plan PROD
                                                                        │
                                                              human approval (GitHub Environment)
                                                                        │
                                                                        ▼
                                                              apply the SAVED plan to PROD
```

Why this design?

| Practice | Problem it solves |
|---|---|
| Changes only via PR | Every change is reviewed and has an audit trail |
| Plan posted on PR | Reviewers see *what will actually change*, not just code |
| Save the plan, apply the same file | What was approved is exactly what gets applied |
| Prod deploys from tags | Every prod release is named, immutable, and easy to find |
| Manual approval on prod | A human checks the plan before production changes |

---

## 2. How this repo is organised

```
terraform/
├── session-01-ec2/        # VPC + ALB + EC2 Auto Scaling Group
├── session-02-ecs/        # ECS Fargate + ECR + Cloud Map
├── session-03-eks/        # EKS + Karpenter + Pod Identity
├── session-04-argocd/     # ArgoCD + ALB controller (needs session-03 first!)
└── <each session>/
    ├── environments/
    │   ├── dev/  dev.tfbackend  dev.tfvars
    │   └── prod/ prod.tfbackend prod.tfvars
    ├── providers.tf  variables.tf  locals.tf  outputs.tf  ...

.github/workflows/
├── terraform-pr.yml       # PR: validate + plan dev + plan prod + PR comment
├── terraform-deploy.yml   # Deploy to DEV
├── terraform-release.yml  # v*.*.* tag → plan prod → approval → apply prod
└── terraform-destroy.yml  # Manual teardown
```

Each session is a **separate Terraform stack** with its **own state file** in S3:

```
s3://vntechies-bucket/session-01-ec2/dev/terraform.tfstate
s3://vntechies-bucket/session-01-ec2/prod/terraform.tfstate
...
```

**Run commands from inside a session folder:**

```bash
cd terraform/session-01-ec2
terraform init  -backend-config=environments/dev/dev.tfbackend -reconfigure
terraform plan  -var-file=environments/dev/dev.tfvars
terraform apply -var-file=environments/dev/dev.tfvars
```

> 💡 Why separate stacks? A smaller **blast radius**. A mistake in the ECS stack can't destroy your EKS cluster, and each `plan` runs faster.

---

## 3. Git & GitHub commands for production

These are the commands you'll actually use day to day on a team. Each subsection follows one step of the workflow from [section 1](#1-the-big-picture-gitops-for-infrastructure).

> `git` is the version control tool. `gh` is the GitHub CLI (install: `brew install gh`, then `gh auth login`). Both are used below.

### 3.1 One-time setup (do this on every new laptop)

```bash
# Identity — must match your GitHub account email
git config --global user.name  "Your Name"
git config --global user.email "you@example.com"

# Sensible defaults
git config --global init.defaultBranch main
git config --global pull.rebase true          # `git pull` rebases instead of creating merge commits
git config --global rebase.autoStash true     # stash/unstash local changes automatically during rebase
git config --global fetch.prune true          # remove local refs to branches deleted on GitHub
git config --global push.autoSetupRemote true # first `git push` on a new branch just works

# Sign commits and tags with your SSH key (shows "Verified" on GitHub)
git config --global gpg.format ssh
git config --global user.signingkey ~/.ssh/id_ed25519.pub
git config --global commit.gpgsign true
git config --global tag.gpgSign true
# Then add the same public key on GitHub: Settings → SSH and GPG keys → New SSH key → Key type: "Signing Key"

# Log in to GitHub CLI
gh auth login
```

> 💡 **Why sign?** Anyone can set `user.email` to your address and commit "as you". A signature proves the commit really came from your key. Rulesets can **require** signed commits on `main`.

### 3.2 Stop mistakes before they're committed: pre-commit hooks

[`pre-commit`](https://pre-commit.com/) runs checks every time you `git commit`. Create `.pre-commit-config.yaml` in the repo root:

```yaml
repos:
  - repo: https://github.com/antonbabenko/pre-commit-terraform
    rev: v1.96.1
    hooks:
      - id: terraform_fmt
      - id: terraform_validate
      - id: terraform_tflint
  - repo: https://github.com/gitleaks/gitleaks
    rev: v8.21.2
    hooks:
      - id: gitleaks          # blocks commits that contain AWS keys, tokens, passwords
```

```bash
brew install pre-commit tflint gitleaks
pre-commit install            # installs the git hook for this repo
pre-commit run --all-files    # run once against everything
```

CI runs the same checks again. Hooks just give you the feedback **seconds** earlier instead of minutes.

### 3.3 Daily feature workflow

```bash
# 1. Always start from an up-to-date main
git switch main
git pull

# 2. Create a branch with a descriptive name: <type>/<short-description>
git switch -c feat/ec2-add-https-listener

# 3. Work, then review what you changed BEFORE staging
git status
git diff

# 4. Stage selectively. Avoid `git add .` (it's how secrets and tfstate files get committed)
git add terraform/session-01-ec2/alb.tf
git add -p                     # interactive: pick hunks one by one

# 5. Double-check what's staged
git diff --staged

# 6. Commit with a Conventional Commit message
git commit -m "feat(ec2): add HTTPS listener to ALB"

# 7. Push
git push
```

**Conventional Commits** — `<type>(<scope>): <summary>`

| Type | Use for | Example |
|---|---|---|
| `feat` | New infra / feature | `feat(eks): add karpenter spot nodepool` |
| `fix` | Bug fix | `fix(ecs): correct target group health check path` |
| `refactor` | No behaviour change | `refactor(vpc): move subnets to locals` |
| `chore` | Tooling, deps | `chore: bump aws provider to 5.80` |
| `docs` | Docs only | `docs: add drift runbook` |
| `ci` | Workflow changes | `ci: add trivy scan to PR workflow` |

Consistent messages make `git log` readable and let tools generate release notes automatically.

### 3.4 Pull Requests from the terminal

```bash
# Open a PR (fills title/body from your commits)
gh pr create --base main --fill

# Or be explicit, add reviewers and labels
gh pr create --base main \
  --title "feat(ec2): add HTTPS listener to ALB" \
  --body  "Adds a 443 listener with ACM cert. Plan: +2 to add, 0 to destroy." \
  --reviewer teammate1 --label infrastructure

gh pr create --draft --fill          # draft = "not ready for review yet"

# Watch CI checks live (fmt, validate, plan, security scan...)
gh pr checks --watch

# Read the plan comment / reviews
gh pr view --comments
gh pr view --web                     # open in browser

# Review someone else's PR locally
gh pr checkout 42
gh pr review 42 --approve
gh pr review 42 --request-changes --body "This plan destroys the ALB — use a moved block"

# Merge (only after checks pass + approval) and delete the branch
gh pr merge --squash --delete-branch
```

> 💡 **Squash merge** turns a branch's commits into **one** commit on `main`. That keeps history clean and makes it easy to revert a whole change later (see [3.8](#38-undo-safely-revert-vs-reset)).

### 3.5 Keep your branch up to date (and fix review comments)

```bash
# main moved on while you were working → replay your commits on top of it
git fetch origin
git rebase origin/main

# Conflict? Fix the files, then:
git add <fixed-file>
git rebase --continue      # or: git rebase --abort to go back to before you started

# After a rebase your branch history changed, so a normal push is rejected.
# Use --force-with-lease, NEVER plain --force:
git push --force-with-lease
```

> ⚠️ `--force-with-lease` refuses to push if someone else pushed to your branch since you last fetched. Plain `--force` silently overwrites their work.
> **Never force-push `main`.** The branch ruleset should block it anyway.

Fixing review comments neatly:

```bash
git commit --fixup <sha-of-commit-to-fix>    # mark a commit as a fix for an earlier one
git rebase -i --autosquash origin/main       # squash fixups into their target commits
git push --force-with-lease
```

### 3.6 Releasing to production with tags

In this repo, **pushing a `v*.*.*` tag deploys to prod**. Be precise.

```bash
git switch main && git pull

# What's the latest release, and what changed since then?
git describe --tags --abbrev=0                     # e.g. v1.2.3
git log --oneline v1.2.3..HEAD                     # commits that will be released

# Create an ANNOTATED, SIGNED tag (records who, when, why)
git tag -s v1.3.0 -m "Release v1.3.0: HTTPS listener on ALB"

# Push ONLY this tag
git push origin v1.3.0

# Watch the release workflow and approve it in the UI
gh run watch

# Create a GitHub Release with auto-generated notes from PR titles
gh release create v1.3.0 --verify-tag --generate-notes
```

> ⚠️ **Don't use `git push --tags`.** It pushes **every** local tag, including old test tags you forgot about, and each one could trigger a prod deploy.

**Semantic Versioning** — `vMAJOR.MINOR.PATCH`

| Bump | When | Example |
|---|---|---|
| PATCH | Fix, no new resources | `v1.3.0 → v1.3.1` |
| MINOR | New resources, backwards compatible | `v1.3.1 → v1.4.0` |
| MAJOR | Breaking: replaces/destroys resources, changes outputs others depend on | `v1.4.0 → v2.0.0` |

Deleting a tag pushed by mistake (needs bypass permission if tags are protected):
```bash
git tag -d v1.3.0
git push origin --delete v1.3.0
```
If the release workflow already started, cancel it: `gh run cancel <run-id>`.

### 3.7 Hotfix: prod is broken, main has unreleased changes

`main` already contains features that aren't ready for prod, but prod needs a fix **now**.

```bash
# 1. Branch from the tag that's running in prod, not from main
git switch -c hotfix/alb-health-check v1.3.0

# 2. Fix, commit, push, PR — reviews and checks still apply
git commit -am "fix(ec2): correct ALB health check path"
git push
gh pr create --base main --fill

# 3. After approval, tag the HOTFIX commit (not main) as a patch release
git tag -s v1.3.1 -m "Hotfix: ALB health check path"
git push origin v1.3.1

# 4. Merge the PR so main also gets the fix
gh pr merge --squash --delete-branch
```

The fix already exists as a commit and you only need it on another branch? Use cherry-pick:
```bash
git cherry-pick -x <sha>     # -x adds "(cherry picked from commit ...)" to the message for traceability
```

### 3.8 Undo safely: revert vs reset

| Situation | Command | Rewrites history? |
|---|---|---|
| Bad change is **already on `main`** | `git revert <sha>` then PR | ❌ No (safe for shared branches) |
| Revert a **merge commit** | `git revert -m 1 <merge-sha>` | ❌ No |
| Undo your last **local** commit, keep the changes | `git reset --soft HEAD~1` | ✅ Yes (local only!) |
| Unstage a file | `git restore --staged <file>` | — |
| Throw away local edits to a file | `git restore <file>` | — |

```bash
# Revert a bad change that already reached main (the GitOps way)
git switch -c revert/https-listener
git revert <sha>
git push
gh pr create --fill --title "revert: HTTPS listener (breaks health checks)"
```

> **Rule:** on shared branches you **add** commits that undo things (`revert`). You never rewrite them (`reset`, `--force`).

### 3.9 Investigate: who changed what, and when did it break?

```bash
git log --oneline --graph --decorate -20          # recent history as a graph
git log -p -- terraform/session-01-ec2/alb.tf     # full change history of one file
git log -S "t3.micro" --oneline                   # commits that added/removed this text
git blame -L 10,30 terraform/session-01-ec2/alb.tf # who last changed lines 10–30
git show <sha>                                    # one commit in detail
git diff v1.2.3 v1.3.0 -- terraform/              # what changed between two releases

# Find the commit that broke something (binary search through history)
git bisect start
git bisect bad                 # current commit is broken
git bisect good v1.2.3         # this release worked
# git checks out a middle commit → test it → `git bisect good` or `git bisect bad` → repeat
git bisect reset               # done, go back to where you were
```

### 3.10 "I messed up" — recovery commands

```bash
# Lost a commit after a bad reset/rebase? reflog remembers every place HEAD has been
git reflog
git switch -c rescue <sha-from-reflog>

# Committed to main by accident (not pushed yet)
git switch -c feat/my-work      # move the work to a new branch
git switch main
git reset --hard origin/main    # put local main back to match GitHub

# Need to switch tasks with uncommitted work
git stash push -m "wip: alb listener"
git stash list
git stash pop

# Work on a hotfix without touching your current branch
git worktree add ../hotfix-dir v1.3.0
```

### 3.11 🚨 You committed a secret

**The order matters:**

1. **Rotate the secret first.** Deactivate the AWS key in IAM *right now*. Once it has been pushed, assume it's compromised. Bots scan GitHub for keys within minutes.
2. Then remove it from history:
   ```bash
   brew install git-filter-repo
   git filter-repo --path terraform/secret.tfvars --invert-paths   # remove a file from all history
   git push --force-with-lease --all
   ```
3. Tell your team to re-clone, and ask GitHub Support to purge cached views if the repo is public.

**Prevention:** enable **Settings → Code security → Secret scanning → Push protection**. GitHub then **rejects** pushes that contain known secret formats. Combine it with the `gitleaks` pre-commit hook from [3.2](#32-stop-mistakes-before-theyre-committed-pre-commit-hooks).

### 3.12 Operating GitHub Actions from the terminal

```bash
# Runs
gh run list --workflow terraform-release.yml --limit 5
gh run watch                                  # follow the current run live
gh run view <run-id> --log-failed             # only the logs of failed steps
gh run rerun <run-id> --failed                # retry only the failed jobs
gh run cancel <run-id>

# Trigger a workflow_dispatch workflow with inputs
gh workflow run terraform-deploy.yml -f module=session-01-ec2
gh workflow run terraform-destroy.yml -f module=session-01-ec2 -f environment=dev

# Download the saved plan to inspect it locally
gh run download <run-id> -n tfplan-session-01-ec2-prod-v1.3.0
terraform show session-01-ec2-prod.tfplan

# Environment variables and secrets (prefer OIDC role ARNs as variables over keys as secrets)
gh variable set AWS_ROLE_ARN --env dev --body "arn:aws:iam::<account-id>:role/gha-terraform-dev"
gh secret list --env prod

# Issues opened by drift detection
gh issue list --label drift
```

### 3.13 Cheat sheet

| I want to... | Command |
|---|---|
| Start new work | `git switch main && git pull && git switch -c feat/x` |
| See what I changed | `git status`, `git diff`, `git diff --staged` |
| Stage part of a file | `git add -p` |
| Open a PR | `gh pr create --fill` |
| Watch CI | `gh pr checks --watch` |
| Update my branch | `git fetch && git rebase origin/main` |
| Push after rebase | `git push --force-with-lease` |
| Merge PR | `gh pr merge --squash --delete-branch` |
| Release to prod | `git tag -s v1.2.3 -m "..." && git push origin v1.2.3` |
| Undo on main | `git revert <sha>` (via PR) |
| Find a lost commit | `git reflog` |
| Find the breaking commit | `git bisect` |
| Debug a failed run | `gh run view <id> --log-failed` |

### 3.14 Git exercises

1. Configure SSH commit signing. Push a commit and confirm it shows **Verified** on GitHub.
2. Install the pre-commit hooks, then try to commit a file containing `AKIAIOSFODNN7EXAMPLE` (AWS's documented example key). Gitleaks should block the commit.
3. Create two branches that edit the **same line** of `dev.tfvars`. Merge one, then `rebase` the other and resolve the conflict.
4. Merge a PR that changes `desired_capacity`, then undo it with `git revert` **through a new PR**.
5. Make five commits where commit 3 introduces a `terraform validate` error. Use `git bisect` to find it automatically. Run this from inside `terraform/session-01-ec2`:
   ```bash
   git bisect run sh -c 'terraform init -backend=false -input=false >/dev/null && terraform validate'
   ```
6. Run the full hotfix flow from [3.7](#37-hotfix-prod-is-broken-main-has-unreleased-changes) and check with `git log --graph` that `main` ends up containing the fix.

---

## 4. Production checklist

Use this list to review any Terraform + GitHub setup, including your capstone project.

### 🔐 Security
- [ ] **OIDC instead of access keys.** GitHub assumes an IAM role, so no long-lived secrets are stored. → [Lab 2](#lab-2--oidc-no-more-aws-keys-in-github)
- [ ] **Separate roles:** read-only for PR plans, write access only for apply jobs
- [ ] **One role per environment** (dev role can't touch prod)
- [ ] **Protected release tags.** If a tag deploys prod, not everyone should be able to push one. → [Lab 8](#lab-8--protect-the-release-tag)
- [ ] **Actions pinned to a commit SHA** (`uses: actions/checkout@<sha>`), kept updated by Dependabot
- [ ] **Plan artifacts treated as secrets.** Plans can contain passwords. Use short `retention-days`.
- [ ] **Never use `pull_request_target`** to run Terraform on fork PRs

### 🛡️ Governance
- [ ] Branch **ruleset** on `main`: PR required, status checks required, no force push
- [ ] **CODEOWNERS** so the right team must review each folder
- [ ] `prod` **Environment** with required reviewers + "Prevent self-review"
- [ ] `prod` Environment restricted to `v*` tags

### 🧪 Quality
- [ ] `terraform fmt -check` and `terraform validate`
- [ ] `tflint` (catches invalid instance types, unused variables, ...)
- [ ] Security scan: `trivy config` or `checkov` → [Lab 3](#lab-3--policy-as-code-blocks-a-bad-pr)
- [ ] Cost estimate on PR → [Lab 4](#lab-4--cost-guardrail-with-infracost)
- [ ] Guard against destroying resources by accident → [Lab 5](#lab-5--destroy-guard)

### ⚙️ Operations
- [ ] **State locking** enabled → [Lab 1](#lab-1--state-locking-two-people-one-state-file)
- [ ] S3 state bucket: versioning ON, encryption ON, public access blocked
- [ ] `concurrency:` group per stack + environment in workflows
- [ ] Scheduled **drift detection** → [Lab 7](#lab-7--drift-detection)
- [ ] `prevent_destroy` on stateful resources (databases, buckets)

---

## 5. Labs

### Lab 1 — State locking: two people, one state file

**Goal:** See why state locking matters.

**Concept:** Without a lock, two `terraform apply` runs can write the same state file at the same time and corrupt it.

**Steps**

1. Open `terraform/session-01-ec2/environments/dev/dev.tfbackend` and add:
   ```hcl
   use_lockfile = true   # native S3 locking, needs Terraform >= 1.10
   ```
   > On older Terraform versions you need a DynamoDB table (`dynamodb_table = "terraform-locks"`) instead.
2. Make sure your local Terraform is `>= 1.10` (`terraform version`), then re-init:
   ```bash
   terraform init -backend-config=environments/dev/dev.tfbackend -reconfigure
   ```
3. Open **two terminals** in the same folder and run in both, quickly one after the other:
   ```bash
   terraform apply -var-file=environments/dev/dev.tfvars
   ```

**Expected result:** The second terminal shows:
```
Error: Error acquiring the state lock
Lock Info:
  ID:        ...
  Who:       you@your-laptop
  Operation: OperationTypeApply
```

**Discuss:** What happens if CI crashes while holding the lock? (Answer: `terraform force-unlock <ID>`, used carefully and only after you're sure nobody is running.)

---

### Lab 2 — OIDC: no more AWS keys in GitHub

**Goal:** Remove `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` from GitHub secrets.

**Concept:** GitHub issues a short-lived signed token (JWT) to each workflow run. AWS trusts GitHub as an identity provider and exchanges the token for temporary credentials (about 1 hour). Nothing long-lived is stored, so there's nothing to leak.

```
GitHub Actions ──(JWT: repo, branch, environment)──► AWS STS ──► temporary credentials
```

**Steps**

1. Create the identity provider and role (Terraform, run once with admin credentials):
   ```hcl
   resource "aws_iam_openid_connect_provider" "github" {
     url             = "https://token.actions.githubusercontent.com"
     client_id_list  = ["sts.amazonaws.com"]
     thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
   }

   resource "aws_iam_role" "gha_dev" {
     name = "gha-terraform-dev"
     assume_role_policy = jsonencode({
       Version = "2012-10-17"
       Statement = [{
         Effect    = "Allow"
         Principal = { Federated = aws_iam_openid_connect_provider.github.arn }
         Action    = "sts:AssumeRoleWithWebIdentity"
         Condition = {
           StringEquals = {
             "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
           }
           StringLike = {
             # Only workflows in YOUR repo, running in the "dev" environment
             "token.actions.githubusercontent.com:sub" = "repo:<your-user>/<your-repo>:environment:dev"
           }
         }
       }]
     })
   }
   # Attach the permissions the role needs (for the lab: PowerUserAccess; in real life: least privilege)
   ```
2. In the workflow, replace the `AWS_ACCESS_KEY_ID` env vars with:
   ```yaml
   permissions:
     id-token: write   # required to request the OIDC token
     contents: read

   steps:
     - uses: aws-actions/configure-aws-credentials@v4
       with:
         role-to-assume: arn:aws:iam::<account-id>:role/gha-terraform-dev
         aws-region: ap-southeast-1
   ```
3. Delete the old secrets from **Settings → Environments → dev**.

**Expected result:** The workflow still deploys. In **CloudTrail**, the event shows the assumed role, not an IAM user.

**Break it:** Change the `sub` condition to `environment:prod` and re-run the dev job. You get `Not authorized to perform sts:AssumeRoleWithWebIdentity`. This shows the trust policy is doing its job.

---

### Lab 3 — Policy-as-code blocks a bad PR

**Goal:** Automatically reject insecure infrastructure before it gets merged.

**Steps**

1. Add this job to `terraform-pr.yml`:
   ```yaml
   security-scan:
     name: Security Scan
     runs-on: ubuntu-latest
     permissions:
       contents: read
       security-events: write   # to upload results to the Security tab
     steps:
       - uses: actions/checkout@v4
       - uses: aquasecurity/trivy-action@master
         with:
           scan-type: config
           scan-ref: terraform/
           format: sarif
           output: trivy.sarif
           severity: HIGH,CRITICAL
           exit-code: "1"
       - uses: github/codeql-action/upload-sarif@v3
         if: always()
         with:
           sarif_file: trivy.sarif
   ```
2. On a new branch, add an insecure rule to a security group in `session-01-ec2`:
   ```hcl
   ingress {
     from_port   = 22
     to_port     = 22
     protocol    = "tcp"
     cidr_blocks = ["0.0.0.0/0"]   # SSH open to the whole internet 😱
   }
   ```
3. Open a PR.

**Expected result:** The **Security Scan** check fails, and the finding shows up as an annotation on the exact line in the PR's **Files changed** tab.

**Discuss:** What if a finding is a false positive? (Answer: suppress it with an inline comment such as `#trivy:ignore:AVD-AWS-0107` plus a written reason, so the exception is reviewed in the PR too.)

---

### Lab 4 — Cost guardrail with Infracost

**Goal:** Show reviewers how much a change costs **before** it's merged.

**Steps**

1. Get a free API key at [infracost.io](https://www.infracost.io/) and add it as the repo secret `INFRACOST_API_KEY`.
2. Add to `terraform-pr.yml`:
   ```yaml
   cost:
     name: Cost Estimate
     runs-on: ubuntu-latest
     permissions:
       contents: read
       pull-requests: write
     steps:
       - uses: actions/checkout@v4
         with:
           ref: ${{ github.event.pull_request.base.ref }}
       - uses: infracost/actions/setup@v3
         with:
           api-key: ${{ secrets.INFRACOST_API_KEY }}
       - name: Baseline cost (main branch)
         run: |
           infracost breakdown --path terraform/session-01-ec2 \
             --terraform-var-file environments/dev/dev.tfvars \
             --format json --out-file /tmp/base.json
       - uses: actions/checkout@v4
       - name: Diff against PR
         run: |
           infracost diff --path terraform/session-01-ec2 \
             --terraform-var-file environments/dev/dev.tfvars \
             --compare-to /tmp/base.json \
             --format json --out-file /tmp/diff.json
       - name: Comment on PR
         run: |
           infracost comment github --path /tmp/diff.json \
             --repo ${{ github.repository }} \
             --pull-request ${{ github.event.pull_request.number }} \
             --github-token ${{ github.token }} \
             --behavior update
   ```
3. On a branch, change `instance_type = "t3.micro"` to `"m5.4xlarge"` in `dev.tfvars` and open a PR.

**Expected result:** A PR comment shows the monthly cost going up by several hundred dollars.

---

### Lab 5 — Destroy guard

**Goal:** Stop a PR that would **delete** infrastructure unless someone explicitly allows it.

**Concept:** Renaming a resource in code looks harmless, but Terraform sees it as "destroy the old one, create a new one". For a database, that means data loss.

**Steps**

1. In the plan job, after `terraform plan -out=tfplan`, add:
   ```yaml
   - name: Block destroys unless labelled
     if: ${{ !contains(github.event.pull_request.labels.*.name, 'allow-destroy') }}
     run: |
       DELETES=$(terraform show -json tfplan \
         | jq -r '.resource_changes[] | select(.change.actions | index("delete")) | .address')
       if [ -n "$DELETES" ]; then
         echo "::error::This plan DESTROYS resources:"
         echo "$DELETES"
         echo "Add the 'allow-destroy' label if this is intentional."
         exit 1
       fi
   ```
2. On a branch, rename a resource, for example `resource "aws_lb" "main"` → `resource "aws_lb" "app"`, and update the references to it.
3. Open a PR. The check fails.
4. **Fix it the right way** with a `moved` block, which tells Terraform the resource was renamed and not replaced:
   ```hcl
   moved {
     from = aws_lb.main
     to   = aws_lb.app
   }
   ```

**Expected result:** After adding `moved`, the plan shows `0 to destroy` and the check passes.

---

### Lab 6 — Stale plan protection

**Goal:** Understand why we **save** the plan and apply that exact file.

**Steps**

1. Push a tag to start a prod release: `git tag v0.1.0 && git push origin v0.1.0`
2. Wait until the workflow is **waiting for approval**. Don't approve yet.
3. Meanwhile, change the same prod state another way, for example run a dev → prod change from a second tag, or apply locally.
4. Now approve the first release.

**Expected result:**
```
Error: Saved plan is stale
The given plan file can no longer be applied because the state was changed
by another operation after the plan was created.
```

**Discuss:** Why is failing here *good*? (Answer: the reviewer approved plan A. Applying it on top of a different state would make changes nobody reviewed.)

---

### Lab 7 — Drift detection

**Goal:** Find out when someone changes infrastructure outside Terraform ("ClickOps").

**Concept:** `terraform plan -detailed-exitcode` returns:
- `0` = no changes
- `1` = error
- `2` = changes found → **drift**

**Steps**

1. Create `.github/workflows/terraform-drift.yml`:
   ```yaml
   name: Terraform Drift Detection
   on:
     schedule:
       - cron: "0 6 * * 1-5"   # weekdays 06:00 UTC
     workflow_dispatch:

   permissions:
     contents: read
     issues: write
     id-token: write

   jobs:
     drift:
       runs-on: ubuntu-latest
       environment: dev
       strategy:
         fail-fast: false
         matrix:
           module: [session-01-ec2, session-02-ecs]
       defaults:
         run:
           working-directory: terraform/${{ matrix.module }}
       steps:
         - uses: actions/checkout@v4
         - uses: hashicorp/setup-terraform@v3
           with:
             terraform_wrapper: false   # so we get the real exit code
         - uses: aws-actions/configure-aws-credentials@v4
           with:
             role-to-assume: ${{ vars.AWS_ROLE_ARN }}
             aws-region: ap-southeast-1
         - run: terraform init -backend-config=environments/dev/dev.tfbackend
         - name: Plan
           id: plan
           run: |
             set +e
             terraform plan -detailed-exitcode -no-color \
               -var-file=environments/dev/dev.tfvars > plan.txt
             echo "exitcode=$?" >> "$GITHUB_OUTPUT"
         - name: Open issue on drift
           if: steps.plan.outputs.exitcode == '2'
           env:
             GH_TOKEN: ${{ github.token }}
           run: |
             gh issue create \
               --title "Drift detected: ${{ matrix.module }} (dev)" \
               --label drift \
               --body "$(printf '```\n%s\n```' "$(tail -c 60000 plan.txt)")"
         - if: steps.plan.outputs.exitcode == '1'
           run: exit 1
   ```
2. Create the `drift` label under **Issues → Labels**.
3. In the **AWS Console**, change something by hand, for example add a tag to the ALB or change the ASG desired capacity.
4. Run the workflow manually (**Actions → Terraform Drift Detection → Run workflow**).

**Expected result:** A new GitHub issue appears showing exactly what was changed in the console.

**Discuss:** Two ways to fix drift: run `apply` to revert the console change, or update the code to match reality. Who decides which?

---

### Lab 8 — Protect the release tag

**Goal:** In this pipeline, **pushing a tag = deploying prod**. Make sure not everyone can do that.

**Steps**

1. **Settings → Rules → Rulesets → New ruleset → New tag ruleset**
2. Target: **Include by pattern** → `v*`
3. Enable: **Restrict creations**, **Restrict updates**, **Restrict deletions**
4. **Bypass list:** add only the release managers (or a "release" team)
5. **Settings → Environments → prod → Deployment branches and tags** → add tag rule `v*`, and tick **Prevent self-review**.

**Test it:** From an account that isn't on the bypass list:
```bash
git tag v9.9.9 && git push origin v9.9.9
```
**Expected result:** `remote: error: GH013: Repository rule violations found` — the push is rejected.

---

### Lab 9 — Rollback has limits

**Goal:** Learn what "rollback" can and **cannot** do with Terraform.

**Concept:** Rolling back means re-applying an older, known-good commit:
```bash
git log --oneline --decorate | grep 'tag: v'    # find the last good release
git tag v1.2.4 <good-commit-sha>
git push origin v1.2.4
```

**Thought experiment (or do it in dev):**
1. Release `v1.0.0` with an ECR repository that contains images.
2. Release `v1.1.0`, which removes the ECR repository from the code.
3. Roll back to the `v1.0.0` code as `v1.1.1`.

**Question:** Do your images come back? **No.** Terraform recreates an *empty* repository. Rollback restores **configuration**, not **data**.

**How production teams protect data:**
```hcl
resource "aws_db_instance" "main" {
  # ...
  deletion_protection = true
  lifecycle {
    prevent_destroy = true   # terraform plan errors out instead of destroying
  }
}
```
Add backups and snapshots, plus the destroy guard from [Lab 5](#lab-5--destroy-guard).

---

## 6. Challenge: find the bugs in our pipeline

Real pipelines have bugs too. Our workflows in `.github/workflows/` have several. Can you find and fix them? Open a PR with your fix.

| # | Hint |
|---|---|
| 1 | In `terraform-release.yml`, look at which **environment** the *plan* jobs use. When does the approval prompt appear, before or after the plan exists? |
| 2 | In `terraform-pr.yml`, the `plan-prod` job reads AWS secrets. From **which** environment? |
| 3 | The README says `terraform-deploy.yml` runs on push to `main`. Read the `on:` block. Does it? |
| 4 | Branch protection asks for a check named `Plan / Dev`. Matrix jobs produce names like `Plan / Dev / session-01-ec2`. Will the required check ever be satisfied? |
| 5 | Look at the `.tfbackend` files. Is there any state locking? (See [Lab 1](#lab-1--state-locking-two-people-one-state-file).) |
| 6 | Every PR plans **all four** stacks, even if you only changed one. How could you plan only what changed? (Hint: `dorny/paths-filter`.) |

<details>
<summary>Show answers</summary>

1. The plan jobs use `environment: prod`, so the reviewer is asked to approve **before** the plan exists, then again for apply. Fix: plan jobs use `prod-plan` (no reviewers), and only apply jobs use `prod`.
2. It has no `environment:`, so it falls back to **repository** secrets. Fix: add `environment: prod-plan`.
3. No. It's `workflow_dispatch` only. Add `push: branches: [main]` with `paths:` filters.
4. No. Required check names must match exactly. Either list each matrix name, or add one final job (for example `plan-summary`) that `needs:` all plan jobs and require only that.
5. No. Add `use_lockfile = true` (Terraform >= 1.10) or a DynamoDB lock table.
6. Use `dorny/paths-filter` to output the list of changed modules, then feed it to the matrix with `fromJSON(...)`.

</details>

---

## 7. Glossary

| Term | Meaning |
|---|---|
| **State file** | Terraform's record of what it created. Lives in S3, never in git. |
| **State lock** | Stops two runs from writing the state at the same time. |
| **Plan file (`tfplan`)** | Saved, binary output of `terraform plan`. Applying it does exactly what was planned. |
| **Drift** | Real infrastructure no longer matches the code (usually because of console changes). |
| **OIDC** | Lets GitHub get temporary AWS credentials without stored keys. |
| **GitHub Environment** | A named target (dev/prod) with its own secrets, approval rules, and branch/tag restrictions. |
| **Ruleset** | GitHub rules that protect branches or tags (who can push, which checks must pass). |
| **CODEOWNERS** | File that automatically requests reviews from the owners of a folder. |
| **SARIF** | Standard format for scan results; GitHub shows them inline in PRs. |
| **Blast radius** | How much can break if one change goes wrong. Smaller stacks mean a smaller blast radius. |

---

## 8. Further reading

- GitHub — [Configuring OpenID Connect in AWS](https://docs.github.com/en/actions/security-for-github-actions/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services)
- GitHub — [Managing environments for deployment](https://docs.github.com/en/actions/managing-workflow-runs-and-deployments/managing-deployments/managing-environments-for-deployment)
- GitHub — [About rulesets](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets)
- GitHub — [Security hardening for GitHub Actions](https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions)
- Terraform — [S3 backend (state locking)](https://developer.hashicorp.com/terraform/language/backend/s3)
- Terraform — [Refactoring with `moved` blocks](https://developer.hashicorp.com/terraform/language/modules/develop/refactoring)
- [Trivy — misconfiguration scanning](https://trivy.dev/latest/docs/scanner/misconfiguration/)
- [Infracost GitHub Actions](https://github.com/infracost/actions)
