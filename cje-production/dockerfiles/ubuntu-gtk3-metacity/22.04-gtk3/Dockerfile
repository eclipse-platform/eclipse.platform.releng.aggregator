FROM ubuntu:22.04@sha256:dfd64a3b4296d8c9b62aa3309984f8620b98d87e47492599ee20739e8eb54fbf

### user name recognition at runtime w/ an arbitrary uid - for OpenShift deployments
COPY scripts/uid_entrypoint /usr/local/bin/uid_entrypoint
RUN chmod u+x /usr/local/bin/uid_entrypoint && \
    chgrp 0 /usr/local/bin/uid_entrypoint && \
    chmod g=u /usr/local/bin/uid_entrypoint /etc/passwd
ENTRYPOINT [ "uid_entrypoint" ]

ENV DEBIAN_FRONTEND noninteractive
RUN apt-get install -y --no-install-recommends gpgv
RUN apt-get update && apt upgrade -y && apt dist-upgrade -y && apt-get install -y --no-install-recommends \
      libgtk-3-0 \
      tigervnc-standalone-server \
      tigervnc-common \
      tigervnc-tools \
      metacity \
      x11-xserver-utils \
      libgl1-mesa-dri \
      xfonts-base \
      xfonts-scalable \
      xfonts-100dpi \
      xfonts-75dpi \
      fonts-liberation \
      fonts-liberation2 \
      fonts-freefont-ttf \
      fonts-dejavu \
      fonts-dejavu-core \
      fonts-dejavu-extra \
      wget \
      curl \
      unzip \
      vim \
    && rm -rf /var/lib/apt/lists/* && apt autoremove -y

ENV HOME=/home/vnc
ENV DISPLAY :0

RUN mkdir -p ${HOME}/.vnc && chmod -R 775 ${HOME} \
  && echo "123456" | vncpasswd -f > ${HOME}/.vnc/passwd \
  && chmod 600 ${HOME}/.vnc/passwd

# Create a custom vnc xstartup file
COPY scripts/xstartup_metacity.sh ${HOME}/.vnc/xstartup.sh
# Make $HOME open to the 'root' group (so Jenkins/OpenShift user can write there)
RUN chgrp -R 0 ${HOME} && chmod -R g=u ${HOME}

USER 10001
