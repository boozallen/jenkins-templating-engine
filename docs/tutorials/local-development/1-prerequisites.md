# Prerequisites

## Docker

Ensure Docker is installed and running on your local machine.

You can validate the Docker process is running by running `docker ps` in your terminal.

You should see output similar to:

![docker ps command output](./images/empty_docker_ps.png)

## Internet Connectivity

The Jenkins LTS container image from Docker Hub will be used in this course. You can validate your ability to download this container image by running `docker pull jenkins/jenkins:lts`.

You should see output similar to:

![docker jenkins image output](./images/docker_pull_jenkins.png)
