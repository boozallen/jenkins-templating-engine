# Jenkins Templating Engine

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/templating-engine.svg)](https://plugins.jenkins.io/templating-engine)
[![GitHub Release](https://img.shields.io/github/v/release/jenkinsci/templating-engine-plugin.svg?label=release)](https://github.com/jenkinsci/templating-engine-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/templating-engine.svg?color=blue)](https://plugins.jenkins.io/templating-engine)
[![Gitter](https://badges.gitter.im/jenkinsci/templating-engine-plugin.svg)](https://gitter.im/jenkinsci/templating-engine-plugin)

<div align="center">
   <img src="docs/modules/ROOT/images/jte.png" width="192">
</div>

**Table of Contents:**
- [Jenkins Templating Engine](#jenkins-templating-engine)
  - [Overview](#overview)
  - [How Does It Work?](#how-does-it-work)
  - [Learn More](#learn-more)
  - [Adopters](#adopters)

## Overview

The Jenkins Templating Engine (JTE) is a plugin originally created by [Booz Allen Hamilton](https://www.boozallen.com/) enabling pipeline templating and governance. JTE  allows you to consolidate pipelines into shareable workflows that define the business logic of your software delivery processes while allowing for optimal pipeline code reuse by pulling out tool specific implementations into library modules.

## How Does It Work?

<div align="center">
   <img src="docs/modules/ROOT/images/jte.gif">
</div>

JTE allows you to separate the business logic of your pipeline (what should happen, and when) from the technical implementation by creating pipeline templates and separating the implementation of the pipeline actions defined in the template out into pipeline libraries.

The idea is that regardless of which specific tools are being used there are common steps that often take place, such as unit testing, static code analysis, packaging an artifact, and deploying that artifact to an application environment.

## Learn More

- [Documentation](https://boozallen.github.io/sdp-docs/jte/1.5.2/index.html)
- **Learning Labs**
  - [Local Development (getting up and running with JTE)](https://boozallen.github.io/sdp-docs/learning-labs/1/local-development/index.html)
  - [JTE: The Basics](https://boozallen.github.io/sdp-docs/learning-labs/1/jte-the-basics/index.html)
  - [JTE: Learn the Primitives](https://boozallen.github.io/sdp-docs/learning-labs/1/jte-primitives/index.html)
  - [JTE: Advanced Features](https://boozallen.github.io/sdp-docs/learning-labs/1/jte-advanced-features/index.html)
- **Presentations**
  - [Jenkins Online Meetup](https://www.youtube.com/watch?v=pz_kPpb9C1w&feature=youtu.be)
  - [JTE at KubeCon 2019](https://www.youtube.com/watch?v=OClSwxhsspA)
  - [Trusted Software Supply Chains with JTE](https://www.youtube.com/watch?v=TMxUAi3XXOg&list=PLj6h78yzYM2MGKo_LNRA-lhxlNXwiDJDT&index=5&t=0s)
  - [Jenkins World 2018](https://www.youtube.com/watch?v=BM9Vmsh2iMI)
- **Blog Posts**
  - [Introducing the Jenkins Templating Engine](https://jenkins.io/blog/2019/05/09/templating-engine/)


## Adopters

If you're using the Jenkins Templating Engine we would love to hear about it!

Please let us know by opening a Pull Request to the `ADOPTERS <https://github.com/jenkinsci/templating-engine-plugin/blob/master/docs/ADOPTERS.rst>`_ file and you'll be featured on the JTE Documentation Site!