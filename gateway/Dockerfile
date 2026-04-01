FROM nginx:alpine

# Copy the nginx routing config
COPY nginx.conf /etc/nginx/nginx.conf

# Copy the compiled client GUI static files.
# The client/ directory is built separately (see client/README.md).
# If you're running without a build step, the html directory can be empty
# and you can still access the API directly through the gateway.
COPY client/ /usr/share/nginx/html/

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
