# PromQL Cheat Sheet for Students

## Basic Selectors

### Simple Metric Selection
```promql
# Select all instances of a metric
up

# Select metric with specific label
up{job="prometheus"}

# Select multiple jobs
up{job=~"prometheus|node-exporter"}
```

### Label Matching
```promql
# Exact match
{job="prometheus"}

# Not equal
{job!="prometheus"}

# Regex match
{job=~"prom.*"}

# Negative regex match  
{job!~"prom.*"}

# Multiple label conditions
{job="prometheus", instance="localhost:9090"}
```

## Range Vectors vs Instant Vectors

### Instant Vector (single value at time T)
```promql
up
prometheus_http_requests_total
```

### Range Vector (values over time range)
```promql
up[5m]                              # Last 5 minutes
prometheus_http_requests_total[1h]  # Last 1 hour
prometheus_http_requests_total[1d]  # Last 1 day
```

## Rate Functions

### rate() - Average per-second rate over time range
```promql
# HTTP request rate per second
rate(prometheus_http_requests_total[5m])

# Best for: Calculating average rates over time
# Use case: Request rates, error rates, throughput
```

### irate() - Instantaneous rate from last 2 data points
```promql
# Instantaneous HTTP request rate
irate(prometheus_http_requests_total[5m])

# Best for: Real-time spiky data, volatile metrics
# Use case: CPU usage spikes, memory allocation bursts
```

### increase() - Total increase over time range
```promql
# Total HTTP requests in last 5 minutes
increase(prometheus_http_requests_total[5m])

# Best for: Calculating absolute increases
# Use case: Total requests, total errors, cumulative counters
```

## Aggregation Functions

### sum() - Add up values
```promql
# Total requests across all instances
sum(rate(prometheus_http_requests_total[5m]))

# Total requests by status code
sum(rate(prometheus_http_requests_total[5m])) by (code)
```

### avg() - Average values
```promql
# Average memory usage
avg(go_memstats_alloc_bytes)

# Average by instance
avg(go_memstats_alloc_bytes) by (instance)
```

### max() / min() - Maximum/Minimum values
```promql
# Maximum memory usage across all instances
max(go_memstats_alloc_bytes)

# Minimum memory usage
min(go_memstats_alloc_bytes)

# Max by job
max(go_memstats_alloc_bytes) by (job)
```

### count() - Count number of time series
```promql
# Count total services
count(up)

# Count services that are up
count(up == 1)

# Count by job
count(up) by (job)
```

## Advanced Aggregations

### topk() / bottomk() - Top/Bottom K values
```promql
# Top 5 highest memory consumers
topk(5, go_memstats_alloc_bytes)

# Bottom 3 lowest memory consumers  
bottomk(3, go_memstats_alloc_bytes)
```

### quantile() - Percentile calculations
```promql
# 95th percentile of memory usage
quantile(0.95, go_memstats_alloc_bytes)

# 50th percentile (median) by job
quantile(0.5, go_memstats_alloc_bytes) by (job)
```

### histogram_quantile() - Histogram percentiles
```promql
# 95th percentile request duration
histogram_quantile(0.95, rate(prometheus_http_request_duration_seconds_bucket[5m]))

# 99th percentile response time by handler
histogram_quantile(0.99, sum(rate(prometheus_http_request_duration_seconds_bucket[5m])) by (le, handler))
```

## Grouping Operations

### by() - Keep only specified labels
```promql
# Group by job, remove all other labels
sum(rate(prometheus_http_requests_total[5m])) by (job)

# Group by job and status code
sum(rate(prometheus_http_requests_total[5m])) by (job, code)
```

### without() - Remove specified labels
```promql
# Remove instance label, keep all others
sum(rate(prometheus_http_requests_total[5m])) without (instance)

# Remove multiple labels
sum(rate(prometheus_http_requests_total[5m])) without (instance, handler)
```

## Arithmetic Operations

### Basic Operations
```promql
# Addition
go_memstats_heap_alloc_bytes + go_memstats_stack_inuse_bytes

# Subtraction  
go_memstats_heap_sys_bytes - go_memstats_heap_alloc_bytes

# Multiplication
go_memstats_heap_alloc_bytes * 1024

# Division (percentage calculation)
(go_memstats_heap_alloc_bytes / go_memstats_heap_sys_bytes) * 100
```

### Comparison Operations
```promql
# Greater than
go_memstats_heap_alloc_bytes > 1000000

# Less than or equal
prometheus_http_requests_total <= 100

# Equal to
up == 1

# Not equal to
up != 1
```

## Time Functions

### Time Shifting
```promql
# Compare current with 1 hour ago
prometheus_http_requests_total - prometheus_http_requests_total offset 1h

# Rate change over time
rate(prometheus_http_requests_total[5m]) - rate(prometheus_http_requests_total[5m] offset 1h)
```

### Time-based Calculations
```promql
# Current time
time()

# Days since epoch
days_in_month()

# Hour of day
hour()
```

## Common Patterns

### Error Rate Calculation
```promql
# Error rate percentage
sum(rate(prometheus_http_requests_total{code=~"5.."}[5m])) /
sum(rate(prometheus_http_requests_total[5m])) * 100
```

### Availability Calculation
```promql
# Service availability percentage
avg(up{job="my-service"}) * 100
```

### Resource Utilization
```promql
# Memory utilization percentage
(go_memstats_heap_alloc_bytes / go_memstats_heap_sys_bytes) * 100

# Disk usage percentage
(node_filesystem_size_bytes - node_filesystem_free_bytes) / node_filesystem_size_bytes * 100
```

### Rate vs Increase vs irate

| Function | Best For | Use Case | Example |
|----------|----------|----------|---------|
| `rate()` | Smooth, average rates | Dashboards, alerts | `rate(requests[5m])` |
| `irate()` | Volatile, real-time rates | Debugging spikes | `irate(cpu_usage[5m])` |
| `increase()` | Total counts over time | Cumulative metrics | `increase(errors[1h])` |

## Time Range Suffixes
- `s` - seconds
- `m` - minutes  
- `h` - hours
- `d` - days
- `w` - weeks
- `y` - years

## Pro Tips for Students

1. **Always use rate() with counters** - Never use raw counter values for rates
2. **Choose appropriate time ranges** - Longer ranges = smoother graphs
3. **Use by() and without() wisely** - Control label cardinality
4. **Test queries step by step** - Build complex queries incrementally
5. **Use regex carefully** - `=~` and `!~` can be expensive
6. **Combine functions logically** - `sum(rate(...))` not `rate(sum(...))`