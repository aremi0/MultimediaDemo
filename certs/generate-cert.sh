#!/bin/bash

mkdir -p certs

cat > certs/openssl.cnf <<EOF
[req]
default_bits       = 4096
distinguished_name = req_distinguished_name
req_extensions     = req_ext
x509_extensions    = v3_ca
prompt             = no

[req_distinguished_name]
CN = multimedia-entrypoint

[req_ext]
subjectAltName = @alt_names

[v3_ca]
subjectAltName = @alt_names

[alt_names]
DNS.1 = multimedia-entrypoint
DNS.2 = localhost
EOF

# Genera certificato e chiave
openssl req -x509 -nodes -days 365 \
  -newkey rsa:4096 \
  -keyout certs/key.pem \
  -out certs/cert.pem \
  -config certs/openssl.cnf \
  -extensions req_ext

# Converte il certificato in formato .crt (compatibile con keytool)
openssl x509 -in certs/cert.pem -out certs/multimedia-entrypoint.crt

# Pulisce il file di configurazione temporaneo
rm certs/openssl.cnf

echo "✅ Certificato autofirmato generato:"
echo "   - PEM: certs/cert.pem"
echo "   - CRT: certs/multimedia-entrypoint.crt"
echo "   - Chiave: certs/key.pem"
