FROM python:3.11-slim

WORKDIR /app

# Install system dependencies for protoc and Python build tools
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Copy source files
COPY requirements.txt .
COPY request_log.proto .

# Install Python dependencies (includes grpcio-tools)
RUN pip install --no-cache-dir -r requirements.txt

# Generate gRPC Python files from .proto
RUN python -m grpc_tools.protoc -I. --python_out=. --grpc_python_out=. request_log.proto
