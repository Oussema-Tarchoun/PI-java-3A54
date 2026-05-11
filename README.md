# AIVA - Artificial Intelligence Visual Assistant 🍎🏋️‍♂️

AIVA is a comprehensive Health and Nutrition Management System built with **JavaFX** and integrated with **AI (Ollama/Groq)**. It provides a modern, data-driven platform for users to track their nutritional intake, manage daily activities, and receive personalized health recommendations.

## 🚀 Features

### 🔐 Security & Authentication
- **Secure Access**: Login and registration with password hashing (BCrypt).
- **Two-Factor Authentication (2FA)**: Enhanced security using TOTP and QR code verification.
- **Account Recovery**: Password reset functionality via email integration (Mailtrap/Jakarta Mail).

### 📊 Personal Dashboard
- **Real-time Statistics**: Interactive dashboard for both users (Front-office) and administrators (Back-office).
- **Data Visualization**: Track progress through visual summaries of nutritional data and activities.

### 🥗 Nutrition & Meal Management
- **Food Database**: Manage a comprehensive list of foods (Aliments).
- **Meal Tracking**: Log daily meals (Repas) and associate them with specific foods.
- **AI Nutritional Analysis**: Get instant AI-powered analysis of your meals and nutritional intake.

### 🤖 AI Integration
- **Smart Chatbot**: Integrated AI assistant for health and nutrition advice (Powered by Ollama/Llama 3).
- **Learning Roadmaps**: Automatically generated roadmaps for health goals.
- **AI Analysis**: Detailed breakdown of nutritional values using advanced LLMs.

### 🏃 Activity & Goals
- **Activity Tracking**: Log physical activities and monitor energy expenditure.
- **Goal Setting**: Define health objectives and track your journey toward achieving them.

### 📄 Reporting & Utilities
- **PDF/Word Export**: Generate professional reports of your progress using iText and Apache POI.
- **Weather Integration**: Live weather widget for planning outdoor activities.
- **Modern UI**: Sleek, responsive interface with glassmorphism and custom CSS themes.

---

## 🛠️ Technologies Used

- **Language**: Java 17
- **GUI Framework**: JavaFX 21
- **Database**: MySQL (Hibernate ORM)
- **AI Models**: Ollama (Llama 3), Groq API
- **Networking**: OkHttp, Apache HttpClient
- **Security**: TOTP, ZXing (QR), BCrypt
- **Reports**: iText, Apache POI
- **Mailing**: Jakarta Mail, Mailtrap

---

## 📦 Installation & Setup

### Prerequisites
- **JDK 17** or higher.
- **Maven** 3.x.
- **MySQL Server**.
- **Ollama** (optional, for local AI features).

### 1. Database Setup
1. Create a MySQL database named `aiva`.
2. Update the connection details in `src/main/resources/hibernate.cfg.xml`:
   ```xml
   <property name="hibernate.connection.url">jdbc:mysql://127.0.0.1:3306/aiva</property>
   <property name="hibernate.connection.username">YOUR_USERNAME</property>
   <property name="hibernate.connection.password">YOUR_PASSWORD</property>
   ```

### 2. AI Setup (Ollama)
To use the local AI chatbot:
1. Install [Ollama](https://ollama.com/).
2. Run `ollama serve`.
3. Pull the required model: `ollama pull llama3`.

### 3. Build & Run
Navigate to the project root and run:
```bash
mvn clean javafx:run
```

---

## 📂 Project Structure

- `src/main/java/Models`: Database entities and business logic.
- `src/main/java/Controllers`: View logic and event handling.
- `src/main/java/utils`: Helper services (AI, DB, Email, Export).
- `src/main/resources/view`: FXML layouts for the UI.
- `src/main/resources/style.css`: Global styling and themes.

---

## 👤 Author
**Oussema Tarchoun** / PI-java-3A54

---
*Developed as part of an academic integration project.*
