# Ενδεικτικό Χρονοδιάγραμμα Πτυχιακής Εργασίας

Βάσει της υπάρχουσας προόδου (ολοκληρωμένη Φάση 1 - Ανάλυση & βασικό Setup), το χρονοδιάγραμμα διαμορφώνεται ως εξής:

## ✅ Φάση 2 & 3: Backend Λειτουργίες (API) + Frontend UI (React) - Υπάρχει ήδη υλοποιήση, έλεγχος/βελτιώσεις και προσθήκες
- **Διάρκεια:** 21 ημέρες
- **Ημερομηνίες:** 17/04/2025 → 07/05/2025

## ✅ Φάση 4: Kubernetes + Minikube Integration
- **Διάρκεια:** 21 ημέρες
- **Ημερομηνίες:** 08/05/2025 → 28/05/2025

## ✅ Φάση 5: SSH Περιβάλλον Φοιτητών
- **Διάρκεια:** 26 ημέρες
- **Ημερομηνίες:** 29/05/2025 → 23/06/2025

## ✅ Φάση 6: Testing – Τελικές Ρυθμίσεις
- **Διάρκεια:** 21 ημέρες
- **Ημερομηνίες:** 24/06/2025 → 14/07/2025

## ✅ Φάση 7: Συγγραφή Πτυχιακής & Demo
- **Διάρκεια:** μέχρι το τέλος της προθεσμίας (με βάση τις διαθέσιμες 136 ημέρες)
- **Ημερομηνίες:** 15/07/2025 →  10/08/2025

---

Δομή Project με βάση το Github Page
```
thesis/
├── backend/               # Spring Boot
│   ├── src/
│   └── Dockerfile
├── frontend/              # React app
│   ├── src/
│   └── Dockerfile
├── database/              # SQL scripts, DB seeds
├── ssh-server/            # SSH setup (Dockerized)
├── k8s/                   # YAML configs για Kubernetes
└── docker-compose.yml     # Τοπικό dev περιβάλλον
```

### ✅ Προαπαιτούμενα

- Java 17+
- Docker
- [Minikube](https://minikube.sigs.k8s.io/docs/start/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- Gradle ή χρήση του `./gradlew`
  
(Βήματα αφότου έχει εγκατασταθεί επιτυχώς το Minikube)

When minikube start is given you have to see something like:
```
... 

💿  Downloading VM boot image ...
    > minikube-v1.35.0-amd64.iso....:  65 B / 65 B [---------] 100.00% ? p/s 0s
    > minikube-v1.35.0-amd64.iso:  345.38 MiB / 345.38 MiB  100.00% 8.85 MiB p/
👍  Starting "minikube" primary control-plane node in "minikube" cluster
💾  Downloading Kubernetes v1.32.0 preload ...
    > preloaded-images-k8s-v18-v1...:  333.57 MiB / 333.57 MiB  100.00% 8.00 Mi
🔥  Creating hyperkit VM (CPUs=2, Memory=2200MB, Disk=20000MB) ...
🐳  Preparing Kubernetes v1.32.0 on Docker 27.4.0 ...
    ▪ Generating certificates and keys ...
    ▪ Booting up control plane ...
    ▪ Configuring RBAC rules ...
🔗  Configuring bridge CNI (Container Networking Interface) ...
🔎  Verifying Kubernetes components...
    ▪ Using image gcr.io/k8s-minikube/storage-provisioner:v5
🌟  Enabled addons: storage-provisioner, default-storageclass
🏄  Done! kubectl is now configured to use "minikube" cluster and "default" namespace by default
```

```bash  
minikube start 
```

### Χρήση του Docker του Minikube
```bash
eval $(minikube docker-env)
```

### Δημιουργία JAR
```bash
./gradlew build -x test
```

### Docker Image της εφαρμογής
```bash
docker build -t thesis-backend:latest .
```

### Εφαρμογή στο Kubernetes
```bash
kubectl apply -f backend-deployment.yaml
```

### Πρόσβαση στην Εφαρμογή
```bash
minikube service thesis-backend-service
```
ή 
```bash
minikube ip
# π.χ. http://192.168.49.2:30007
```


| NAMESPACE |          NAME          | TARGET PORT |            URL            |
|-----------|------------------------|-------------|---------------------------|
| default   | thesis-backend-service |        8080 | http://192.168.65.3:30007 |

### Έλεγχος κατάστασης Pods και Services
```bash
kubectl get pods
kubectl get svc
```
