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

## Κατάσταση Έργου (07/07/2025)

### Τι έχει υλοποιηθεί:
- ✅ **Backend (Spring Boot + Kubernetes)**: Δημιουργία πραγματικών SSH-enabled containers
- ✅ **Frontend (React)**: Διαχείριση containers και εμφάνιση SSH οδηγιών
- ✅ **Kubernetes Integration**: Πλήρης ενσωμάτωση με Minikube για deployment
- ✅ **SSH Environment**: Οι φοιτητές μπορούν να συνδεθούν με SSH στα containers τους
- ✅ **Real-time Status**: Αυτόματη ανανέωση κατάστασης containers κάθε 30 δευτερόλεπτα
- ✅ **User Authentication**: Σύστημα εισόδου για καθηγητές και φοιτητές

### Λειτουργίες:
- **Καθηγητές**: Δημιουργία containers για φοιτητές, παρακολούθηση όλων των containers
- **Φοιτητές**: Πρόσβαση στα δικά τους containers, SSH σύνδεση με αντιγραφή εντολών
- **Διαχειριστές**: Πλήρη διαχείριση χρηστών και containers

### Open issues (Τα χρειαζόμαστε;)
- Actual delete pods if needed (in progress)
- Admin role 
- New template creation + new students, υπάρχουν προκαθορισμένοι χρήστες στα πλαίσια του demo.

---

## 📁 Δομή Project
```
thesis/
├── thesis-backend-starter/    # Spring Boot Backend
│   ├── src/main/java/        # Java source code
│   ├── src/main/resources/   # Configuration files
│   ├── build.gradle          # Dependencies & build config
│   ├── Dockerfile.ssh        # SSH-enabled Docker image
│   └── k8s-deployment.yaml   # Kubernetes deployment
├── thesis_frontend_prototype/ # React Frontend
│   ├── src/                  # React components & pages
│   ├── public/               # Static assets
│   ├── package.json          # Dependencies
│   └── build/                # Production build
├── mariadb-*.yaml            # Database configurations
├── *.sql                     # Database initialization scripts
└── README.md                 # Αυτό το αρχείο
```

## 🚀 Γρήγορη Εκκίνηση

### 1. Προαπαιτούμενα

