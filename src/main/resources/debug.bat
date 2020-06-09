set "JAVA_OPTS=-Dcom.flexicore.init.Initializator.props=/home/flexicore/flexicore.config --add-opens java.base/jdk.internal.loader=ALL-UNNAMED -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8787"
java %JAVA_OPTS% -jar .\FlexiCore-3.1.7-thorntail.jar"
