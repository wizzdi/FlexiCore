set "JAVA_OPTS= -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8787 -Dloader.main=com.flexicore.init.FlexiCoreApplication -Dloader.path=file:/home/flexicore/entities/"
java %JAVA_OPTS% -jar .\FlexiCore-4.0.19-exec.jar"
