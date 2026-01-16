FROM nginx:alpine

# Copy checkout page files
COPY . /usr/share/nginx/html/

# Copy nginx config
RUN echo 'server { \
    listen 80; \
    server_name _; \
    root /usr/share/nginx/html; \
    index index.html; \
    \
    location / { \
        try_files $uri /index.html; \
    } \
    \
    location ~* \.js$ { \
        expires 1h; \
        add_header Cache-Control "public, immutable"; \
    } \
    \
    location ~* \.css$ { \
        expires 1h; \
        add_header Cache-Control "public, immutable"; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
