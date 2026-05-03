# 🚀 AIVA - Projet Intégré Complet

## 📋 Vue d'ensemble

Ce projet est la **fusion complète** de tous les modules AIVA dans une seule branche `integration`:
- ✅ **Gestion Utilisateur** (Authentification + Rôles)
- ✅ **Alimentation** (Suivi calories)
- ✅ **Finance** (Gestion budgétaire)
- ✅ **Énergie** (Consommation énergétique)
- ✅ **Activités** (Exercice et sport)
- ✅ **Apprentissage** (Suivi éducation)
- ✅ **Objectifs** (Suivi de progression)

---

## 🔐 Authentification et Rôles

### Utilisateurs par défaut:

| Email | Mot de passe | Rôle | Accès |
|-------|-------------|------|-------|
| `admin@aiva.tn` | `admin123` | Admin | Tous les modules + Dashboard Admin |
| `user@aiva.tn` | `user123` | User | Dashboard personnel |

### Fonctionnement des rôles:

```java
User login:
  ├─ Vérifier credentials
  ├─ Si roles = 'admin' → AdminDashboard (7 modules)
  └─ Si roles = 'user' → UserDashboard (3 sections)
```

---

## 📦 Structure du Projet

```
PI-java-3A54/
├── src/main/java/
│   ├── Models/
│   │   ├── User.java              # ✅ Utilisateur avec rôles
│   │   ├── Alimentation.java      # ✅ Suivi nutritionnel
│   │   ├── Finance.java           # ✅ Gestion budgétaire
│   │   ├── Energie.java           # ✅ Consommation énergétique
│   │   ├── Activite.java          # ✅ Exercices sportifs
│   │   ├── Apprentissage.java     # ✅ Suivi éducatif
│   │   └── Objectif.java          # ✅ Objectifs personnels
│   │
│   ├── Services/
│   │   ├── ServiceUser.java           # ✅ Authentification
│   │   ├── ServiceAlimentation.java   # ✅ CRUD Alimentation
│   │   ├── ServiceFinance.java        # ✅ CRUD Finance
│   │   ├── ServiceEnergie.java        # ✅ CRUD Énergie
│   │   ├── ServiceActivite.java       # ✅ CRUD Activités
│   │   ├── ServiceApprentissage.java  # ✅ CRUD Apprentissage
│   │   └── IService.java              # ✅ Interface générique
│   │
│   ├── utils/
│   │   ├── MyDatabase.java        # ✅ Connexion BD
│   │   └── SessionManager.java    # ✅ Gestion de session
│   │
│   └── controllers/
│       ├── LoginController.java       # ✅ Page de connexion
│       ├── AdminDashboardController.java  # ✅ Dashboard admin
│       └── UserDashboardController.java   # ✅ Dashboard user
│
├── src/main/resources/
│   ├── views/
│   │   ├── Login.fxml              # ✅ Interface login
│   │   ├── AdminDashboard.fxml     # ✅ Dashboard admin
│   │   └── UserDashboard.fxml      # ✅ Dashboard user
│   │
│   ├── styles/
│   │   └── style.css               # ✅ Styles communs
│   │
│   └── database/
│       └── init.sql                # ✅ Script de création BD
│
├── pom.xml                          # ✅ Configuration Maven
└── README_INTEGRATION.md            # ✅ Documentation
```

---

## 🛠️ Installation et Configuration

### 1. **Cloner la branche intégrée:**

```bash
git clone https://github.com/Oussema-Tarchoun/PI-java-3A54.git
cd PI-java-3A54
git checkout integration
```

### 2. **Créer la base de données:**

```bash
mysql -u root -p < src/main/resources/database/init.sql
```

**Résultat:** 
- ✅ Base `aiva` créée
- ✅ 8 tables créées (user, alimentation, finance, énergie, activité, apprentissage, objectif)
- ✅ Données de test insérées

### 3. **Configuration BD (si nécessaire):**

Modifier `src/main/java/utils/MyDatabase.java`:

