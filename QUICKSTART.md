# Quick Start Guide

## 🚀 Get Started in 5 Minutes

### Option 1: Docker Compose (Easiest)

```bash
# Navigate to project directory
cd "Distributed URL Shortener with Analytics"

# Start all services
docker-compose up -d

# Wait for services to be healthy (30 seconds)
sleep 30

# Open in browser
# API: http://localhost:8080
# Dashboard: http://localhost:8080/dashboard
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### Option 2: Local Development

#### Prerequisites
```bash
# Install Java 17+
java -version

# Install Maven
mvn -version

# Install MySQL
mysql --version

# (Optional) Install Redis
redis-cli --version
```

#### Setup Database

```bash
# Create database
mysql -u root -p -e "CREATE DATABASE url_shortener CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Import schema
mysql -u root -p url_shortener < src/main/resources/schema.sql
```

#### Build & Run

```bash
# Build project
mvn clean package

# Run application
mvn spring-boot:run

# Application will be available at http://localhost:8080
```

---

## 📝 First API Call

### 1. Register User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "fullName": "Test User"
  }'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "userId": 1,
  "username": "testuser",
  "email": "test@example.com",
  "expiresIn": "1 day"
}
```

### 2. Save Token

```bash
export TOKEN="eyJhbGciOiJIUzUxMiJ9..."
```

### 3. Create Short URL

```bash
curl -X POST http://localhost:8080/api/urls \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "longUrl": "https://github.com/torvalds/linux",
    "customAlias": "linux",
    "description": "Linux kernel repository"
  }'
```

Response:
```json
{
  "id": 1,
  "shortCode": "1",
  "longUrl": "https://github.com/torvalds/linux",
  "clickCount": 0,
  "createdAt": "2026-06-19T12:00:00",
  "description": "Linux kernel repository"
}
```

### 4. Test Redirect (Public)

```bash
curl -L http://localhost:8080/api/urls/redirect/1
# Will redirect to https://github.com/torvalds/linux
```

### 5. Get Analytics

```bash
curl http://localhost:8080/api/analytics/1 \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🎯 Key Features to Try

### View All Your URLs
```bash
curl http://localhost:8080/api/urls/user/all \
  -H "Authorization: Bearer $TOKEN"
```

### Get Admin Dashboard
```bash
curl http://localhost:8080/api/admin/dashboard \
  -H "Authorization: Bearer $TOKEN"
```

### View API Documentation
```
http://localhost:8080/swagger-ui.html
```

### View Interactive Dashboard
```
http://localhost:8080/dashboard
```

---

## 🔍 Explore the Application

### Database
Access MySQL directly:
```bash
mysql -u root -p url_shortener
SELECT * FROM users;
SELECT * FROM urls;
SELECT COUNT(*) FROM click_events;
```

### Redis Cache
Access Redis CLI:
```bash
redis-cli
KEYS *
GET shortcode:1
```

### Logs
View application logs:
```bash
# Docker
docker-compose logs -f app

# Or Maven
# See console output
```

---

## ⚙️ Configuration

### Change Rate Limit

Edit `src/main/resources/application.yml`:
```yaml
app:
  rate-limiter:
    tokens-per-minute: 120  # Changed from 60
```

### Change Cache Size

```yaml
app:
  cache:
    max-size: 20000  # Changed from 10000
```

### Change JWT Secret

```yaml
app:
  jwt:
    secret: "your-new-secret-key-here"
```

---

## 🐛 Troubleshooting

### Docker Issues

```bash
# View logs
docker-compose logs

# Restart services
docker-compose restart

# Clear everything
docker-compose down -v
docker-compose up -d
```

### MySQL Connection Error

```bash
# Check MySQL is running
docker exec url-shortener-mysql mysql -u root -proot -e "SELECT 1"

# Or locally
mysql -u root -p -e "SELECT 1"
```

### Redis Connection Error

```bash
# Check Redis is running
docker exec url-shortener-redis redis-cli ping

# Or locally
redis-cli ping
```

### Build Errors

```bash
# Clean cache
mvn clean

# Rebuild
mvn package

# Check Java version
java -version  # Should be 17+
```

---

## 📚 Documentation

- **Full README**: [README.md](README.md)
- **Enhancements**: [ENHANCEMENTS.md](ENHANCEMENTS.md)
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs

---

## 🎓 Learning Resources

### Interview Preparation
- **Base62 Encoder**: See `src/main/java/.../service/Base62Encoder.java`
- **LRU Cache**: See `src/main/java/.../service/LRUCache.java`
- **Rate Limiter**: See `src/main/java/.../service/RateLimiterService.java`

### Unit Tests
```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=Base62EncoderTest
```

---

## 📊 Performance Tips

1. **Enable Redis** for distributed caching
2. **Use database indexes** for analytics queries
3. **Monitor batch jobs** for performance
4. **Scale horizontally** with load balancer

---

## 🚀 Next Steps

1. ✅ Test all API endpoints
2. ✅ Review the code
3. ✅ Deploy to production
4. ✅ Add monitoring
5. ✅ Scale as needed

---

## 💡 Tips

- **Save token** in environment variable for easier testing
- **Use Postman/Insomnia** for API testing
- **Check logs** when something goes wrong
- **Read ENHANCEMENTS.md** for all new features
- **Read README.md** for detailed documentation

---

**Happy URL shortening! 🎉**
