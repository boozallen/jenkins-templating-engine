parallel "Unit Test": {
    node{
        stage("Unit Test"){
            checkout scm 
            docker.image("gradle:4.10.2-jdk8").inside{
                sh "gradle clean test" 
            }
            archiveArtifacts 'build/reports/**'
        }
    }
}, "Compile Docs": {
    node{
        stage("Compile Docs"){
            checkout scm 
            sh "make docs" 
            sh "ls -R" 
            archiveArtifacts 'docs/_build/html/**'
        }
    }
}, "Build HPI": {
    node{
        stage("Build HPI"){
            checkout scm 
            docker.image("gradle:4.10.2-jdk8").inside{
                sh "gradle clean jpi" 
            }
            archiveArtifacts 'build/libs/*.hpi'
        }
    }
}
