pipelineJob('DevOpsBootcamp/BasicPipelines/CowsayBackendApi') {
    description('A pipeline created automatically')
    definition {
        cps {
            script("""
                node {
                    stage('Checkout') {
                        git url: 'https://github.com/devopsway/devops-project-application-cowsay-api', branch: 'main'
                    }
                    stage('Build') {
                        echo "Building the project..."
                    }
                    stage('Test') {
                        echo "Running tests..."
                    }
                    stage('Build Docker Image') {
                        echo "Building Docker image..."
                    }
                    stage('Push to Registry') {
                        echo "Pushing Docker image to registry..."
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