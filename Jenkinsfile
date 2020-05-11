properties([
  buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '3', daysToKeepStr: '', numToKeepStr: '20')),
  disableConcurrentBuilds(),
  parameters([
    string(name: 'BUILD_NODE', defaultValue: 'omar-build', description: 'The build node to run on'),
    booleanParam(name: 'CLEAN_WORKSPACE', defaultValue: true, description: 'Clean the workspace at the end of the run')

  ])
])
podTemplate(
  containers: [
    containerTemplate(
      name: 'docker',
      image: 'docker:latest',
      ttyEnabled: true,
      command: 'cat',
      privileged: true
    ),
    containerTemplate(
      envVars: [
      ]
      image: "${params.DOCKER_REGISTRY}/ktis-builder:2.0", //TODO
      name: 'builder',
      command: 'cat',
      ttyEnabled: true
    )
  ],
  volumes: [
    hostPathVolume(
      hostPath: '/var/run/docker.sock',
      mountPath: '/var/run/docker.sock'
    ),
  ]
)
{
node("${BUILD_NODE}"){

    stage("Checkout branch $BRANCH_NAME")
    {
        checkout(scm)
    }

    stage("Load Variables")
    {
      withCredentials([string(credentialsId: 'o2-artifact-project', variable: 'o2ArtifactProject')]) {
        step ([$class: "CopyArtifact",
          projectName: o2ArtifactProject,
          filter: "common-variables.groovy",
          flatten: true])
        }
        load "common-variables.groovy"
    }
    stage('Build') {
      container('builder') {
 	      sh """
        ./gradlew assemble \
            -PossimMavenProxy=${MAVEN_DOWNLOAD_URL}
        """
        archiveArtifacts "plugins/*/build/libs/*.jar"
        archiveArtifacts "apps/*/build/libs/*.jar"
      }
    }
	stage ("Publish Nexus"){	
        withCredentials([[$class: 'UsernamePasswordMultiBinding',
                        credentialsId: 'nexusCredentials',
                        usernameVariable: 'MAVEN_REPO_USERNAME',
                        passwordVariable: 'MAVEN_REPO_PASSWORD']])
        {
          sh """
          ./gradlew publish \
              -PossimMavenProxy=${MAVEN_DOWNLOAD_URL}
          """
        }
    	}

  stage('Docker build') {
    container('docker') {
      withDockerRegistry(credentialsId: 'dockerCredentials', url: "https://${DOCKER_REGISTRY_DOWNLOAD_URL}") {  //TODO
        sh """
          docker build -t "${params.DOCKER_REGISTRY}"/${params.GIT_SERVICE_NAME}:${BRANCH_NAME} .
        """
      }
    }
    stage('Docker push'){
      container('docker') {
        withDockerRegistry(credentialsId: 'dockerCredentials', url: "https://${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}") {
        sh """
            docker push "${params.DOCKER_REGISTRY}"/${params.GIT_SERVICE_NAME}:${BRANCH_NAME}
        """
        }
		  }
	  }
  }
	stage("Clean Workspace"){
    if ("${CLEAN_WORKSPACE}" == "true")
      step([$class: 'WsCleanup'])
  }
}
}
    

