#!/bin/bash
JAVA_HOME=${env.JAVA_HOME}
APP_MAIN_CLASS=${app.mainClass}
RUNNING_USER=ubuntu
pid=0
checkPid() {
   javaJps=`${JAVA_HOME}/bin/jps -l | grep ${APP_MAIN_CLASS}`

   if [ -n "${javaJps}" ]; then
      pid=`echo ${javaJps} | awk '{print $1}'`
      echo ${pid}
   else
      pid=0
      echo ${pid}
   fi
}
checkPid
