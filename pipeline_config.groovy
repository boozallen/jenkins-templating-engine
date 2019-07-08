pipeline_template = "jenkins-templating-engine"

libraries {
  gradle {
    image {
      name = "gradle:4.10.2-jdk8"
    }
  }
}
