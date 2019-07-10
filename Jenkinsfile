node{
    stage("Unit Test"){
        checkout scm 
        docker.image("gradle:4.10.2-jdk8").inside{
            sh "gradle clean test" 
        }
        archiveArtifacts 'build/reports/**'
    }
}
