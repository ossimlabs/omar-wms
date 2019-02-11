//==================================================================================================
// This is the Jenkins pipeline script for building the OMAR WMS application.
// Environment varialbes that MUST be passed in by Jenkins:
//    OSSIM_GIT_BRANCH: The tag of the branch to be built. Typically dev or master.
//
// Environment variables that MUST be set in the Jenkins global environment (manage jenkins -> configure system -> environment varaibles)
//    REPOSITORY_MANAGER_USER: The user to use when pushing artifacts
//    REPOSITORY_MANAGER_PASSWORD: The password to use when pushing artifacts
//    DOCKER_REGISTRY_USERNAME: The user to use logging into the docker registry
//    DOCKER_REGISTRY_PASSWORD: The password to use logging into the docker registry
//==================================================================================================

parameters {
    string(defaultValue: 'omar-build', description: 'The build node to run on', name: 'BUILD_NODE')
}

node("${BUILD_NODE}"){


    stage("Load Variables"){
        dir("ossim-ci"){
            git branch: "${OSSIM_GIT_BRANCH}", 
            url: "${GIT_PRIVATE_SERVER_URL}/ossim-ci.git", 
            credentialsId: "${CREDENTIALS_ID}"
        }

        load "ossim-ci/jenkins/variables/common-variables.groovy"
    }

    stage ("Assemble") {
        sh """
        pushd ./${appName}
        gradle assemble
        popd
        """
    }

    stage ("Publish Nexus")
    {
        withCredentials([[$class: 'UsernamePasswordMultiBinding',
                        credentialsId: 'nexusCredentials',
                        usernameVariable: 'MAVEN_REPO_USERNAME',
                        passwordVariable: 'MAVEN_REPO_PASSWORD']])
        {
            sh """
            pushd ./${appName}
            gradle publish \
                -PmavenRepoUsername=${MAVEN_REPO_USERNAME} \
                -PmavenRepoPassword=${MAVEN_REPO_PASSWORD}
            popd
            """
        }
    }

    stage ("Publish Docker App")
    {
        withCredentials([[$class: 'UsernamePasswordMultiBinding',
                        credentialsId: 'dockerCredentials',
                        usernameVariable: 'DOCKER_REGISTRY_USERNAME',
                        passwordVariable: 'DOCKER_REGISTRY_PASSWORD']])
        {
            // Run all tasks on the app. This includes pushing to OpenShift and S3.
            sh """
            pushd ${workspaceDir}/${appName}
            gradle pushDockerImage \
                -PdockerRegistryUsername=${DOCKER_REGISTRY_USERNAME} \
                -PdockerRegistryPassword=${DOCKER_REGISTRY_PASSWORD}
            popd
            """
        }
    }

    stage ("Trigger OpenShift Pull")
        {
            withCredentials([[$class: 'UsernamePasswordMultiBinding',
                              credentialsId: 'openshiftCredentials',
                              usernameVariable: 'OPENSHIFT_USERNAME',
                              passwordVariable: 'OPENSHIFT_PASSWORD']])
                {
                    // Run all tasks on the app. This includes pushing to OpenShift and S3.
                    sh """
                        pushd ${workspaceDir}/${appName}
                        gradle tagImage
                        popd
                    """
                }
        }

    try {
        stage('SonarQube analysis') {
            withSonarQubeEnv("${SONARQUBE_NAME}") {
                // requires SonarQube Scanner for Gradle 2.1+
                // It's important to add --info because of SONARJNKNS-281
                sh """
                  pushd ${workspaceDir}/${appName}/
                  gradle --info sonarqube
                  popd
                """
            }
        }
    }
    catch (e) {
        echo e.toString()
    }
        
   stage("Clean Workspace")
   {
      if ("${CLEAN_WORKSPACE}" == "true")
        step([$class: 'WsCleanup'])
   }
}
