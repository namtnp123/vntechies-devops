# Jenkins Docker Compose Setup

A complete Jenkins CI/CD environment with Docker-in-Docker agent support, pre-installed plugins, and sample pipeline configurations.

## 🏗️ Architecture

This setup includes:
- **Jenkins Master**: Custom Jenkins image with pre-installed plugins
- **Jenkins Agent**: Docker-in-Docker (DinD) agent for containerized builds

## 🚀 Quick Start

### 1. Generate DockerHub PAT and GitHub PAT (personal access token)

- Create file `.env` from `.env.example`
- Replace evn with your generated PAT

### 2. Start Services

```bash
# Or using docker-compose directly
docker-compose up -d
```

### 3. Access Jenkins

- **URL**: http://localhost:9080
- **Default Credentials**: `admin` / `admin123`
- **Install reconmended plugins**
- **Run Seed job to create all sample pipeline**
- **If you dont see seed job, run `docker compose restart`**

## 📋 What's Included

### 🔧 Jenkins Configuration as Code (JCasC)
- **Automated Setup**: Complete Jenkins configuration through `jenkins.yaml`
- **Agent Configuration**: Pre-configured Docker-in-Docker agent node
- **Security Settings**: Basic security configuration with admin user
- **Plugin Management**: Automatic plugin installation and configuration


### 🚀 Sample Pipeline Collection

#### 📁 Basic Pipelines (`BasicPipelines/`)
- **HelloWorldPipeline.groovy**: Simple pipeline introduction
- **CowsayFrontend.groovy**: Basic containerized application pipeline
- **multiStageBasic.groovy**: Multi-stage pipeline demonstration
- **multiStageWithParameter.groovy**: Parameterized pipeline example

#### 📁 Advanced Pipelines (`AdvancePipelines/`)
- **CowsayFrontendAdvance.groovy**: Advanced pipeline with:
  - Multi-environment deployment
  - Docker image building and pushing
  - Security scanning integration
  - Health checks and rollback capabilities
  - Credential usage examples

#### 📁 Organization Structure (`AFolders.groovy`)
- **Folder-based Organization**: Automatic folder creation for pipeline organization
- **Permission Management**: Folder-level access control setup
- **Scalable Structure**: Ready for enterprise-level job organization

### 🐳 Docker Infrastructure

#### Jenkins Master Container
- **Base**: `jenkins/jenkins:lts`
- **Custom Plugins**: Pre-installed plugin suite
- **Docker Integration**: Docker CLI for container operations
- **Volume Persistence**: Configuration and job data persistence
- **JCasC Integration**: Configuration as Code setup

#### Jenkins Agent Container
- **Docker-in-Docker**: Full Docker support for builds
- **Build Tools**: Pre-configured build environment
- **SSH Access**: Secure agent communication
- **Workspace Persistence**: Build artifact preservation
- **Label**: `docker-node` for pipeline targeting

### 📊 Service Configuration

| Service | Port | Purpose | Access |
|---------|------|---------|---------|
| Jenkins Master | 9080 | Main Jenkins interface | http://localhost:9080 |
| Jenkins Agent | Internal | DinD build agent | Internal communication |
| Agent SSH | 50000 | Agent connection | Jenkins protocol |

### 🔄 Automation Features

#### Job DSL Seed Job
- **Automatic Pipeline Creation**: Creates all sample pipelines on startup
- **Folder Organization**: Organizes jobs into logical folders
- **Template-based**: Easy to extend with new pipeline templates
- **Version Controlled**: Pipeline definitions stored in code
