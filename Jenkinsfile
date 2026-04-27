pipeline {
    agent any

    options {
        timestamps()
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    parameters {
        string(name: 'APP_NAME', defaultValue: 'demo1', description: 'Kubernetes app name')
        string(name: 'NAMESPACE', defaultValue: 'demo1-dev', description: 'Kubernetes namespace')
        string(name: 'IMAGE_REGISTRY', defaultValue: 'registry.local/demo', description: 'Registry/repository without tag')
        string(name: 'IMAGE_TAG', defaultValue: '', description: 'Optional image tag. Empty = use BUILD_NUMBER-GIT_SHA')
        string(name: 'SERVICE_PORT', defaultValue: '8080', description: 'Application service port')
        string(name: 'REPLICAS', defaultValue: '1', description: 'Deployment replica count')
        booleanParam(name: 'FOLLOW_APP_LOGS', defaultValue: true, description: 'Tail application logs after rollout')
        booleanParam(name: 'AUTO_ROLLBACK_ON_FAILURE', defaultValue: true, description: 'Rollback deployment when rollout fails')
    }

    environment {
        CI_ENV_FILE = '.ci/jenkins.env'
        JAVA_HOME = ''
        PATH = "${env.JAVA_HOME}/bin:${env.PATH}"
        APP_PORT = ''
        REPLICAS = ''
        CPU_LIMIT = ''
        MEMORY_LIMIT = ''
        MEMORY_REQUEST = ''
        CPU_REQUEST = ''
        REGISTRY_CREDENTIALS_ID = ''
        KUBECONFIG_CREDENTIALS_ID = ''
        INGRESS_ENABLED = ''
        INGRESS_HOST = ''
        INGRESS_CLASS_NAME = ''
        ROUTE_ENABLED = ''
        ROUTE_HOST = ''
        IMAGE_PULL_SECRET = ''
        SERVICE_ACCOUNT_NAME = ''
        CONFIG_KEYSTORE_TEST = ''
        SECRET_KEYSTORE_PASSWORD = ''
        CLI_BIN = ''
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    def defaults = [
                        JDK_TOOL_NAME: 'jdk21',
                        REGISTRY_CREDENTIALS_ID: 'registry-creds',
                        KUBECONFIG_CREDENTIALS_ID: 'kubeconfig',
                        IMAGE_REGISTRY: 'registry.local/demo',
                        NAMESPACE: 'demo1-dev',
                        APP_NAME: 'demo1',
                        SERVICE_PORT: '8080',
                        REPLICAS: '1',
                        CPU_REQUEST: '500m',
                        CPU_LIMIT: '2',
                        MEMORY_REQUEST: '1024Mi',
                        MEMORY_LIMIT: '2560Mi',
                        INGRESS_ENABLED: 'true',
                        INGRESS_HOST: 'demo1.apps.example.local',
                        INGRESS_CLASS_NAME: 'nginx',
                        ROUTE_ENABLED: 'true',
                        ROUTE_HOST: 'demo1.apps.example.local',
                        IMAGE_PULL_SECRET: '',
                        SERVICE_ACCOUNT_NAME: 'demo1',
                        CONFIG_KEYSTORE_TEST: 'testsign',
                        SECRET_KEYSTORE_PASSWORD: ''
                    ]
                    def envFile = fileExists(env.CI_ENV_FILE) ? readProperties file: env.CI_ENV_FILE : [:]
                    def cfg = defaults + envFile
                    env.JAVA_HOME = tool name: cfg.JDK_TOOL_NAME, type: 'jdk'
                    env.PATH = "${env.JAVA_HOME}/bin:${env.PATH}"
                    env.APP_NAME = params.APP_NAME?.trim() ?: cfg.APP_NAME
                    env.NAMESPACE = params.NAMESPACE?.trim() ?: cfg.NAMESPACE
                    env.APP_PORT = params.SERVICE_PORT?.trim() ?: cfg.SERVICE_PORT
                    env.REPLICAS = params.REPLICAS?.trim() ?: cfg.REPLICAS
                    env.CPU_REQUEST = cfg.CPU_REQUEST
                    env.CPU_LIMIT = cfg.CPU_LIMIT
                    env.MEMORY_REQUEST = cfg.MEMORY_REQUEST
                    env.MEMORY_LIMIT = cfg.MEMORY_LIMIT
                    env.REGISTRY_CREDENTIALS_ID = cfg.REGISTRY_CREDENTIALS_ID
                    env.KUBECONFIG_CREDENTIALS_ID = cfg.KUBECONFIG_CREDENTIALS_ID
                    env.INGRESS_ENABLED = cfg.INGRESS_ENABLED
                    env.INGRESS_HOST = cfg.INGRESS_HOST
                    env.INGRESS_CLASS_NAME = cfg.INGRESS_CLASS_NAME
                    env.ROUTE_ENABLED = cfg.ROUTE_ENABLED
                    env.ROUTE_HOST = cfg.ROUTE_HOST
                    env.IMAGE_PULL_SECRET = cfg.IMAGE_PULL_SECRET
                    env.SERVICE_ACCOUNT_NAME = cfg.SERVICE_ACCOUNT_NAME
                    env.CONFIG_KEYSTORE_TEST = cfg.CONFIG_KEYSTORE_TEST
                    env.SECRET_KEYSTORE_PASSWORD = cfg.SECRET_KEYSTORE_PASSWORD
                    env.CLI_BIN = sh(script: 'command -v oc >/dev/null 2>&1 && echo oc || echo kubectl', returnStdout: true).trim()
                    env.GIT_SHA = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.RESOLVED_IMAGE_TAG = params.IMAGE_TAG?.trim() ? params.IMAGE_TAG.trim() : "${env.BUILD_NUMBER}-${env.GIT_SHA}"
                    env.IMAGE = "${(params.IMAGE_REGISTRY?.trim() ?: cfg.IMAGE_REGISTRY)}:${env.RESOLVED_IMAGE_TAG}"
                }
            }
        }

        stage('Build Jar') {
            steps {
                sh './mvnw -DskipTests clean package'
            }
        }

        stage('Build Image') {
            steps {
                sh '''
                    podman build \
                      -t "$IMAGE" \
                      .
                '''
            }
        }

        stage('Push Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: "${env.REGISTRY_CREDENTIALS_ID}", usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PASSWORD')]) {
                    sh '''
                        REGISTRY_HOST="$(printf '%s' "$IMAGE" | cut -d/ -f1)"
                        podman login "$REGISTRY_HOST" -u "$REGISTRY_USER" -p "$REGISTRY_PASSWORD"
                        podman push "$IMAGE"
                    '''
                }
            }
        }

        stage('Deploy To Kubernetes') {
            steps {
                withCredentials([file(credentialsId: "${env.KUBECONFIG_CREDENTIALS_ID}", variable: 'KUBECONFIG_FILE')]) {
                    sh '''
                        export KUBECONFIG="$KUBECONFIG_FILE"
                        CLI_BIN="${CLI_BIN}"
                        mkdir -p .rendered

                        sed \
                          -e "s|__APP_NAME__|$APP_NAME|g" \
                          -e "s|__NAMESPACE__|$NAMESPACE|g" \
                          -e "s|__IMAGE__|$IMAGE|g" \
                          -e "s|__APP_PORT__|$APP_PORT|g" \
                          -e "s|__REPLICAS__|$REPLICAS|g" \
                          -e "s|__CPU_REQUEST__|$CPU_REQUEST|g" \
                          -e "s|__CPU_LIMIT__|$CPU_LIMIT|g" \
                          -e "s|__MEMORY_REQUEST__|$MEMORY_REQUEST|g" \
                          -e "s|__MEMORY_LIMIT__|$MEMORY_LIMIT|g" \
                          -e "s|__SERVICE_ACCOUNT_NAME__|$SERVICE_ACCOUNT_NAME|g" \
                          k8s/deployment.yaml > .rendered/deployment.yaml

                        awk -v secret="$IMAGE_PULL_SECRET" '
                          /__IMAGE_PULL_SECRETS_BLOCK__/ {
                            if (secret != "") {
                              print "      imagePullSecrets:"
                              print "        - name: " secret
                            }
                            next
                          }
                          { print }
                        ' .rendered/deployment.yaml > .rendered/deployment.yaml.tmp && mv .rendered/deployment.yaml.tmp .rendered/deployment.yaml

                        sed \
                          -e "s|__APP_NAME__|$APP_NAME|g" \
                          -e "s|__NAMESPACE__|$NAMESPACE|g" \
                          -e "s|__APP_PORT__|$APP_PORT|g" \
                          k8s/service.yaml > .rendered/service.yaml

                        sed \
                          -e "s|__APP_NAME__|$APP_NAME|g" \
                          -e "s|__NAMESPACE__|$NAMESPACE|g" \
                          -e "s|__CONFIG_KEYSTORE_TEST__|$CONFIG_KEYSTORE_TEST|g" \
                          k8s/configmap.yaml > .rendered/configmap.yaml

                        SECRET_KEYSTORE_PASSWORD_B64="$(printf '%s' "$SECRET_KEYSTORE_PASSWORD" | base64 -w0)"
                        sed \
                          -e "s|__APP_NAME__|$APP_NAME|g" \
                          -e "s|__NAMESPACE__|$NAMESPACE|g" \
                          -e "s|__SECRET_KEYSTORE_PASSWORD_B64__|$SECRET_KEYSTORE_PASSWORD_B64|g" \
                          k8s/secret.yaml > .rendered/secret.yaml

                        if [ "$INGRESS_ENABLED" = "true" ]; then
                          sed \
                            -e "s|__APP_NAME__|$APP_NAME|g" \
                            -e "s|__NAMESPACE__|$NAMESPACE|g" \
                            -e "s|__APP_PORT__|$APP_PORT|g" \
                            -e "s|__INGRESS_HOST__|$INGRESS_HOST|g" \
                            -e "s|__INGRESS_CLASS_NAME__|$INGRESS_CLASS_NAME|g" \
                            k8s/ingress.yaml > .rendered/ingress.yaml
                        fi

                        if [ "$ROUTE_ENABLED" = "true" ]; then
                          sed \
                            -e "s|__APP_NAME__|$APP_NAME|g" \
                            -e "s|__NAMESPACE__|$NAMESPACE|g" \
                            -e "s|__APP_PORT__|$APP_PORT|g" \
                            -e "s|__ROUTE_HOST__|$ROUTE_HOST|g" \
                            k8s/route.yaml > .rendered/route.yaml
                        fi

                        sed \
                          -e "s|__APP_NAME__|$APP_NAME|g" \
                          -e "s|__NAMESPACE__|$NAMESPACE|g" \
                          k8s/serviceaccount.yaml > .rendered/serviceaccount.yaml

                        awk -v secret="$IMAGE_PULL_SECRET" '
                          /__IMAGE_PULL_SECRETS_BLOCK__/ {
                            if (secret != "") {
                              print "imagePullSecrets:"
                              print "  - name: " secret
                            }
                            next
                          }
                          { print }
                        ' .rendered/serviceaccount.yaml > .rendered/serviceaccount.yaml.tmp && mv .rendered/serviceaccount.yaml.tmp .rendered/serviceaccount.yaml

                        $CLI_BIN create namespace "$NAMESPACE" --dry-run=client -o yaml | $CLI_BIN apply -f -
                        $CLI_BIN apply -f .rendered/configmap.yaml
                        $CLI_BIN apply -f .rendered/secret.yaml
                        $CLI_BIN apply -f .rendered/serviceaccount.yaml
                        $CLI_BIN apply -f .rendered/deployment.yaml
                        $CLI_BIN apply -f .rendered/service.yaml
                        if [ -f .rendered/ingress.yaml ]; then
                          $CLI_BIN apply -f .rendered/ingress.yaml
                        fi
                        if [ -f .rendered/route.yaml ]; then
                          $CLI_BIN apply -f .rendered/route.yaml
                        fi
                    '''
                }
            }
        }

        stage('Watch Rollout') {
            steps {
                withCredentials([file(credentialsId: "${env.KUBECONFIG_CREDENTIALS_ID}", variable: 'KUBECONFIG_FILE')]) {
                    sh '''
                        export KUBECONFIG="$KUBECONFIG_FILE"
                        CLI_BIN="${CLI_BIN}"
                        $CLI_BIN -n "$NAMESPACE" rollout status deployment/"$APP_NAME" --timeout=300s
                        $CLI_BIN -n "$NAMESPACE" get pods -l app="$APP_NAME" -o wide
                        $CLI_BIN -n "$NAMESPACE" top pods -l app="$APP_NAME" || true
                        $CLI_BIN -n "$NAMESPACE" get svc "$APP_NAME" -o wide || true
                        $CLI_BIN -n "$NAMESPACE" get ingress "$APP_NAME" || true
                        $CLI_BIN -n "$NAMESPACE" get route "$APP_NAME" || true
                        $CLI_BIN -n "$NAMESPACE" get events --sort-by=.lastTimestamp | tail -n 30 || true
                    '''
                }
            }
        }

        stage('Watch App Logs') {
            when {
                expression { return params.FOLLOW_APP_LOGS }
            }
            steps {
                withCredentials([file(credentialsId: "${env.KUBECONFIG_CREDENTIALS_ID}", variable: 'KUBECONFIG_FILE')]) {
                    sh '''
                        export KUBECONFIG="$KUBECONFIG_FILE"
                        CLI_BIN="${CLI_BIN}"
                        POD_NAME="$($CLI_BIN -n "$NAMESPACE" get pods -l app="$APP_NAME" -o jsonpath='{.items[0].metadata.name}')"
                        $CLI_BIN -n "$NAMESPACE" logs "$POD_NAME" --tail=200 || true
                    '''
                }
            }
        }
    }

    post {
        always {
            script {
                if (fileExists('.rendered')) {
                    archiveArtifacts artifacts: '.rendered/*.yaml', onlyIfSuccessful: false
                }
            }
        }
        failure {
            withCredentials([file(credentialsId: "${env.KUBECONFIG_CREDENTIALS_ID}", variable: 'KUBECONFIG_FILE')]) {
                sh '''
                    export KUBECONFIG="$KUBECONFIG_FILE"
                    CLI_BIN="${CLI_BIN}"
                    $CLI_BIN -n "$NAMESPACE" get pods -l app="$APP_NAME" -o wide || true
                    $CLI_BIN -n "$NAMESPACE" get rs -l app="$APP_NAME" || true
                    $CLI_BIN -n "$NAMESPACE" get svc "$APP_NAME" -o wide || true
                    $CLI_BIN -n "$NAMESPACE" get ingress "$APP_NAME" || true
                    $CLI_BIN -n "$NAMESPACE" get route "$APP_NAME" || true
                    $CLI_BIN -n "$NAMESPACE" get events --sort-by=.lastTimestamp | tail -n 50 || true
                    $CLI_BIN -n "$NAMESPACE" describe deployment "$APP_NAME" || true
                    $CLI_BIN -n "$NAMESPACE" describe pods -l app="$APP_NAME" || true
                    $CLI_BIN -n "$NAMESPACE" logs deployment/"$APP_NAME" --tail=200 || true
                    if [ "${AUTO_ROLLBACK_ON_FAILURE}" = "true" ]; then
                      $CLI_BIN -n "$NAMESPACE" rollout undo deployment/"$APP_NAME" || true
                      $CLI_BIN -n "$NAMESPACE" rollout status deployment/"$APP_NAME" --timeout=300s || true
                    fi
                '''
            }
        }
    }
}
