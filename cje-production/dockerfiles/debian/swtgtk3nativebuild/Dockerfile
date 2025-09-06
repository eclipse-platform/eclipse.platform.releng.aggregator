FROM debian:11

### user name recognition at runtime w/ an arbitrary uid - for OpenShift deployments
COPY scripts/uid_entrypoint /usr/local/bin/uid_entrypoint
RUN chmod u+x /usr/local/bin/uid_entrypoint && \
    chgrp 0 /usr/local/bin/uid_entrypoint && \
    chmod g=u /usr/local/bin/uid_entrypoint /etc/passwd
### end

ENV LANG=en_US.UTF-8
RUN apt-get update -qq && apt-get install -qq -y \
      locales \
      build-essential \
      procps \
      git \
      libgtk-3-dev \
      freeglut3-dev \
      webkit2gtk-driver \
      gcc-aarch64-linux-gnu

ENV HOME=/home/swtbuild
ENV DISPLAY :0
RUN useradd -u 10001 -d ${HOME} testuser

RUN mkdir -p /var/lib/dbus && dbus-uuidgen > /var/lib/dbus/machine-id \
  && mkdir -p /var/run/dbus

# Make $HOME open to the 'root' group (so Jenkins/OpenShift user can write there)
WORKDIR $HOME
RUN chgrp -R 0 ${HOME} && chmod -R g=u ${HOME}

RUN localedef -i en_US -f UTF-8 en_US.UTF-8
ENV LANG=en_US.UTF-8

USER 10001
