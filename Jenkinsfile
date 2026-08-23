pipeline {
    agent any
    parameters {
        string(name: 'GIT_URL', defaultValue: 'https://github.com/YOUR_ACCOUNT/xhzb-nursing-platform.git', description: 'Git仓库地址')
        string(name: 'GIT_TAG', defaultValue: '*/main', description: '要拉取的分支，如 */main 或 */master')
        string(name: 'services', defaultValue: 'xhzb-admin', description: '要构建的服务名，多个用逗号分隔，如 xhzb-admin,xhzb-oss')
        string(name: 'DOCKER_TAG', defaultValue: 'latest', description: 'Docker镜像标签')
    }
    options {
        timestamps()
    }
    tools {
        maven 'maven'
        jdk 'jdk17'
    }
    stages {
        stage('清除工作空间') {
            steps {
                cleanWs()
            }
        }
        stage('Build') {
            steps {
                sh 'java -version'
            }
        }
        stage('拉取Git代码') {
            steps {
                echo "正在拉取代码..."
                echo "当前分支:${params.GIT_TAG},当前服务:${params.services}"
                checkout([$class: 'GitSCM',
                          branches: [[name: params.GIT_TAG]],
                          doGenerateSubmoduleConfigurations: false,
                          extensions: [],
                          submoduleCfg: [],
                          userRemoteConfigs: [[credentialsId: 'GIT_CREDENTIALS_ID', url: params.GIT_URL]]
                ])
                sh "pwd"
            }
        }
        stage('重新Maven打包') {
            steps {
                script {
                    echo "正在执行maven打包...."
                    sh "/var/jenkins_home/maven/bin/mvn clean install -DskipTests"
                }
            }
        }
        stage('重新构建镜像') {
            steps {
                echo "当前打镜像tag:${params.DOCKER_TAG}"
                script {
                    for (ds in params.services.tokenize(",")) {
                         sh "pwd"
                         echo "进入target目录执行镜像打包......"
                         sh "cd ./${ds}/target/ && docker build -t ${ds}:${params.DOCKER_TAG} -f ../Dockerfile ."
                    }
                }
            }
        }
        stage('部署服务'){
            steps {
                script {
                    for (ws in params.services.tokenize(",")) {
                        sh "pwd"
                        sh "cd `pwd`"
                        echo "部署升级:${ws}服务"
                        sh "chmod +x ./${ws}/deploy.sh && sh ./${ws}/deploy.sh ${ws} ${params.DOCKER_TAG}"
                    }
                }
            }
        }

    }
    post {
        always {
            echo '任务构建完毕'
        }
    }
}