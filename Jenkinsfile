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

properties([
    parameters ([
        string(name: 'BUILD_NODE', defaultValue: 'omar-build', description: 'The build node to run on'),
        booleanParam(name: 'CLEAN_WORKSPACE', defaultValue: true, description: 'Clean the workspace at the end of the run')
    ])
])

node("${BUILD_NODE}"){

    stage("Checkout branch $BRANCH_NAME") {
        checkout(scm)
    }

    stage("Load Variables"){
        step ([$class: "CopyArtifact",
        projectName: "ossim-ci",
           filter: "common-variables.groovy",
           flatten: true])

        load "common-variables.groovy"
    }

    stage ("Assemble") {
        sh """
        gradle assemble \
            -PossimMavenProxy=${OSSIM_MAVEN_PROXY}
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
            gradle publish \
                -PossimMavenProxy=${OSSIM_MAVEN_PROXY} \
                -PmavenRepoUsername=${MAVEN_REPO_USERNAME} \
                -PmavenRepoPassword=${MAVEN_REPO_PASSWORD}
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
            gradle pushDockerImage \
                -PossimMavenProxy=${OSSIM_MAVEN_PROXY} \
                -PdockerRegistryUsername=${DOCKER_REGISTRY_USERNAME} \
                -PdockerRegistryPassword=${DOCKER_REGISTRY_PASSWORD}
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
                        gradle openshiftTagImage \
                            -PossimMavenProxy=${OSSIM_MAVEN_PROXY} \
                            -PopenshiftUsername=${OPENSHIFT_USERNAME} \
                            -PopenshiftPassword=${OPENSHIFT_PASSWORD}

                    """
                }
        }

    try {
        stage('SonarQube analysis') {
            withSonarQubeEnv("${SONARQUBE_NAME}") {
                // requires SonarQube Scanner for Gradle 2.1+
                // It's important to add --info because of SONARJNKNS-281
                sh """
                    gradle --info sonarqube
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
