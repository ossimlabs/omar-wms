#ARG BASE_IMAGE
#FROM ${BASE_IMAGE}
FROM alpine
RUN apk add openjdk8-jre

ENV HOME /home/omar
RUN useradd -u 1001 -r -g 0 --create-home -d $HOME -s /sbin/nologin -c 'Default Application User' omar
COPY run.sh $HOME
RUN chown 1001:0 -R $HOME && \
    chmod 777 $HOME && \
    chmod 777 $HOME/*.sh
USER 1001

ENV SPRING_CLOUD_CONFIG_ENABLED=true \
    SPRING_CLOUD_DISCOVERY_ENABLED=true
WORKDIR $HOME
EXPOSE 8080
COPY *.jar $HOME/
CMD $HOME/run.sh
