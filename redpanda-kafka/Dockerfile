FROM docker.redpanda.com/redpandadata/redpanda:v24.1.3

ENTRYPOINT [ "redpanda", "start", "--overprovisioned", "--smp", "1", "--memory", "1G", "--reserve-memory", "0M", "--node-id", "0", "--check=false", "--kafka-addr", "PLAINTEXT://0.0.0.0:9092", "--advertise-kafka-addr", "PLAINTEXT://redpanda:9092" ]