- Java 17+
- Node.js 16+ και npm
- Docker
- [Minikube](https://minikube.sigs.k8s.io/docs/start/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- Gradle (ή χρήση του `./gradlew`)

### 2. Εκκίνηση Minikube

```bash
# Εκκίνηση Minikube
minikube start

# Χρήση του Docker του Minikube
eval $(minikube docker-env)
```

### 3. Δημιουργία SSH-enabled Docker Image

```bash
# Μετάβαση στον φάκελο backend
cd thesis-backend-starter

# Δημιουργία SSH-enabled image
docker build -f Dockerfile.ssh -t thesis-ssh-container:latest .
```

### 4. Εκκίνηση Backend

```bash
# Από τον φάκελο thesis-backend-starter
./gradlew bootRun
```

Το backend θα εκκινήσει στο http://localhost:8080

### 5. Εκκίνηση Frontend

```bash
# Σε νέο terminal, μετάβαση στον φάκελο frontend
cd thesis_frontend_prototype

# Εγκατάσταση dependencies (μόνο την πρώτη φορά)
npm install

# Εκκίνηση development server
npm start
```

Το frontend θα εκκινήσει στο http://localhost:3000

### 6. Κτίσιμο Backend & Frontend Images

```bash
# Χρήση του Docker του Minikube (αν δεν το κάναμε ήδη)
eval $(minikube docker-env)

# Backend API image
docker build -t thesis-backend:latest ./thesis-backend-starter

# Frontend React image
docker build -t thesis-frontend:latest ./thesis_frontend_prototype
```

### 7. Deployment στο Minikube

```bash
# Database
kubectl apply -f mariadb-init-configmap.yaml
kubectl apply -f mariadb-deployment.yaml

# Backend
kubectl apply -f thesis-backend-starter/backend-deployment.yaml

# Frontend
kubectl apply -f thesis_frontend_prototype/k8s-deployment.yaml
```

Μετά το deployment, πάρτε τα NodePort URLs:

```bash
kubectl get svc

# Ή απευθείας
minikube service thesis-frontend-service --url
minikube service thesis-backend-service --url
```


## 👥 Χρήση της Εφαρμογής

### Προκαθορισμένοι Χρήστες:

| Όνομα Χρήστη | Κωδικός | Ρόλος | Περιγραφή |
|---------------|---------|-------|-----------|
| `teacher` | `TeachSecure2024!` | Καθηγητής | Δημιουργία containers για φοιτητές |
| `student` | `StudyHard2024#` | Φοιτητής | Πρόσβαση σε προσωπικά containers |
| `admin` | `AdminPower2024$` | Διαχειριστής | Πλήρη διαχείριση συστήματος |

### Ροή Εργασίας:

1. **Σύνδεση**: Χρήση των παραπάνω διαπιστευτηρίων
2. **Καθηγητής**: 
   - Πήγαινε στο "Container Management"
   - Δημιούργησε container για φοιτητή
   - Παρακολούθησε την κατάσταση όλων των containers
3. **Φοιτητής**:
   - Πήγαινε στο "My Containers" 
   - Δες τα διαθέσιμα containers
   - Πάτησε "SSH Info" για οδηγίες σύνδεσης

## 🔧 SSH Σύνδεση

Όταν ένα container είναι έτοιμο, οι φοιτητές θα δουν:

### Μέθοδος 1: Άμεση Σύνδεση (ενδέχεται να μη λειτουργεί σε macOS)
```bash
ssh -p [NodePort] root@[MinikubeIP]
```

### Μέθοδος 2: Port Forwarding (Προτεινόμενο για macOS/Minikube)
```bash
# Βήμα 1: Άνοιγμα terminal
kubectl port-forward service/[service-name] 8023:22

# Βήμα 2: Σε νέο terminal
ssh -p 8023 root@127.0.0.1

# Κωδικός: student123
```

### Γιατί Διαφορετικές Πόρτες;

- **NodePort (π.χ. 31945)**: Τυχαία πόρτα που δίνει το Kubernetes (30000-32767)
- **Port Forward (8023)**: Τοπική πόρτα που επιλέγουμε για ευκολία

Η εφαρμογή παρέχει αυτόματα αντιγραφή όλων των εντολών με κουμπιά "Copy"!

## 🔍 Έλεγχος Κατάστασης

### Backend Status
```bash
# Έλεγχος αν το backend τρέχει
curl http://localhost:8080/api/auth/test

# Αναμενόμενη απάντηση: "Authentication is working!"
```

### Kubernetes Status
```bash
# Έλεγχος pods
kubectl get pods

# Έλεγχος services
kubectl get svc

# Έλεγχος όλων των resources
kubectl get all
```

### Database Status
```bash
# Το backend χρησιμοποιεί H2 in-memory database
# Δεδομένα διαθέσιμα στο: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:testdb
```

## 🛠️ Προχωρημένες Εντολές

### Rebuild Backend
```bash
cd thesis-backend-starter
./gradlew clean build -x test
./gradlew bootRun
```

### Rebuild Frontend
```bash
cd thesis_frontend_prototype
npm run build
npm start
```

### Reset Kubernetes Environment
```bash
# Διαγραφή όλων των pods και services
kubectl delete pods --all
kubectl delete services --all --selector=app!=kubernetes

# Επανεκκίνηση Minikube
minikube stop
minikube start
eval $(minikube docker-env)
```

### Debug SSH Connections
```bash
# Έλεγχος αν το SSH service τρέχει
kubectl get svc | grep ssh

# Έλεγχος logs από SSH container
kubectl logs [pod-name]

# Test SSH connectivity
ssh -p [port] -o ConnectTimeout=5 root@[host]
```

## 📋 Troubleshooting

### Συνήθη Προβλήματα:

1. **"Backend not responding"**
   ```bash
   # Έλεγχος αν τρέχει
   lsof -i :8080
   # Αν όχι, εκκίνηση ξανά
   cd thesis-backend-starter && ./gradlew bootRun
   ```

2. **"SSH connection refused"**
   ```bash
   # Χρήση port forwarding αντί για άμεση σύνδεση
   kubectl port-forward service/[service-name] 8023:22
   ssh -p 8023 root@127.0.0.1
   ```

3. **"Container stuck in Pending"**
   ```bash
   # Έλεγχος events
   kubectl describe pod [pod-name]
   # Πιθανά θέματα με resources ή images
   ```

4. **"Minikube not accessible"**
   ```bash
   # Επανεκκίνηση Minikube
   minikube stop && minikube start
   eval $(minikube docker-env)
   ```

## 📖 API Documentation

### Authentication Endpoints
- `POST /api/auth/login` - Σύνδεση χρήστη
- `GET /api/auth/test` - Έλεγχος authentication

### Container Management
- `GET /api/containers/my-containers` - Containers του χρήστη
- `POST /api/containers/create-for-student` - Δημιουργία container (καθηγητές)
- `GET /api/containers/{id}/ssh-info` - SSH πληροφορίες
- `POST /api/containers/{id}/refresh-status` - Ανανέωση κατάστασης

### Image Templates
- `GET /api/images` - Διαθέσιμα templates

---

## 📺 Demo & Screenshots

### 🎬 Βίντεο Παρουσίασης
**[Educational Container Platform - Full Demo](https://drive.google.com/file/d/1fdohHMIUZMHQjFXh8YItTLzdNB3PBgPy/view?usp=sharing)**

**Περιεχόμενο Demo:**
1. 🔐 Login ως καθηγητής και φοιτητής
2. 🐳 Δημιουργία νέου container για φοιτητή
3. ⚡ Real-time status updates
4. 🔧 SSH connection setup με port forwarding
5. 💻 Πλήρη πρόσβαση στο Ubuntu terminal
6. 📋 Copy-paste SSH commands από το UI
7. 🔄 Container lifecycle management

### 📸 Επιτυχημένη SSH Σύνδεση
Το screenshot δείχνει πλήρη πρόσβαση στο Ubuntu container:
```bash
ssh -p 8023 root@127.0.0.1
# Welcome to Ubuntu 20.04.6 LTS (GNU/Linux 6.10.14-linuxkit x86_64)
# root@container-student-20250707150626:~#
```
<img width="569" alt="Screenshot 2025-07-07 at 3 27 38 PM" src="https://github.com/user-attachments/assets/a668d918-ecff-4e2a-8f80-0648fa65f720" />



**Αποδεικνύει:**
- Επιτυχή SSH authentication
- Πλήρη shell access
- Ubuntu 20.04.6 LTS environment
- Root privileges για educational purposes
  Network connectivity μέσω port forwarding

