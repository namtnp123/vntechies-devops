# Prometheus, Grafana & Loki Monitoring Stack

This Docker Compose setup includes a complete monitoring and logging stack with:
- **Prometheus** - Metrics collection and storage
- **Grafana** - Visualization and dashboards
- **cAdvisor** - Container metrics
- **Node Exporter** - Host system metrics
- **Loki** - Log aggregation and storage
- **Promtail** - Log collection agent
- **Log Generator** - Sample application that generates logs

## Services & Ports

| Service | Port | Description |
|---------|------|-------------|
| Grafana | 3000 | Web UI for dashboards and visualization |
| Prometheus | 9090 | Metrics database and query interface |
| cAdvisor | 8080 | Container metrics and monitoring |
| Node Exporter | 9100 | Host system metrics |
| Loki | 3100 | Log aggregation and storage |
| Promtail | 9080 | Log collection agent (internal) |
| Log Generator | - | Sample app generating various log levels |

## Quick Start

1. Navigate to this directory:
   ```bash
   cd labprep/prometheus-grafana
   ```

2. Start the monitoring stack:
   ```bash
   docker-compose up -d
   ```

3. Access the services:
   - **Grafana**: http://localhost:3000 (admin/admin)
   - **Prometheus**: http://localhost:9090
   - **cAdvisor**: http://localhost:8080
   - **Loki**: http://localhost:3100

## Default Credentials

- **Grafana**: 
  - Username: `admin`
  - Password: `admin`

## What's Included

### Prometheus Configuration
- Scrapes metrics from all services
- 15-second scrape interval
- 200 hours data retention

### Grafana Setup
- Pre-configured Prometheus and Loki datasources
- Admin user with default credentials
- Organized provisioning folders for Prometheus and Loki dashboards
- Ready for dashboard import

### Loki Configuration
- Filesystem storage for logs
- Integration with Promtail for log collection
- Ready for log aggregation from containers and system

### Log Generation
- **Log Generator** service creates sample logs with different levels (INFO, DEBUG, WARN, ERROR)
- Simulates real application logging patterns
- Useful for testing Loki and Grafana log dashboards

### Monitoring Targets
- **Prometheus self-monitoring**
- **cAdvisor** - Docker container metrics
- **Node Exporter** - System metrics (CPU, memory, disk, network)
- **Grafana** - Application metrics
- **Container logs** - All Docker container logs via Promtail
- **System logs** - Host system logs

## Useful Commands

```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Remove everything including volumes
docker-compose down -v

# Restart a specific service
docker-compose restart prometheus
```

## Adding Custom Dashboards

1. Access Grafana at http://localhost:3000
2. Login with admin/admin
3. Import dashboards from Grafana.com:

### Prometheus Dashboards:
   - **Node Exporter Full**: Dashboard ID 1860
   - **Docker Container & Host Metrics**: Dashboard ID 179
   - **cAdvisor**: Dashboard ID 893

### Loki Dashboards:
   - **Loki Stack Monitoring**: Dashboard ID 14055
   - **Loki Logs Dashboard**: Dashboard ID 13639
   - **Container Logs**: Dashboard ID 15141

## Viewing Logs

1. Go to Grafana → Explore
2. Select "Loki" as the datasource
3. Use LogQL queries to explore logs:
   - `{container="log-generator"}` - View log generator output
   - `{job="containerlogs"}` - View all container logs
   - `{job="systemlogs"}` - View system logs
   - `{container="log-generator"} |= "ERROR"` - Filter for error logs

## Sample LogQL Queries

```logql
# All logs from log generator
{container="log-generator"}

# Only error logs
{container="log-generator"} |= "ERROR"

# Logs with specific pattern
{container="log-generator"} |~ "User logged in.*"

# Count of log lines by level
sum by (level) (count_over_time({container="log-generator"} | regexp "\\[(?P<level>\\w+)\\]" [5m]))
```

## Troubleshooting

- If cAdvisor doesn't start on macOS, try removing the `/dev/disk/:/dev/disk:ro` volume mount
- Ensure no other services are running on ports 3000, 8080, 9090, or 9100
- Check logs with `docker-compose logs [service-name]` for specific issues