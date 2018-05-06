
## Version 
```
mvn versions:set -DnewVersion=0.0.4-SNAPSHOT
```

## Run in the background

* unix & linux    

    ```Shell
    # config ss-client.sh:
    JAVA_HOME=../lib/jre8
    # Run:
    nohup sh ss-client.sh >/dev/null 2>&1 &
    nohup sh ss-server.sh >/dev/null 2>&1 &
    ```
* windows

    add blew codes at the head of the bat file:

    ```Bat
    @echo off 
    if "%1" == "h" goto begin 
    mshta vbscript:createobject("wscript.shell").run("%~nx0 h",0)(window.close)&&exit 
    :begin 
    
    :: 修改：JAVACMD
    if "%JAVACMD%"=="" set JAVACMD=..\lib\jre8\bin\java
    ```

## DEBUG

```bash
com.shadowsocks.common.utils.ChannelUtils.
```





