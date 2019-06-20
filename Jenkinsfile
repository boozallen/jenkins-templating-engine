parallel [
"Unit Test": {
    node{
        stage("Unit Test"){
            checkout scm 
            docker.image("gradle:4.10.2-jdk8").inside{
                sh "gradle clean test jpi" 
            }
            archiveArtifacts 'build/reports/**'
        }
    }
},    
"Compile Docs": {
    node{
        stage("Compile Docs"){
            checkout scm 
            sh "make docs" 
            archiveArtifacts 'docs/_build/html'
        }
    }
}]
