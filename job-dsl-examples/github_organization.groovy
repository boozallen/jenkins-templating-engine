githubOrg = System.getenv("TENANT_GITHUB_ORG")
credId = System.getenv("TENANT_GITHUB_CREDS_ID")
apiUri = System.getenv("TENANT_GITHUB_API_URL")
orgPipelineRepo = System.getenv("TENANT_GITHUB_CONFIG_URL")
libraryRepo = System.getenv("TENANT_GITHUB_LIBRARY_URL")
baseDirectory = System.getenv("BASE_DIRECTORY")

organizationFolder githubOrg, {
  configure{
    it / 'properties' << 'org.boozallen.plugins.jte.config.TemplateConfigFolderProperty' {
        tier {
            baseDir(baseDirectory)
            scm (class: 'hudson.plugins.git.GitSCM'){
                configVersion 2
                userRemoteConfigs{
                  'hudson.plugins.git.UserRemoteConfig' {
                    url orgPipelineRepo
                    credentialsId credId
                  }
                }
                branches{
                  'hudson.plugins.git.BranchSpec'{
                    name "*/master"
                  }
                }
      		}
            librarySources{
                'org.boozallen.plugins.jte.config.TemplateLibrarySource'{
                    scm (class: 'hudson.plugins.git.GitSCM'){
                        configVersion 2
                        userRemoteConfigs{
                          'hudson.plugins.git.UserRemoteConfig' {
                            url libraryRepo
                            credentialsId credId
                          }
                        }
                        branches{
                          'hudson.plugins.git.BranchSpec'{
                            name "*/master"
                          }
                        }
      				      }
                }
            }
        }
    }
    it / 'navigators' << 'org.jenkinsci.plugins.github__branch__source.GitHubSCMNavigator'{
      repoOwner githubOrg
      scanCredentialsId credId
      checkoutCredentialsId 'SAME'
      apiUri apiUri
      buildOriginBranch true
      buildOriginBranchWithPR false
      buildOriginPRMerge true
      buildOriginPRHead false
      buildForkPRMerge false
      buildForkPRHead false
    }
    it / 'projectFactories' << 'org.boozallen.plugins.jte.job.TemplateMultiBranchProjectFactory' {
    }
  }
}
