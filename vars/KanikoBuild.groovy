def call(body) {
    // evaluate the body block, and collect configuration into the object
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    def label = "kaniko-${UUID.randomUUID().toString()}"

    podTemplate(name: 'kaniko', label: label, yaml: """
kind: Pod
metadata:
  name: kaniko
spec:
  containers:
  - name: kaniko
    image: gcr.io/kaniko-project/executor:debug
    imagePullPolicy: Always
    command:
    - /busybox/cat
    tty: true
    volumeMounts:
      - name: jenkins-docker-cfg
        mountPath: /kaniko/.docker
  volumes:
  - name: jenkins-docker-cfg
    projected:
      sources:
      - secret:
          name: docker-credentials 
          items:
            - key: .dockerconfigjson
              path: config.json
""")
    {
        node(label) {
            stage('Checkout') {
                checkout scm
            }
            stage("Prepare to build") {
                dockerOrg = pipelineParams.dockerOrg
                containerName = pipelineParams.containerName
            }
            stage('Build with Kaniko') {
                container(name: 'kaniko', shell: '/busybox/sh') {
                    withEnv(['PATH+EXTRA=/busybox']) {
                        sh "/kaniko/executor --context `pwd` --destination ${dockerOrg}/${containerName}:${BRANCH_NAME}"
                    }
                }
            }
        }
    }
}