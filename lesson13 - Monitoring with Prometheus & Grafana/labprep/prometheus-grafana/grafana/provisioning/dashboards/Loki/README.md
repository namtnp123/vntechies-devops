# Loki Provisioning

This folder contains Grafana provisioning configuration for Loki:

- `loki-datasource.yml` - Configures Loki as a datasource for log aggregation
- `dashboards.yml` - Configures dashboard provider for Loki dashboards
- `dashboards/` - Directory for Loki-related dashboard JSON files

## Adding Loki Dashboards

Place your Loki dashboard JSON files in the `dashboards/` directory. They will be automatically loaded by Grafana.

Popular Loki dashboards:
- Loki Stack Monitoring (ID: 14055)
- Loki Logs Dashboard (ID: 13639)
- Container Logs (ID: 15141)

## Note

To use Loki, you'll need to add Loki service to your docker-compose.yml file.