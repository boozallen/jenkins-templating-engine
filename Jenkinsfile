node{
    stage("Unit Test"){
        checkout scm 
        sh 'echo \'org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8\' > gradle.properties'
        sh 'ls -al'
        docker.image("gradle:4.10.2-jdk8").inside{
            sh "gradle clean test" 
        }
        sh 'ls -al'
        archiveArtifacts 'build/reports/**'
    }
}
