pipelineJob('DevOpsBootcamp/BasicPipelines/HelloWorldPipeline') {
    description('A simple Hello World pipeline - Perfect for beginners')
    definition {
        cps {
            script("""
                node {
                    stage('Hello World') {
                        echo "Hello, World!"
                        echo "Welcome to Jenkins Pipeline!"
                        echo "Build Number: \${env.BUILD_NUMBER}"
                        echo "Job Name: \${env.JOB_NAME}"
                    }
                }
            """)
            sandbox()
        }
    }
}
