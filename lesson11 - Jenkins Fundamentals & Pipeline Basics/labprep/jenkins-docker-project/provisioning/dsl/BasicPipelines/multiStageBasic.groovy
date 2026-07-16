pipelineJob('DevOpsBootcamp/BasicPipelines/MultiStagePipeline') {
    description('A pipeline created automatically')
    definition {
        cps {
            script("""
                node {
                    stage('Build') {
                        echo "Building the project..."
                    }
                    stage('Test') {
                        echo "Running tests..."
                    }
                    stage('Deploy') {
                        echo "Deploying application..."
                    }
                }
            """)
            sandbox()
        }
    }
}