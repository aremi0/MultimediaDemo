#!/bin/sh

# Copia tutti i file statici in RAM
cp -r /usr/share/nginx/html-ro/* /usr/share/nginx/html/

# Sostituisce la variabile DOMAIN_NAME nel template
envsubst '${DOMAIN_NAME}' < /usr/share/nginx/html/script.template.js > /usr/share/nginx/html/script.js

# Avvia Nginx
nginx -g 'daemon off;'
