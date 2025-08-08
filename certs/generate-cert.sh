#!/bin/bash

# Create certs directory if it doesn't exist
mkdir -p certs

# Temporary OpenSSL config file with SAN
cat > certs/openssl.cnf <<EOF
[req]
default_bits       = 4096
distinguished_name = req_distinguished_name
req_extensions     = req_ext
x509_extensions    = v3_ca
prompt             = no

[req_distinguished_name]
CN = localhost

[req_ext]
subjectAltName = @alt_names

[v3_ca]
subjectAltName = @alt_names

[alt_names]
DNS.1 = localhost
EOF

# Generate the certificate and key
openssl req -x509 -nodes -days 365 \
  -newkey rsa:4096 \
  -keyout certs/key.pem \
  -out certs/cert.pem \
  -config certs/openssl.cnf \
  -extensions req_ext

# Clean up config file
rm certs/openssl.cnf

echo "✅ Certificato autofirmato generato in ./certs/cert.pem e ./certs/key.pem"
