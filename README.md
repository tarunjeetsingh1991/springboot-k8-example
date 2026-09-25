# Spring Boot CRUD on Kubernetes with MySQL and Jenkins CI/CD

## Project overview

This project demonstrates a full local deployment pipeline for a Spring Boot CRUD application using:

- Spring Boot 4.x
- MySQL 8.4
- Docker multi-stage build
- Kubernetes on Minikube
- Jenkins pipeline
- GitHub source control
- Postman for API testing

The application runs in Kubernetes, connects to a MySQL pod through a Kubernetes Service, stores database data on a PersistentVolumeClaim, and can be built and deployed automatically from Jenkins after code is pushed to GitHub.

---

## What this project demonstrates

This project is beyond a basic Kubernetes demo. It includes:

- Kubernetes Deployment for the Spring Boot application
- Kubernetes Deployment for MySQL
- ClusterIP Service for MySQL
- NodePort Service for the application
- ConfigMap for database configuration
- Secret for database credentials
- PersistentVolumeClaim for MySQL storage
- Docker image build and Minikube image load
- Jenkins CI/CD pipeline triggered from GitHub

This is a strong beginner-to-intermediate Kubernetes and DevOps project.

---

## Architecture

GitHub -> Jenkins -> Docker Build -> Minikube Image Load -> Kubernetes Deployment -> Spring Boot Pods -> MySQL Pod -> PVC

### Runtime flow

1. User sends API request from Postman or browser.
2. Request reaches the Spring Boot Kubernetes Service.
3. One Spring Boot pod handles the request.
4. Spring Boot connects to MySQL using the Kubernetes Service name `mysql`.
5. MySQL stores data on persistent storage through the PVC.

---

## Prerequisites

Install and verify these tools on Windows:

- Java 17
- Maven 3.9+
- Docker Desktop
- Minikube
- kubectl
- Git
- Jenkins

Useful checks:

```bash
java -version
mvn -version
docker version
minikube version
kubectl version --client
git --version
```

---

## Project structure

```text
springboot-crud-k8s-example/
├── src/
├── pom.xml
├── Dockerfile
├── db-deployment.yaml
├── mysql-secrets.yaml
├── mysql-configMap.yaml
├── app-deployment.yaml
└── Jenkinsfile
```

---

## Spring Boot configuration

Use environment variables so the same image works locally and in Kubernetes.

Example `application.yml`:

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${DB_HOST}:3306/${DB_NAME}?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=America/Toronto
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update

server:
  port: 8080
```

Inside Kubernetes the app should connect to:

- `DB_HOST=mysql`
- `DB_NAME=productdb`
- `DB_USERNAME=root`
- `DB_PASSWORD=<from secret>`

---

## Dockerfile

Use a multi-stage Docker build so Docker creates the jar automatically.

```dockerfile
FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build the image:

```bash
docker build -t springboot-crud-k8s:1.0 .
```

---

## Kubernetes files

### 1. MySQL Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: mysql-secrets
type: Opaque
stringData:
  password: bolina1497
```

### 2. MySQL ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: db-config
data:
  dbName: productdb
```

### 3. MySQL Deployment and Service

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mysql-pv-claim
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mysql
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mysql
      tier: database
  strategy:
    type: Recreate
  template:
    metadata:
      labels:
        app: mysql
        tier: database
    spec:
      containers:
        - name: mysql
          image: mysql:8.4
          env:
            - name: MYSQL_ROOT_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: mysql-secrets
                  key: password
            - name: MYSQL_DATABASE
              valueFrom:
                configMapKeyRef:
                  name: db-config
                  key: dbName
          ports:
            - containerPort: 3306
          volumeMounts:
            - name: mysql-persistent-storage
              mountPath: /var/lib/mysql
      volumes:
        - name: mysql-persistent-storage
          persistentVolumeClaim:
            claimName: mysql-pv-claim
---
apiVersion: v1
kind: Service
metadata:
  name: mysql
spec:
  type: ClusterIP
  ports:
    - port: 3306
      targetPort: 3306
  selector:
    app: mysql
    tier: database
```

### 4. Spring Boot Deployment and Service

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: springboot-crud-deployment
spec:
  replicas: 3
  selector:
    matchLabels:
      app: springboot-k8s-mysql
  template:
    metadata:
      labels:
        app: springboot-k8s-mysql
    spec:
      containers:
        - name: springboot-crud-k8s
          image: springboot-crud-k8s:1.0
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
          env:
            - name: DB_HOST
              value: mysql
            - name: DB_NAME
              valueFrom:
                configMapKeyRef:
                  name: db-config
                  key: dbName
            - name: DB_USERNAME
              value: root
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: mysql-secrets
                  key: password
---
apiVersion: v1
kind: Service
metadata:
  name: springboot-crud-svc
spec:
  selector:
    app: springboot-k8s-mysql
  ports:
    - protocol: TCP
      port: 8080
      targetPort: 8080
  type: NodePort
```

---

## Manual deployment steps

### Start Docker and Minikube

```bash
minikube start --driver=docker
minikube status
docker version
kubectl get nodes
```

### Build and load the image

```bash
docker build -t springboot-crud-k8s:1.0 .
minikube image load springboot-crud-k8s:1.0
```

### Apply Kubernetes manifests

```bash
kubectl apply -f mysql-secrets.yaml
kubectl apply -f mysql-configMap.yaml
kubectl apply -f db-deployment.yaml
kubectl apply -f app-deployment.yaml
```

### Verify

```bash
kubectl get pods
kubectl get deployments
kubectl get svc
kubectl get pvc
```

### Open the app

```bash
minikube service springboot-crud-svc --url

open the dashboard, use : 
minikube dashboard
```

