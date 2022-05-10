ARG FROM_TAG="7"
FROM centos:${FROM_TAG}

### user name recognition at runtime w/ an arbitrary uid - for OpenShift deployments
COPY scripts/uid_entrypoint /usr/local/bin/uid_entrypoint
RUN chmod u+x /usr/local/bin/uid_entrypoint && \
    chgrp 0 /usr/local/bin/uid_entrypoint && \
    chmod g=u /usr/local/bin/uid_entrypoint /etc/passwd
### end

ENTRYPOINT [ "uid_entrypoint" ]

RUN yum -y update && yum -y install \
      gtk3 \
      tigervnc-server \
      tigervnc \
      metacity \
      xorg-x11-server-utils \
      mesa-libGL \
      xorg-x11-fonts-misc \
      xorg-x11-fonts-75dpi \
      xorg-x11-fonts-Type1 \
      xorg-x11-fonts-100dpi \
      liberation-fonts \
      gnu-free-fonts-common \
      dejavu-fonts-common \
      unzip \
      lsof \
      wget \
      curl \
      webkitgtk4 \
      dbus \
      glibc-locale-source \
    && yum clean all

ENV HOME=/home/vnc
ENV DISPLAY :0
RUN dbus-uuidgen --ensure

RUN mkdir -p ${HOME}/.vnc && chmod -R 775 ${HOME} \
  && echo "123456" | vncpasswd -f > ${HOME}/.vnc/passwd \
  && chmod 600 ${HOME}/.vnc/passwd

# Create a custom vnc xstartup file
COPY scripts/xstartup_metacity.sh ${HOME}/.vnc/xstartup.sh
# Make $HOME open to the 'root' group (so Jenkins/OpenShift user can write there)
RUN chgrp -R 0 ${HOME} && chmod -R g=u ${HOME

RUN localedef -i en_US -f UTF-8 en_US.UTF-8
ENV LANG=en_US.UTF-8

USER 10001
