FROM centos:7

RUN yum install -y epel-release && \
  yum install -y \
    java-1.8.0-openjdk-devel \
    weasyprint \
    xorg-x11-fonts-Type1 \
    liberation-sans-fonts \
    liberation-fonts-common \
  && \
  yum clean all

ADD /target/docker/aorra /opt/aorra
RUN useradd -d /opt/aorra -s /sbin/nologin -u 1000 aorra && \
  mkdir -p /opt/aorra/logs /opt/aorra/repository && \
  chown -R aorra:aorra ~aorra

VOLUME /opt/aorra/logs
VOLUME /opt/aorra/repository
WORKDIR /opt/aorra
USER aorra:aorra
ENTRYPOINT ["/opt/aorra/bin/aorra"]
EXPOSE 5000
EXPOSE 9000
