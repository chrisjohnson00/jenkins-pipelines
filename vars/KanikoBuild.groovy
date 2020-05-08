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
""") {
    node(label) {
        stage('Build with Kaniko') {

            git 'https://github.com/cb-jeffduska/simple-docker-example.git'
            container(name: 'kaniko', shell: '/busybox/sh') {
                withEnv(['PATH+EXTRA=/busybox']) {
                    sh '''#!/busybox/sh
            /kaniko/executor --context `pwd` --destination chrisjohnson00/hello-kaniko:latest 
            '''
                }
            }
        }
    }
}