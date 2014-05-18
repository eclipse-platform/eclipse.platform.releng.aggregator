
#!/usr/bin/env bash

# of special interest, though most won't be defined
echo "\$WINDOWMANAGER: $WINDOWMANAGER"
echo "\$WINDOW_MANAGER: $WINDOW_MANAGER"
echo "\$DESKTOP_SESSION: $DESKTOP_SESSION"
echo "\$XDG_CURRENT_DESKTOP: $XDG_CURRENT_DESKTOP"
echo "\$GDMSESSION: $GDMSESSION"

echo "uname -a"
uname -a
echo
echo "lsb_release -a"
lsb_release -a
echo
echo "cat /etc/lsb-release"
cat /etc/lsb-release
echo
echo "cat /etc/SuSE-release"
cat /etc/SuSE-release
echo
echo "rpm -q cairo"
rpm -q cairo
echo
echo "rpm -q gtk2"
rpm -q gtk2
echo
echo "rpm -q glibc"
rpm -q glibc
echo
echo "rpm -q pango"
rpm -q pango
echo
echo "rpm -q glib2"
rpm -q glib2
echo

echo
echo "Check if any window managers are running (xfwm|twm|metacity|beryl|fluxbox|compiz):"
ps -ef | egrep -i "xfwm|twm|metacity|beryl|fluxbox|compiz" | grep -v egrep
echo
echo
# unity|mint|gnome|kde|xfce|ion|wmii|dwm (was original list, but matched too much,
# espeically "ion' I suppose.
echo "Check for popular desktop environments (gnome or kde):"
ps -ef | egrep -i "gnome|kde" | grep -v egrep

echo
echo " == all env variables == "
printenv
echo
echo
# we always end with "success" even though some commands may "fail"
exit 0
