pipeline {
    agent {
        dockerfile {
            args '-u root:root -v /root/.m2:/root/.m2'
        }
    }
    stages {
        stage('Build') {
            steps {
                configFileProvider([configFile(fileId: 'settings-maven', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh 'mvn -B clean prepare-package war:exploded -DskipTests -s $MAVEN_SETTINGS_XML'
                }
            }
        }

        stage('Test') {
            steps {
                configFileProvider([configFile(fileId: 'settings-maven', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh 'chmod 777 /runAll.sh && /runAll.sh'
                    sleep 5
                    sh 'mvn -B test -s $MAVEN_SETTINGS_XML -Djava.util.logging.manager=org.jboss.logmanager.LogManager'
                }
            }
        }
    }
}