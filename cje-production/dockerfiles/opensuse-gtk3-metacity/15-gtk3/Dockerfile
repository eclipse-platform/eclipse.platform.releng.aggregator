FROM opensuse/leap:15@sha256:039283c971f917bfd2ac9d51f259ce5eff1b7e92716aeaf07446be9c97f77d2e

### user name recognition at runtime w/ an arbitrary uid - for OpenShift deployments
COPY scripts/uid_entrypoint /usr/local/bin/uid_entrypoint
RUN chmod u+x /usr/local/bin/uid_entrypoint && \
    chgrp 0 /usr/local/bin/uid_entrypoint && \
    chmod g=u /usr/local/bin/uid_entrypoint /etc/passwd
ENTRYPOINT [ "uid_entrypoint" ]

RUN zypper --non-interactive update && zypper --non-interactive install \
      libgtk-3-0 \
      xorg-x11-Xvnc \
      tigervnc \
      procps \
      metacity \
      xorg-x11-fonts \
      dejavu-fonts \
      liberation-fonts \
      Mesa-libGL1 \
      Mesa-libEGL1 \
      Mesa-dri \
      wget \
      curl \
      unzip \
      git-core \
      vim \
      tar \
      gzip

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
