node{
    checkout scm 
    docker.image("gradle:4.10.2-jdk8").inside{
        sh "gradle clean test jpi" 
    }
    archiveArtifacts 'build/reports/**'
}