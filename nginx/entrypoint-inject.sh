#!/bin/sh

# Sostituisce la variabile DOMAIN_NAME nel template
envsubst '${DOMAIN_NAME}' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf

exec nginx -g 'daemon off;'