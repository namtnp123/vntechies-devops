pipelineJob('DevOpsBootcamp/BasicPipelines/CowsayFrontend') {
    description('A pipeline created automatically')
    definition {
        cps {
            script("""
                node {
                    stage('Checkout') {
                        git url: 'https://github.com/devopsway/devops-project-application-cowsay-frontend', branch: 'main'
                    }
                    stage('Build') {
                        echo "Building the project..."
                    }
                    stage('Test') {
                        echo "Running tests..."
                    }
                    stage('Build Docker Image') {
                        def imageName = "daotoanhd/cowsay-frontend:latest"
                        echo "Building Docker image: \${imageName}"
                        sh "docker build -t \${imageName} ."
                        echo "Docker image built successfully"
                    }
                    stage('Push to Registry') {
                        def imageName = "daotoanhd/cowsay-frontend:latest"
                        echo "Pushing Docker image to registry: \${imageName}"
                        withCredentials([usernamePassword(credentialsId: 'DOCKER_REGISTRY_CREDS', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                            sh "echo \${DOCKER_PASSWORD} | docker login -u \${DOCKER_USERNAME} --password-stdin"
                            sh "docker push \${imageName}"
                        }
                        echo "Docker image pushed successfully to registry"
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