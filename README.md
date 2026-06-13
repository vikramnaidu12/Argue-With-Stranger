<div align="center">

# ⚔️ Argue With Stranger

### *Challenge a stranger. Defend your stance. Win the crowd.*

An AI-powered real-time debate platform where users argue their opinions live,  
get judged by an AI, and earn their reputation through debate.

[![Live Demo](https://img.shields.io/badge/🌐_Live_Demo-argue--with--stranger-0B0D0E?style=for-the-badge&logo=railway&logoColor=white)](https://argue-with-stranger-production.up.railway.app)
[![GitHub](https://img.shields.io/badge/GitHub-vikramnaidu12-181717?style=for-the-badge&logo=github)](https://github.com/vikramnaidu12)

</div>

---

## 💡 What is this?

Most social platforms reward agreement. **Argue With Stranger rewards conviction.**

Pick a topic → Get matched with a stranger → Argue your side in real-time → **AI summarizes the debate** → Let the crowd vote on who won.

---

## ✨ Features

| Feature | Description |
|---|---|
| 🔐 **JWT Authentication** | Secure register/login with BCrypt + token-based auth |
| 💬 **Real-time Debate Chat** | WebSocket-powered live messaging between opponents |
| 🤖 **AI Debate Summary** | Gemini AI analyzes and summarizes each debate |
| 🏛️ **Topic-based Debates** | Browse, search, and join debates by topic |
| 🗳️ **Audience Voting** | Crowd votes on who made the stronger argument |
| 👤 **User Profiles** | Track your debate history and win rate |
| 📊 **Debate Tracking** | Full history of all debates and outcomes |
| ☁️ **Railway Deployment** | Live, always-on production environment |

---

## 🛠 Tech Stack

### Backend
![Java](https://img.shields.io/badge/Java-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat&logo=spring-boot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat&logo=springsecurity&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=flat&logo=JSON%20web%20tokens&logoColor=white)
![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-010101?style=flat)
![Gemini AI](https://img.shields.io/badge/Gemini_AI-4285F4?style=flat&logo=google&logoColor=white)

### Frontend
![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=flat&logo=html5&logoColor=white)
![CSS3](https://img.shields.io/badge/CSS3-1572B6?style=flat&logo=css3&logoColor=white)
![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=flat&logo=javascript&logoColor=black)

### Database & Deployment
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat&logo=mysql&logoColor=white)
![Railway](https://img.shields.io/badge/Railway-0B0D0E?style=flat&logo=railway&logoColor=white)

---

## 🚀 Project Structure

```
src/main/java/com/arguewithstranger/
│
├── ai/                          # 🤖 Gemini AI Integration
│   ├── AIController.java
│   ├── AIService.java
│   ├── GeminiService.java
│   └── PromptBuilder.java
│
├── config/                      # ⚙️ App Configuration
│   ├── CorsConfig.java
│   ├── SecurityConfig.java
│   └── WebSocketConfig.java
│
├── controller/                  # 🔌 REST Controllers
│   ├── AuthController.java
│   ├── ChatWebSocketController.java
│   ├── DebateController.java
│   ├── MessageController.java
│   ├── UserController.java
│   └── VoteController.java
│
├── service/                     # 🧠 Business Logic
│   ├── AuthService.java
│   ├── DebateService.java
│   ├── MessageService.java
│   ├── UserService.java
│   └── VoteService.java
│
├── entity/                      # 🗄️ Database Models
│   ├── Debate.java
│   ├── Message.java
│   ├── User.java
│   └── Vote.java
│
├── security/                    # 🔐 JWT Security
│   ├── JwtAuthFilter.java
│   ├── JwtUtil.java
│   └── UserDetailsServiceImpl.java
│
└── exception/                   # 🚨 Error Handling
    ├── GlobalExceptionHandler.java
    ├── AuthException.java
    ├── DebateException.java
    └── ...

src/main/resources/static/
├── css/style.css
├── js/
│   ├── api.js          # Centralized HTTP client
│   ├── auth.js         # Login / Register logic
│   ├── debateRoom.js   # Live debate UI
│   ├── debates.js      # Debate listing
│   └── profile.js      # User profile
├── index.html
├── login.html
├── register.html
├── debate.html
└── profile.html
```

---

## ⚙️ Running Locally

### Prerequisites
- Java 17+
- Maven
- MySQL

### Steps

```bash
# 1. Clone the repo
git clone https://github.com/vikramnaidu12/Argue-With-Stranger.git
cd Argue-With-Stranger

# 2. Create the database
mysql -u root -p
CREATE DATABASE arguewithstranger;

# 3. Set up application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/arguewithstranger
spring.datasource.username=your_username
spring.datasource.password=your_password
jwt.secret=your_jwt_secret
gemini.api.key=your_gemini_api_key

# 4. Run the app
./mvnw spring-boot:run
```

App runs at → `http://localhost:8080`

---

## 🔌 API Overview

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/auth/register` | ❌ | Register new user |
| `POST` | `/auth/login` | ❌ | Login and get JWT |
| `GET` | `/debates` | ✅ | List all debates |
| `POST` | `/debates` | ✅ Admin | Create debate topic |
| `POST` | `/debates/{id}/join` | ✅ | Join a debate |
| `POST` | `/debates/{id}/vote` | ✅ | Cast your vote |
| `GET` | `/debates/{id}/result` | ✅ | Get debate result |
| `GET` | `/users/profile` | ✅ | Your profile |
| `GET` | `/users/leaderboard` | ✅ | Top debaters |
| `POST` | `/ai/summarize` | ✅ | AI debate summary |
| `WS` | `/chat/**` | ✅ | Live debate room |

---

## 🎯 Roadmap

- [x] JWT Authentication & Security
- [x] Real-time WebSocket debate chat
- [x] Gemini AI debate summarization
- [x] Audience voting system
- [x] User profiles & debate history
- [x] Global exception handling
- [x] Railway cloud deployment
- [ ] 🏆 Global Leaderboard
- [ ] 🎖️ Achievement Badges
- [ ] 📱 Fully Responsive Mobile UI
- [ ] 📈 Analytics Dashboard
- [ ] 🤖 AI auto-judge with scoring

---

## 👨‍💻 Author

**Vikram Naidu** — Final Year CS Student | Backend Developer

[![GitHub](https://img.shields.io/badge/GitHub-vikramnaidu12-181717?style=for-the-badge&logo=github)](https://github.com/vikramnaidu12)

---

<div align="center">
  Built with ☕ Java, 🤖 Gemini AI, and the will to argue about everything.
</div>
