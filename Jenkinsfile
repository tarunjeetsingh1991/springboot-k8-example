pipeline {
    agent any

    tools {
        jdk 'jdk-17'
        maven 'maven'
    }

    environment {
        APP_NAME = 'springboot-crud-k8s'
        APP_VERSION = '1.0'
        IMAGE_NAME = "${APP_NAME}:${APP_VERSION}"
        DEPLOYMENT_NAME = 'springboot-crud-deployment'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/YOUR_USERNAME/YOUR_GITHUB_REPO_URL.git'
            }
        }

        stage('Build') {
            steps {
                bat 'mvn clean compile'
            }
        }

        stage('Test') {
            steps {
                bat 'mvn test'
            }
        }

        stage('Package') {
            steps {
                bat 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                bat 'docker build -t %IMAGE_NAME% .'
            }
        }

        stage('Load Image Into Minikube') {
            steps {
                bat 'minikube image load %IMAGE_NAME%'
            }
        }

        stage('Deploy MySQL Resources') {
            steps {
                bat 'kubectl apply -f mysql-secrets.yaml'
                bat 'kubectl apply -f mysql-configMap.yaml'
                bat 'kubectl apply -f db-deployment.yaml'
            }
        }

        stage('Deploy App Resources') {
            steps {
                bat 'kubectl apply -f app-deployment.yaml'
            }
        }

        stage('Restart Rollout') {
            steps {
                bat 'kubectl rollout restart deployment/%DEPLOYMENT_NAME%'
                bat 'kubectl rollout status deployment/%DEPLOYMENT_NAME%'
            }
        }

        stage('Verify') {
            steps {
                bat 'kubectl get deployments'
                bat 'kubectl get pods'
                bat 'kubectl get svc'
            }
        }
    }

    post {
        success {
            mail(
                to: 'singh.tarunjeet1991@gmail.com',
                subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """Jenkins build succeeded.

Job Name: ${env.JOB_NAME}
Build Number: ${env.BUILD_NUMBER}
Build URL: ${env.BUILD_URL}

The application was built, tested, dockerized, loaded into Minikube, and deployed to Kubernetes successfully.
"""
            )
        }

        failure {
            mail(
                to: 'singh.tarunjeet1991@gmail.com',
                subject: "FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """Jenkins build failed.

Job Name: ${env.JOB_NAME}
Build Number: ${env.BUILD_NUMBER}
Build URL: ${env.BUILD_URL}

Please check Jenkins Console Output for the exact failure.
"""
            )
        }

        always {
            echo 'Pipeline finished'
        }
    }
}