```java
private final String URl = "jdbc:mysql://localhost:3306/aiva";
private final String USERNAME = "root";
private final String PASSWORD = ""; // Votre mot de passe
```

### 4. **Compiler le projet:**

```bash
mvn clean install
```

### 5. **Lancer l'application:**

```bash
mvn javafx:run
```

---

## 🎮 Utilisation

### Login:

1. **Ouvrir l'application** → Page de connexion
2. **Entrer les identifiants:**
   - Admin: `admin@aiva.tn` / `admin123`
   - User: `user@aiva.tn` / `user123`
3. **Cliquer "Se connecter"**

### Pour Admin:

- ✅ **Dashboard Admin** avec accès à 7 modules
- ✅ Gérer les utilisateurs
- ✅ Consulter toutes les données
- ✅ Analytics et statistiques
- ✅ Gestion des modules

### Pour User Normal:

- ✅ **Dashboard Personnel** avec:
  - 💪 Mes Activités
  - 🎯 Mes Objectifs
  - 📊 Statistiques personnelles
- ✅ Ajouter/Modifier/Supprimer ses données

---

## 🗄️ Base de Données

### Tables créées:

| Table | Description | Clé étrangère |
|-------|-------------|---------------|
| `user` | Utilisateurs avec rôles | - |
| `alimentation` | Suivi nutritionnel | user_id |
| `finance` | Gestion budgétaire | user_id |
| `energie` | Consommation énergétique | user_id |
| `activite` | Exercices et sports | user_id |
| `apprentissage` | Suivi éducatif | user_id |
| `objectif` | Objectifs personnels | user_id |

---

## 🔧 Dépendances Maven

```xml
<!-- MySQL -->
<mysql-connector-j>8.0.33</mysql-connector-j>

<!-- JavaFX -->
<javafx-controls>17.0.2</javafx-controls>
<javafx-fxml>17.0.2</javafx-fxml>
<javafx-swing>17.0.2</javafx-swing>

<!-- QR Code -->
<zxing-core>3.5.2</zxing-core>
<zxing-javase>3.5.2</zxing-javase>

<!-- JSON -->
<json>20231013</json>

<!-- PDF -->
<openpdf>1.3.30</openpdf>

<!-- Email -->
<javax.mail>1.6.2</javax.mail>
```

---

## 🎯 Flux d'Application

```
Application Start
    ↓
Login Page (Login.fxml)
    ↓
Authentification (ServiceUser)
    ↓
┌─────────────────────────────────┐
│  SessionManager (Rôle?)         │
└──────────────┬──────────────────┘
               ├─ ADMIN  → AdminDashboard (7 modules)
               └─ USER   → UserDashboard (3 sections)
```

---

## 🔒 Sécurité

- ✅ **Authentification** basée sur email/mot de passe
- ✅ **Session Management** pour tracker l'utilisateur
- ✅ **Rôles et permissions** (admin vs user)
- ✅ **Isolation des données** par user_id
- ✅ **FOREIGN KEY constraints** pour l'intégrité des données

---

## 📝 Notes Importantes

1. **Les autres branches restent INTACTES** - Aucune modification à `GestionUser`, `Apprentissage`, etc.
2. **Seule la branche `integration`** contient le projet complet et fusionné
3. **Utilisateurs de test** fournis par défaut pour démarrage rapide
4. **Design unifié** appliqué à toutes les interfaces

---

## 🚀 Prochaines étapes

- [ ] Ajouter des validations plus strictes
- [ ] Implémenter le chiffrage des mots de passe (BCrypt)
- [ ] Ajouter 2FA (Two-Factor Authentication)
- [ ] Améliorer les graphiques et statistiques
- [ ] Ajouter l'export en PDF/Excel
- [ ] Implémenter des notifications
- [ ] Ajouter des rappels automatiques

---

## 👨‍💻 Support

Pour toute question ou problème:
1. Vérifier la configuration de la BD
2. Vérifier les imports Java
3. Vérifier les fichiers FXML
4. Consulter les logs d'erreur

---

**✅ Projet complètement intégré et prêt à déployer!** 🎉
