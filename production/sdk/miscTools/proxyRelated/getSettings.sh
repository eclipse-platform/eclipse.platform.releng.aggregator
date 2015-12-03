#!/usr/bin/env bash

function getconf ()
{
  key=$1
  if [[ -n "${key}" ]]
  then
    value=$(gconftool-2 --get "${key}")
    echo -e "${key} \t\t${value}"
  fi
}

getconf /system/http_proxy/use_http_proxy
getconf /system/http_proxy/use_authentication
getconf /system/http_proxy/host
getconf /system/http_proxy/authentication_user
getconf /system/http_proxy/authentication_password
getconf /system/http_proxy/port
getconf /system/proxy/socks_host
getconf /system/proxy/mode
getconf /system/proxy/ftp_host
getconf /system/proxy/secure_host
getconf /system/proxy/socks_port
getconf /system/proxy/ftp_port
getconf /system/proxy/secure_port
getconf /system/proxy/no_proxy_for
getconf /system/proxy/gopher_host
getconf /system/proxy/gopher_port

