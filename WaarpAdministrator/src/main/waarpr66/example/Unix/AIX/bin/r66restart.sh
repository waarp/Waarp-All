#!/bin/sh

# Script to allow to restart the R66 Server
# Specially useful when used in a task after upgrade of binary components

# Change path if necesary
. /appli/R66/ENV_R66

# Give a try to one or the other
# same shell
nohup { r66shutd ; sleep 60 ; r66server } &
# new shell
#nohup ( r66shutd ; sleep 60 ; r66server ) &


