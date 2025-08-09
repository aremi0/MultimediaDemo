FROM fluent/fluentd:v1.16-1

USER root

# Installazione del plugin gRPC
RUN gem install fluent-plugin-grpc

USER fluent
