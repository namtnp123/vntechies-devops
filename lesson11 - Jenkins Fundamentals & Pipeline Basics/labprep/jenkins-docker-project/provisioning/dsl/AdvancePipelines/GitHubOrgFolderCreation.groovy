// Create GitHub Organization Folder for devopsway
organizationFolder('DevOpsBootcamp/AdvancePipelines/DevOpsWay-GitHub-Org') {
    description('Automatically scans and creates pipelines for all repositories in the devopsway GitHub organization')
    
    // Display name for the folder
    displayName('DevOpsWay GitHub Organization')
    
    organizations {
        github {
            // GitHub organization name
            repoOwner('devopsway')
            
            // Credentials for GitHub access (you'll need to configure this in Jenkins)
            credentialsId('GITHUB_PAT_CREDS')
            
            
            // Configure behavior for different types of branches/PRs
            traits {
                // Discover branches
                gitHubBranchDiscovery {
                    strategyId(1) // 1 = Exclude branches that are also filed as PRs
                }
                
                // Discover pull requests from origin
                gitHubPullRequestDiscovery {
                    strategyId(1) // 1 = Merging the pull request with the current target branch revision
                }
                
                // Discover pull requests from forks
                gitHubForkDiscovery {
                    strategyId(1) // 1 = Merging the pull request with the current target branch revision
                    trust {
                        gitHubTrustContributors()
                    }
                }
            }
        }
    }
    
    // Configure the orphaned item strategy
    orphanedItemStrategy {
        discardOldItems {
            daysToKeep(7)
            numToKeep(10)
        }
    }
    
    // Configure periodic folder computation
    triggers {
        periodicFolderTrigger {
            interval('1d') // Check for new repositories daily
        }
    }
    
    // Configure build configuration
    configure { node ->
        def projectFactories = node / 'projectFactories'
        projectFactories / 'org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory' {
            owner(class: 'org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject', reference: '../..')
            scriptPath('Jenkinsfile') // Default Jenkinsfile location
        }
    }
}