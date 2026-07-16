pipelineJob('DevOpsBootcamp/SampleSnippets/BasicGroovyOperation') {
    description('A pipeline created automatically')
    definition {
        cps {
            script("""
                // Global variables (accessible across all stages)
                def globalCounter = 0
                def globalAppName = "DevOps Bootcamp App"
                
                // Global methods (accessible across all stages)
                def calculateSum(a, b) {
                    return a + b
                }
                
                def formatMessage(msg) {
                    return ">>> \${msg} <<<"
                }

                node {
                    stage('Global Scope Demo') {
                        echo "Global App Name: \${globalAppName}"
                        echo "Global Counter Initial Value: \${globalCounter}"
                        
                        globalCounter = calculateSum(globalCounter, 5)
                        echo "Global Counter After Addition: \${globalCounter}"
                        
                        echo formatMessage("This is using global method")
                    }
                    
                    stage('Local Scope Demo') {
                        // Local variables (only accessible within this stage)
                        def localCounter = 10
                        def localMessage = "Local scope variable"
                        
                        // Local closures (anonymous functions - only accessible within this stage)
                        def multiplyNumbers = { x, y ->
                            return x * y
                        }
                        
                        def processArray = { arr ->
                            def result = []
                            for (item in arr) {
                                result.add("Processed: \${item}")
                            }
                            return result
                        }
                        
                        echo "Local Message: \${localMessage}"
                        echo "Local Counter: \${localCounter}"
                        
                        // Using local closure
                        def product = multiplyNumbers(localCounter, 3)
                        echo "Product (local closure): \${product}"
                        
                        // Demonstrating array processing
                        def items = ['Item1', 'Item2', 'Item3']
                        def processedItems = processArray(items)
                        
                        for (processed in processedItems) {
                            echo processed
                        }
                        
                        // Accessing global variables from local scope
                        echo "Accessing global counter from local scope: \${globalCounter}"
                        globalCounter = calculateSum(globalCounter, localCounter)
                        echo "Updated global counter: \${globalCounter}"
                    }
                    
                    stage('Scope Validation') {
                        // Global variables and methods are still accessible here
                        echo "Final Global Counter: \${globalCounter}"
                        echo formatMessage("Global method still works here")
                        
                        // Note: Local variables from previous stage are NOT accessible here
                        // This would cause an error: echo localMessage
                        
                        // New local scope within this stage
                        def anotherLocalVar = "This is another local variable"
                        echo "Another Local Var: \${anotherLocalVar}"
                        
                        // Conditional logic with different scopes
                        if (globalCounter > 10) {
                            def conditionalLocal = "Counter is greater than 10"
                            echo conditionalLocal
                        } else {
                            def conditionalLocal = "Counter is 10 or less"
                            echo conditionalLocal
                        }
                    }
                }
            """)
            sandbox()
        }
    }
}