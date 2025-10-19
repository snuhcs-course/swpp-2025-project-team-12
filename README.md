# **Iteration 2 Demo of Daily Insight**

## 1. Implemented Features

| Area | Description |
|:--|:--|
| **Backend (Django on AWS EC2)** | Daily article crawler, market index crawler, stock data crawler, JWT-based authentication, user interest management |
| **Frontend (Android Studio - Kotlin)** | Splash → Sign in / Sign up → Home (Recommendation List) → Detail → History → Index → Profile UI implemented |
| **Integration** | Backend REST API connected to Android client for login display |

---

## 2. How to Set Environment

### 2.1 Clone Repository

```bash
git clone https://github.com/swsnu/swpp-2025-project-team-12.git
cd swpp-2025-project-team-12
git checkout iteration-2-demo
```

Project structure overview:

```
swpp-2025-project-team-12/
 ├── MnA_BE/      # Backend (Django)
 ├── MnA_FE/      # Frontend (Android Studio Project)
 └── [other project docs]
```

---

### 2.2 Backend Setup (Django)

1. Create a virtual environment and activate it:

```bash
cd MnA_BE
conda create -n dailyinsight python=3.10
conda activate dailyinsight
```

2. Install dependencies:

```bash
pip install -r requirements.txt
```

3. Run the development server (locally or connect to AWS EC2):

```bash
python manage.py runserver 0.0.0.0:8000
```

> Make sure the server is running before launching the Android app.

---

### 2.3 Frontend Setup (Android Studio – Kotlin)

1. Open Android Studio → **Open an existing project**  
 Select: `MnA_FE/`

2. Wait until Gradle build completes and dependencies are downloaded.

3. Select a virtual device (Emulator) or real device and click ▶ **Run App**.

---

## 3. How to Run Demo

### 3.1 Backend Start
```bash
conda activate dailyinsight
cd MnA_BE
python manage.py runserver
```

Keep the server running in the terminal.

### 3.2 Frontend Launch
- Open Android Studio  
- Ensure the emulator or real device is connected  
- Press **Run App** (▶)  

The app will launch starting from the **Splash** screen → **Sign in / Sign up** → **Home (Recommendation List)**.  

---

## 4. Demo Features Overview

| Feature | Description |
|:--|:--|
| **Splash Screen** | Checks token validity, redirects to Sign in if expired. |
| **Sign in / Sign up** | User authentication linked with backend. |
| **Home Screen** | Displays daily recommendations (general or personalized). |
| **Detail View** | Shows financial data and LLM-generated reasoning. |
| **History Tab** | Shows previous recommendations with timestamps. |
| **Index Information** | Displays KOSPI/KOSDAQ with trend summary. |

---

## 5. Demo Video

Demo video  
https://youtube.com/shorts/MVdy5fdfMh8

---