If needed, use:

```bash
kubectl logs -l app=springboot-k8s-mysql
kubectl logs -l app=mysql
```

---

## Access MySQL from the command line

Open a shell in the MySQL pod:

```bash
kubectl exec -it <mysql-pod-name> -- bash
```

Then run:

```bash
mysql -u root -p
```

Check data:

```sql
SHOW DATABASES;
USE productdb;
SHOW TABLES;
SELECT * FROM orders_tbl;
```

This is useful while testing Postman requests.

---

## Jenkins CI/CD with GitHub

### Pipeline goal

When code is pushed to GitHub:

1. Jenkins pulls the repository
2. Jenkins builds the Spring Boot application
3. Jenkins builds the Docker image
4. Jenkins loads the image into Minikube
5. Jenkins applies Kubernetes YAML files
6. Jenkins rolls out the latest application version

### Jenkins prerequisites

Install Jenkins and make sure the Jenkins machine can run:

```bash
git --version
docker version
kubectl version --client
minikube status
```

Also install these Jenkins plugins:

- Pipeline
- Git
- GitHub
- Credentials Binding

The Jenkins Pipeline docs explain how Jenkinsfile-based pipelines work, and the Git plugin provides Git support in Jenkins. Jenkins recommends defining pipelines in source control. See official Jenkins Pipeline and Git plugin docs for details. 

### Jenkins credentials

Add these credentials in Jenkins:

- GitHub username or token if your repository is private
- Docker Hub credentials only if you later decide to push images to Docker Hub
- No extra Docker registry credentials are needed if you only load images into Minikube locally

### Jenkinsfile

Add this file in the project root:

```groovy
pipeline {
    agent any

    environment {
        APP_NAME = 'springboot-crud-k8s'
        APP_VERSION = '1.0'
        IMAGE_NAME = "${APP_NAME}:${APP_VERSION}"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/YOUR_USERNAME/YOUR_REPO.git'
            }
        }

        stage('Build JAR') {
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

        stage('Restart App Rollout') {
            steps {
                bat 'kubectl rollout restart deployment/springboot-crud-deployment'
                bat 'kubectl rollout status deployment/springboot-crud-deployment'
            }
        }
    }

    post {
        always {
            bat 'kubectl get pods'
            bat 'kubectl get svc'
        }
        success {
            echo 'Build and deployment completed successfully.'
        }
        failure {
            echo 'Build or deployment failed.'
        }
    }
}
```

---

## GitHub to Jenkins webhook

If your Jenkins server is reachable from GitHub, add a webhook in GitHub:

- Payload URL: `http://YOUR_JENKINS_URL/github-webhook/`
- Content type: `application/json`
- Event: `Just the push event`

For webhook security, GitHub recommends adding a secret token and validating it on the receiver side.

If Jenkins is running only on your local machine and is not exposed publicly, GitHub cannot reach it directly. In that case, trigger the build manually, use polling, or expose Jenkins temporarily with a tunneling tool.

---

## Full step-by-step runbook from zero

### A. Start local infrastructure

```bash
docker version
minikube start --driver=docker
minikube status
kubectl get nodes
```

### B. Start Jenkins

On Windows services, start Jenkins and open:

```text
http://localhost:8080
```

### C. Push the project to GitHub

```bash
git init
git add .
git commit -m "Initial Spring Boot Kubernetes project"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
git push -u origin main
```

### D. Add Jenkins pipeline job

1. Open Jenkins
2. Click New Item
3. Enter a job name
4. Choose Pipeline
5. Under Pipeline definition, choose Pipeline script from SCM
6. SCM = Git
7. Repository URL = your GitHub URL
8. Branch = `*/main`
9. Script Path = `Jenkinsfile`
10. Save

### E. Trigger the build

#### Manual
Click **Build Now**

#### Automatic
Set a GitHub webhook to:

```text
http://YOUR_JENKINS_URL/github-webhook/
```

GitHub sends an HTTP request to that URL when the selected event happens.

### F. Validate deployment

```bash
kubectl get deployments
kubectl get pods
kubectl get svc
kubectl logs -l app=springboot-k8s-mysql
minikube service springboot-crud-svc --url
```

---

## Troubleshooting

### ErrImagePull
Run:

```bash
minikube image load springboot-crud-k8s:1.0
```

### Secret not found
Create or reapply:

```bash
kubectl apply -f mysql-secrets.yaml
```

### ConfigMap not found
Create or reapply:

```bash
kubectl apply -f mysql-configMap.yaml
```

### MySQL CrashLoopBackOff
Delete old PVC and redeploy if the data directory is corrupted:

```bash
kubectl delete deployment mysql
kubectl delete svc mysql
kubectl delete pvc mysql-pv-claim
kubectl apply -f db-deployment.yaml
```

### NodePort not reachable on Windows with Docker driver
Use:

```bash
minikube service springboot-crud-svc --url
```

or:

```bash
kubectl port-forward service/springboot-crud-svc 8080:8080
```

---

## Suggested next improvements

To move this project toward advanced Kubernetes and DevOps:

- Replace MySQL Deployment with StatefulSet
- Add readiness and liveness probes to the Spring Boot app
- Add resource requests and limits
- Push image to Docker Hub instead of only loading into Minikube
- Add Ingress instead of NodePort
- Add automated rollback and version tagging
- Add Jenkins shared library or multi-branch pipeline

---

## Official references

This README follows the workflows described in official documentation for:

- Jenkins Pipeline and Jenkins Git support
- GitHub webhooks
- Minikube image loading and service access
- Kubernetes ConfigMaps, Secrets, and PersistentVolumes
