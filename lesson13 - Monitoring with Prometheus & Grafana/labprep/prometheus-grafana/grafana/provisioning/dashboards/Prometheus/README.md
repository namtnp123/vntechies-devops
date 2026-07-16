# Prometheus Provisioning

This folder contains Grafana provisioning configuration for Prometheus:

- `prometheus-datasource.yml` - Configures Prometheus as a datasource
- `dashboards.yml` - Configures dashboard provider for Prometheus dashboards
- `dashboards/` - Directory for Prometheus-related dashboard JSON files

## Adding Prometheus Dashboards

Place your Prometheus dashboard JSON files in the `dashboards/` directory. They will be automatically loaded by Grafana.

Popular Prometheus dashboards:
- Node Exporter Full (ID: 1860)
- Docker Container & Host Metrics (ID: 179)
- Kubernetes Cluster Monitoring (ID: 7249)