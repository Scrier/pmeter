#!/bin/bash
### BEGIN INIT INFO
# Provides:           duke
# Required-Start:     $network
# Required-Stop:      $network
# Default-Start:      2 3 4 5
# Default-Stop:       0 1 6
# Short-Description:  Start duke
### END INIT INFO
#
# description: Java deamon script
#
# Run Duke, a opus client.
#


# Set this to your Java installation
JAVA_HOME=/usr/java/latest

serviceNameLo="duke"                                  # service name with the first letter in lowercase
serviceName="Duke"                                    # service name
serviceUser="opus"                                # OS user name for the service
serviceGroup="verif.testtool.access"                        # OS group name for the service
applDir="/usr/share/java/opus"                  # home directory of the service application
serviceUserHome="/home/opus"                      # home directory of the service user
serviceLogFile="/var/log/opus/$serviceNameLo.log"               # log file for StdOut/StdErr
maxShutdownTime=15                                         # maximum number of seconds to wait for the daemon to terminate normally
log4j2file=${log4j2file:-$applDir/log4j2duke.xml} # where log4j2 xml configuration file resides.
pidFile="$applDir/$serviceNameLo.pid"                      # name of PID file (PID = process ID number)
dukeConfig=${dukeConfig:-$serviceUserHome/dukeConfig.xml}                          # confi input to running testfile
hazelcastConfig=${hazelcastConfig:-$serviceUserHome/hazelcastDukeConfig.xml} # if not set, set iut to home dir.
[[ ! -f $hazelcastConfig ]] && hazelcastConfig=/etc/opus/hazelcastDukeConfig.xml # if not exist, do default
javaCommand="java"                                         # name of the Java launcher without the path
javaExe="$JAVA_HOME/bin/$javaCommand"                      # file name of the Java application launcher executable
javaAppArgs="-Djava.net.preferIPv4Stack=true -Dlog4j.configurationFile=$log4j2file -Dhazelcast.client.config=$hazelcastConfig"
if [[ -f $dukeConfig ]]; then
  echo "Running with file $dukeConfig."
  javaArgs="-jar $applDir/duke.jar $dukeConfig"                 # arguments for Java launcher
else
  echo "No valid config file to duke specified in param dukeConfig, running default."
  javaArgs="-jar $applDir/duke.jar"                 # arguments for Java launcher
fi
javaCommandLine="$javaExe $javaAppArgs $javaArgs"          # command line to start the Java service application
javaCommandLineKeyword="duke.jar"     # a keyword that occurs on the commandline, used to detect an already running service process and to distinguish it from others

# Makes the file $1 writable by the group $serviceGroup.
function makeFileWritable {
   local filename="$1"
   touch $filename || return 1
   chgrp $serviceGroup $filename || return 1
   chmod g+w $filename || return 1
   return 0; }

# Returns 0 if the process with PID $1 is running.
function checkProcessIsRunning {
   local pid="$1"
   if [ -z "$pid" -o "$pid" == " " ]; then return 1; fi
   if [ ! -e /proc/$pid ]; then return 1; fi
   return 0; }

# Returns 0 if the process with PID $1 is our Java service process.
function checkProcessIsOurService {
   local pid="$1"
   if [ "$(ps -p $pid --no-headers -o comm)" != "$javaCommand" ]; then return 1; fi
   grep -q --binary -F "$javaCommandLineKeyword" /proc/$pid/cmdline
   if [ $? -ne 0 ]; then return 1; fi
   return 0; }

# Returns 0 when the service is running and sets the variable $pid to the PID.
function getServicePID {
   if [ ! -f $pidFile ]; then return 1; fi
   pid="$(<$pidFile)"
   checkProcessIsRunning $pid || return 1
   checkProcessIsOurService $pid || return 1
   return 0; }

function startServiceProcess {
   cd $applDir || return 1
   rm -f $pidFile
   makeFileWritable $pidFile || return 1
   makeFileWritable $serviceLogFile || return 1
   cmd="cd $applDir ; nohup $javaCommandLine >>$serviceLogFile 2>&1 & echo \$! >$pidFile"
   su -l $serviceUser -s $SHELL -c "$cmd" || return 1
   sleep 0.1
   pid="$(<$pidFile)"
   if checkProcessIsRunning $pid; then :; else
      echo -ne "\n$serviceName start failed, see logfile."
      return 1
   fi
   return 0; }

function stopServiceProcess {
   kill $pid || return 1
   for ((i=0; i<maxShutdownTime*10; i++)); do
      checkProcessIsRunning $pid
      if [ $? -ne 0 ]; then
         rm -f $pidFile
         return 0
         fi
      sleep 0.1
      done
   echo -e "\n$serviceName did not terminate within $maxShutdownTime seconds, sending SIGKILL..."
   kill -s KILL $pid || return 1
   local killWaitTime=15
   for ((i=0; i<killWaitTime*10; i++)); do
      checkProcessIsRunning $pid
      if [ $? -ne 0 ]; then
         rm -f $pidFile
         return 0
         fi
      sleep 0.1
      done
   echo "Error: $serviceName could not be stopped within $maxShutdownTime+$killWaitTime seconds!"
   return 1; }

function startService {
   getServicePID
   if [ $? -eq 0 ]; then echo -n "$serviceName is already running"; RETVAL=0; return 0; fi
   echo -n "Starting $serviceName   "
   startServiceProcess
   if [ $? -ne 0 ]; then RETVAL=1; echo "failed"; return 1; fi
   echo "started PID=$pid"
   RETVAL=0
   return 0; }

function stopService {
   getServicePID
   if [ $? -ne 0 ]; then echo -n "$serviceName is not running"; RETVAL=0; echo ""; return 0; fi
   echo -n "Stopping $serviceName   "
   stopServiceProcess
   if [ $? -ne 0 ]; then RETVAL=1; echo "failed"; return 1; fi
   echo "stopped PID=$pid"
   RETVAL=0
   return 0; }

function checkServiceStatus {
   echo -n "Checking for $serviceName:   "
   if getServicePID; then
   echo "running PID=$pid"
   RETVAL=0
   else
   echo "stopped"
   RETVAL=3
   fi
   return 0; }

function main {
   RETVAL=0
   case "$1" in
      start)                                               # starts the Java program as a Linux service
         startService
         ;;
      stop)                                                # stops the Java program service
         stopService
         ;;
      restart)                                             # stops and restarts the service
         stopService && startService
         ;;
      status)                                              # displays the service status
         checkServiceStatus
         ;;
      *)
         echo "Usage: $0 {start|stop|restart|status}"
         exit 1
         ;;
      esac
   exit $RETVAL
}

main $1
