import jenkins.model.*
import javaposse.jobdsl.plugin.ExecuteDslScripts

println "[INIT] Running Job DSL seed setup..."

def seedJobName = 'seed-job'
def jenkinsFile = """
    pipeline {
      agent any
      stages {
        stage('Generate DSL jobs') {
          steps {
            sh "rm -rf dsl 2>/dev/null"
            sh "cp -r /var/jenkins_home/provisioning/dsl ."
            jobDsl targets: 'dsl/**/*.groovy'
          }
        }
      }
    }
"""
def jenkins = Jenkins.instance
// if (jenkins.getItem(seedJobName) == null) {
    def seed = jenkins.createProject(org.jenkinsci.plugins.workflow.job.WorkflowJob, seedJobName)
    seed.definition = new org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition(jenkinsFile, true)
    seed.save()
    println "[INIT] Created seed pipeline."
// } else {
//     println "[INIT] Seed job already exists."
// }