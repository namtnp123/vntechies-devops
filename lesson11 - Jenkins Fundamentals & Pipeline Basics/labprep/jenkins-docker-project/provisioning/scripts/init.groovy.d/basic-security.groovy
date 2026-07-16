#!groovy

import jenkins.model.*
import hudson.security.*
import hudson.security.csrf.DefaultCrumbIssuer
import jenkins.security.s2m.AdminWhitelistRule

def instance = Jenkins.getInstance()
def env = System.getenv()

// Create admin user
def hudsonRealm = new HudsonPrivateSecurityRealm(false)
def username = env['JENKINS_ADMIN_USER'] ?: "admin"
def password = env['JENKINS_ADMIN_PASSWORD'] ?: "admin123"
hudsonRealm.createAccount(username, password)
instance.setSecurityRealm(hudsonRealm)

// Set authorization strategy
def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
strategy.setAllowAnonymousRead(false)
instance.setAuthorizationStrategy(strategy)

// Enable CSRF protection
instance.setCrumbIssuer(new DefaultCrumbIssuer(true))

// Disable remoting security
instance.getDescriptor("jenkins.CLI").get().setEnabled(false)

// Configure agent protocols
Set<String> agentProtocolsList = ['JNLP4-connect', 'Ping']
instance.setAgentProtocols(agentProtocolsList)

// Save configuration
instance.save()

println("✅ Basic security configuration completed!")
println("📋 Default admin credentials: admin/admin123")
println("⚠️  Please change the default password after first login!")