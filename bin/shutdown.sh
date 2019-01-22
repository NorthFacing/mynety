#!/bin/sh
JAVA_HOME=${env.JAVA_HOME}
APP_MAIN_CLASS=${app.mainClass}
RUNNING_USER=ubuntu
pid=0
checkPid() {
   javaJps=`${JAVA_HOME}/bin/jps -l | grep ${APP_MAIN_CLASS}`

   if [[ -n "${javaJps}" ]]; then
      pid=`echo ${javaJps} | awk '{print $1}'`
   else
      pid=0
   fi
}
checkPid
   if [[ ${pid} -ne 0 ]]; then
      echo -n "Stopping $APP_MAIN_CLASS ...(pid=$pid) "
      kill -9 ${pid}
      if [[ $? -eq 0 ]]; then
         echo "[OK]"
      else
         echo "[Failed]"
      fi

      checkPid
      if [[ ${pid} -ne 0 ]]; then
         echo "================================"
         echo "[warn:]Stop is failed!!!"
         echo "================================"
      fi
   else
      echo "================================"
      echo "warn: $APP_MAIN_CLASS is not running"
      echo "================================"
   fi
