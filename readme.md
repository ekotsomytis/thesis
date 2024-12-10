# Development lifecycle

run ./gradlew bootjar to build the executable jar 

Configure your shell to use Minikube's Docker daemon if it's not already configured
run eval $(minikube docker-env)
in windows is : & minikube -p minikube docker-env | Invoke-Expression 
This command sets shell environment to use the Minikube Docker daemon.

build new Docker image with the latest changes:
run docker build -t thesis-prototype:latest .

# Deploy to kubernetes
run: kubectl rollout restart deployment thesis-prototype-deployment
 (This command restarts the pods in the deployment with the latest image.)

# Access the Application
we can access the application by using the minikube ip and the NodePort 
http://<minikube-ip>:30008

