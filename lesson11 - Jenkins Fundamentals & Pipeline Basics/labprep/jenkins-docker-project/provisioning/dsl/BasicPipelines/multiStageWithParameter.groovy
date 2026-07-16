pipelineJob('DevOpsBootcamp/BasicPipelines/MultiStagePipelineWithParameter') {
    description('A pipeline created automatically')
    definition {
        cps {
            script("""
                node {
                    properties([
                        parameters([
                            string(name: 'ENVIRONMENT', defaultValue: 'dev', description: 'Target environment for deployment'),
                            choice(name: 'BUILD_TYPE', choices: ['debug', 'release'], description: 'Type of build to perform'),
                            booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: 'Skip running tests'),
                            text(name: 'RELEASE_NOTES', defaultValue: '', description: 'Release notes for this build')
                        ])
                    ])
                    
                    stage('Build') {
                        echo "Building the project..."
                        echo "Build Type: \${params.BUILD_TYPE}"
                        echo "Environment: \${params.ENVIRONMENT}"
                    }
                    stage('Test') {
                        if (params.SKIP_TESTS) {
                            echo "Skipping tests as requested"
                        } else {
                            echo "Running tests..."
                        }
                    }
                    stage('Deploy') {
                        echo "Deploying application to \${params.ENVIRONMENT}..."
                        if (params.RELEASE_NOTES) {
                            echo "Release Notes: \${params.RELEASE_NOTES}"
                        }
                    }
                }
            """)
            sandbox()
        }
    }
}