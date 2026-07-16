pipelineJob('DevOpsBootcamp/SampleSnippets/GitShellCurlSnippet') {
    description('A pipeline demonstrating git clone, shell commands, and curl operations')
    definition {
        cps {
            script("""
                node {
                    // Global variables
                    def repoUrl = 'https://github.com/devopsway/devops-project-application-cowsay-frontend'
                    def branch = 'main'
                    def workDir = 'cloned-repo'
                    def apiUrl = 'https://api.github.com/repos/devopsway/devops-project-application-cowsay-frontend'
                    
                    stage('Git Clone Demo') {
                        echo "Cloning repository: \${repoUrl}"
                        
                        // Clean workspace first
                        sh 'rm -rf *'
                        
                        // Git clone using git command
                        git url: repoUrl, branch: branch
                        
                        // Alternative: Using sh command for git clone
                        // sh "git clone -b \${branch} \${repoUrl} \${workDir}"
                        
                        echo "Repository cloned successfully"
                        
                        // List files in the repository
                        sh 'ls -la'
                    }
                    
                    stage('Shell Commands Demo') {
                        echo "Demonstrating various shell commands"
                        
                        // Basic shell commands
                        sh '''
                            echo "=== System Information ==="
                            uname -a
                            echo ""
                            
                            echo "=== Current Directory ==="
                            pwd
                            echo ""
                            
                            echo "=== Directory Contents ==="
                            ls -la
                            echo ""
                            
                            echo "=== File Count ==="
                            find . -type f | wc -l
                            echo ""
                        '''
                        
                        // Conditional shell execution
                        script {
                            def fileExists = sh(
                                script: 'test -f package.json && echo "exists" || echo "missing"',
                                returnStdout: true
                            ).trim()
                            
                            echo "Package.json \${fileExists}"
                            
                            if (fileExists == 'exists') {
                                sh '''
                                    echo "=== Package.json Contents ==="
                                    cat package.json
                                '''
                            }
                        }
                        
                        // Shell command with return value
                        def gitCommitHash = sh(
                            script: 'git rev-parse HEAD',
                            returnStdout: true
                        ).trim()
                        
                        echo "Current commit hash: \${gitCommitHash}"
                        
                        // Environment variables in shell
                        sh '''
                            export CUSTOM_VAR="Hello from Jenkins"
                            echo "Custom variable: \$CUSTOM_VAR"
                            
                            echo "Jenkins workspace: \$WORKSPACE"
                            echo "Build number: \$BUILD_NUMBER"
                            echo "Job name: \$JOB_NAME"
                        '''
                    }
                    
                    stage('Curl Commands Demo') {
                        echo "Demonstrating curl operations"
                        
                        // Simple GET request
                        sh '''
                            echo "=== GitHub API Repository Info ==="
                            curl -s ''' + apiUrl + ''' | head -20
                            echo ""
                        '''
                        
                        // Curl with error handling
                        script {
                            def curlResponse = sh(
                                script: "curl -s -w '%{http_code}' -o /tmp/github_response.json " + apiUrl,
                                returnStdout: true
                            ).trim()
                            
                            echo "HTTP Response Code: \${curlResponse}"
                            
                            if (curlResponse == '200') {
                                sh '''
                                    echo "=== Repository Details ==="
                                    cat /tmp/github_response.json | grep -E '"name"|"description"|"stargazers_count"|"language"' | head -10
                                '''
                            } else {
                                echo "Failed to fetch repository info"
                            }
                        }
                        
                        // Multiple curl requests
                        sh '''
                            echo "=== Checking Multiple Endpoints ==="
                            
                            echo "1. GitHub API Status:"
                            curl -s https://api.github.com/rate_limit | grep -E '"limit"|"remaining"'
                            echo ""
                            
                            echo "2. Public IP Address:"
                            curl -s https://api.ipify.org
                            echo ""
                            
                            echo "3. HTTP Headers Test:"
                            curl -I -s https://httpbin.org/get | head -5
                            echo ""
                        '''
                        
                        // POST request with data
                        sh '''
                            echo "=== POST Request Demo ==="
                            curl -X POST \\
                                 -H "Content-Type: application/json" \\
                                 -d '{"message": "Hello from Jenkins", "timestamp": "'"\$(date)"'"}' \\
                                 -s https://httpbin.org/post | grep -A 5 '"json"'
                        '''
                    }
                    
                    stage('Combined Operations') {
                        echo "Combining git, shell, and curl operations"
                        
                        // Get repository statistics
                        script {
                            def fileCount = sh(
                                script: 'find . -name "*.js" -o -name "*.json" -o -name "*.md" | wc -l',
                                returnStdout: true
                            ).trim()
                            
                            def lastCommit = sh(
                                script: 'git log -1 --pretty=format:"%h - %an: %s"',
                                returnStdout: true
                            ).trim()
                            
                            echo "Files found (js/json/md): \${fileCount}"
                            echo "Last commit: \${lastCommit}"
                            
                            // Send stats to a webhook (simulated)
                            sh '''
                                echo "=== Sending Build Statistics ==="
                                curl -X POST \\
                                     -H "Content-Type: application/json" \\
                                     -d '{
                                         "project": "cowsay-frontend",
                                         "build_number": "''' + env.BUILD_NUMBER + '''",
                                         "file_count": "''' + fileCount + '''",
                                         "last_commit": "''' + lastCommit.replaceAll('"', '\\\\"') + '''",
                                         "timestamp": "'"\$(date)"'"
                                     }' \\
                                     -s https://httpbin.org/post | grep '"json"' -A 10
                            '''
                        }
                        
                        // Cleanup
                        sh '''
                            echo "=== Cleanup ==="
                            rm -f /tmp/github_response.json
                            echo "Temporary files cleaned up"
                        '''
                    }
                }
            """)
            sandbox()
        }
    }
}