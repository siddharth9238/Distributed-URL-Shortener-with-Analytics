# Distributed URL Shortener with Analytics

A production-grade URL shortening service built with Spring Boot, MySQL, Redis, and advanced algorithms (Base62 encoding, LRU cache, token bucket rate limiting).

## Features

- **URL Shortening**: Convert long URLs into short, shareable codes
- **Custom Aliases**: Support for user-defined short codes
- **Authentication**: JWT-based user authentication
- **Rate Limiting**: Token bucket algorithm per-user rate limiting (60 URLs/minute)
- **LRU Cache**: In-memory caching layer for fast URL resolution
- **Redis Caching**: Distributed caching for scalability
- **Analytics**: Track clicks, referrers, geographic distribution, device types
- **Batch Processing**: Async analytics aggregation (every 30 minutes)
- **Expiration**: Set expiration dates on short URLs
- **Async Click Tracking**: Fire-and-forget analytics recording
- **Scheduled Cleanup**: Daily cleanup of expired URLs
- **Swagger/OpenAPI**: Auto-generated API documentation
- **Admin Dashboard**: Real-time system metrics and monitoring
- **CORS Support**: Frontend-friendly cross-origin configuration
- **Docker Support**: Full containerization with docker-compose

## Architecture

### Core Components

**Base62Encoder**
- Converts long numeric IDs to short alphanumeric codes
- Uses 0-9a-zA-Z character set (62 characters)
- Real number-base-conversion algorithm
- O(log n) encode/decode complexity

**LRUCache**
- Fixed-capacity cache with least-recently-used eviction
- HashMap for O(1) lookup + doubly-linked list for O(1) eviction
- Thread-safe with synchronized methods
- Configurable max capacity (default: 10,000 entries)

**RateLimiterService**
- Token bucket rate limiter per user
- Concurrent-safe using ConcurrentHashMap and AtomicLong
- Configurable tokens per minute (default: 60)
- Per-user bucket refills at fixed rate

**UrlService** (Orchestrator)
- Cache-aside pattern: cache hit → return, cache miss → DB → populate cache
- Handles URL creation, resolution, and click recording
- Validates URLs and checks for custom alias collisions
- Integrates with rate limiter and analytics

**AnalyticsService**
- Records clicks asynchronously (@Async)
- Aggregates statistics: clicks per day, top referrers, geographic distribution
- Extracts device type, country, referrer from request headers
- Supports time-range queries

**BatchAnalyticsService**
- Scheduled batch aggregation every 30 minutes
- Cleanup of old click events daily at 3 AM
- Generates daily analytics summaries

**AuthService**
- User registration and login
- Password hashing with BCrypt
- JWT token generation and validation

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and get JWT token

### URL Operations
- `POST /api/urls` - Create short URL (requires auth)
- `GET /api/urls/{code}` - Get URL details (requires auth)
- `DELETE /api/urls/{code}` - Delete short URL (requires auth, owner only)
- `GET /api/urls/user/all` - Get all URLs for user (requires auth)
- `GET /api/urls/redirect/{code}` - Redirect to original URL (public)

### Analytics
- `GET /api/analytics/{code}` - Get full analytics for a URL (requires auth)
- `GET /api/analytics/{code}/clicks` - Get recent clicks (requires auth)
- `GET /api/analytics/{code}/range` - Get clicks in time range (requires auth)

### Admin
- `GET /api/admin/dashboard` - Get system statistics (requires auth)
- `GET /api/admin/health` - Health check (public)
- `POST /api/admin/cache/clear` - Clear application caches (requires auth)

### Documentation
- `GET /swagger-ui.html` - Swagger UI (public)
- `GET /v3/api-docs` - OpenAPI specification (public)
- `GET /dashboard` - Admin dashboard (public, interactive)

## Setup & Configuration

### Prerequisites
- Java 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 7.0+ (optional, for distributed caching)
- Docker & Docker Compose (optional)

### Option 1: Local Development Setup

#### Database Setup

1. Create MySQL database:
```sql
CREATE DATABASE url_shortener CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Run schema migration:
```bash
mysql -u root -p url_shortener < src/main/resources/schema.sql
```

#### Redis Setup (Optional)

1. Install Redis locally or use Docker:
```bash
docker run -d -p 6379:6379 redis:7-alpine
```

#### Application Configuration

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/url_shortener?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: root
  redis:
    host: localhost
    port: 6379

app:
  jwt:
    secret: "your-secret-key-change-in-production"
  cache:
    max-size: 10000
  rate-limiter:
    tokens-per-minute: 60
```

#### Building & Running

```bash
# Build
mvn clean package

# Run
mvn spring-boot:run
```

### Option 2: Docker Compose (Recommended)

```bash
# Start all services (MySQL, Redis, App)
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down
```

The application will be available at `http://localhost:8080`

## Testing

### Unit Tests

```bash
mvn test
```

Test files cover:
- Base62Encoder encode/decode correctness
- LRUCache hit/miss/eviction
- RateLimiterService token bucket logic

### API Testing with cURL

#### 1. Register User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "securepass123",
    "fullName": "John Doe"
  }'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "userId": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "expiresIn": "1 day"
}
```

#### 2. Create Short URL
```bash
TOKEN="your_jwt_token_here"

curl -X POST http://localhost:8080/api/urls \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "longUrl": "https://www.example.com/very/long/url/path?query=value",
    "customAlias": "mylink",
    "description": "My awesome link",
    "category": "project"
  }'
```

Response:
```json
{
  "id": 1,
  "shortCode": "1",
  "longUrl": "https://www.example.com/very/long/url/path?query=value",
  "clickCount": 0,
  "createdAt": "2026-06-19T10:30:00",
  "description": "My awesome link",
  "category": "project"
}
```

#### 3. Redirect (Public)
```bash
# Browser or curl
curl -L http://localhost:8080/api/urls/redirect/1
```

#### 4. Get Analytics
```bash
curl http://localhost:8080/api/analytics/1 \
  -H "Authorization: Bearer $TOKEN"
```

Response:
```json
{
  "urlId": 1,
  "shortCode": "1",
  "totalClicks": 42,
  "topReferrers": {
    "twitter.com": 25,
    "facebook.com": 15,
    "direct": 2
  },
  "countryDistribution": {
    "US": 30,
    "UK": 8,
    "CA": 4
  },
  "deviceTypeDistribution": {
    "mobile": 28,
    "desktop": 12,
    "tablet": 2
  }
}
```

#### 5. Admin Dashboard Stats
```bash
curl http://localhost:8080/api/admin/dashboard \
  -H "Authorization: Bearer $TOKEN"
```

## Swagger/OpenAPI Documentation

Interactive API documentation available at:
```
http://localhost:8080/swagger-ui.html
```

All endpoints are documented with:
- Request/response examples
- Parameter descriptions
- Error codes and messages
- Authentication requirements

## Admin Dashboard

Interactive dashboard available at:
```
http://localhost:8080/dashboard
```

Features:
- Real-time system statistics
- Total users, URLs, and clicks
- Active vs expired URL counts
- Average clicks per URL
- Quick action buttons
- API endpoint reference

## Performance Characteristics

- **URL Resolution**: O(1) average case (cache hit), O(log n) worst case (base62 decode) + O(1) DB lookup (indexed)
- **Cache Eviction**: O(1) with LRU doubly-linked list
- **Rate Limiting**: O(1) per request with ConcurrentHashMap
- **Click Recording**: Async, non-blocking
- **Analytics Queries**: Indexed on url_id and timestamp

### Caching Strategy

1. **LRU In-Memory Cache** (10K entries)
   - Fast access to frequently used short codes
   - Automatic eviction of least-recently-used entries

2. **Redis Cache** (optional)
   - Distributed caching for multi-instance deployments
   - Session and token caching

3. **Database Indexes**
   - Short code lookup: O(1)
   - Owner ID queries: Indexed
   - Timestamp ranges: Indexed for analytics

## Scheduled Jobs

1. **Click Statistics Aggregation**
   - Frequency: Every 30 minutes
   - Purpose: Update click counts on URLs
   - Impact: Minimal, non-blocking

2. **Expired URL Cleanup**
   - Frequency: Daily at 3 AM
   - Purpose: Mark expired URLs as inactive
   - Impact: Soft delete, preserves analytics

## Deployment Considerations

### Production Environment

1. **Database**
   - Use managed MySQL service (AWS RDS, Google Cloud SQL)
   - Enable automated backups
   - Configure read replicas for high traffic
   - Connection pooling: 20-50 connections

2. **Caching**
   - Use managed Redis (ElastiCache, Cloud Memorystore)
   - Enable AOF persistence
   - Configure expiration policies

3. **Security**
   - Change JWT secret via environment variable
   - Enable HTTPS/TLS
   - Use environment variables for sensitive config
   - Implement rate limiting at CDN level

4. **Scaling**
   - Deploy multiple instances behind load balancer
   - Use Redis for distributed caching
   - Database read replicas for analytics queries
   - CDN for redirect endpoints

5. **Monitoring**
   - Enable CloudWatch/Stackdriver logging
   - Set up alerts for error rates
   - Monitor cache hit/miss ratios
   - Track database connection pool usage

### Docker Deployment

```bash
# Build image
docker build -t url-shortener:latest .

# Run with docker-compose
docker-compose -f docker-compose.yml up -d

# View logs
docker-compose logs -f app

# Health check
curl http://localhost:8080/api/admin/health
```

## Interview-Defensible Algorithms

This project demonstrates three key algorithmic concepts:

1. **Base62 Encoding** (Number Base Conversion)
   - Converts auto-increment IDs into short strings
   - Reversible encoding/decoding
   - Real-world application of base conversion

2. **LRU Cache** (LeetCode 146)
   - Fixed-capacity cache with optimal eviction policy
   - O(1) get, put, eviction via HashMap + doubly-linked list
   - Production-grade thread safety

3. **Token Bucket Rate Limiting** (Distributed Systems)
   - Per-user rate limiting with token refill
   - Handles burst vs sustained traffic
   - Thread-safe concurrent implementation

## Project Structure

```
url-shortener/
├── src/main/java/com/siddharth/urlshortener/
│   ├── controller/          # REST endpoints
│   ├── service/             # Business logic
│   ├── repository/          # Data access
│   ├── model/               # JPA entities
│   ├── security/            # JWT & auth
│   ├── config/              # Spring configuration
│   ├── dto/                 # Data transfer objects
│   ├── exception/           # Custom exceptions
│   ├── scheduler/           # Scheduled jobs
│   └── util/                # Utility classes
├── src/main/resources/
│   ├── application.yml      # Configuration
│   ├── schema.sql           # Database schema
│   └── dashboard.html       # Admin dashboard
├── src/test/java/           # Unit tests
├── pom.xml                  # Maven dependencies
├── Dockerfile               # Docker image
├── docker-compose.yml       # Container orchestration
└── README.md                # This file
```

## Troubleshooting

### Application fails to start
- Check MySQL is running: `mysql -u root -p`
- Check Redis is running (if enabled): `redis-cli ping`
- Check port 8080 is available
- View logs: `mvn spring-boot:run | tail -20`

### Database connection error
- Verify credentials in `application.yml`
- Ensure database exists: `CREATE DATABASE url_shortener;`
- Check MySQL service: `service mysql status`

### Redis connection error
- Ensure Redis is running: `redis-cli ping`
- Check Redis host/port in `application.yml`
- Use `docker run -d -p 6379:6379 redis:7-alpine`

### Rate limit not working
- Check `app.rate-limiter.tokens-per-minute` in config
- Verify user ID is being extracted from JWT
- Check Redis connection for distributed rate limiting

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Write tests
5. Submit a pull request

## License

MIT

## Contact

For questions or support, please open an issue on GitHub.

## Architecture

### Core Components

**Base62Encoder**
- Converts long numeric IDs to short alphanumeric codes
- Uses 0-9a-zA-Z character set (62 characters)
- Real number-base-conversion algorithm
- O(log n) encode/decode complexity

**LRUCache**
- Fixed-capacity cache with least-recently-used eviction
- HashMap for O(1) lookup + doubly-linked list for O(1) eviction
- Thread-safe with synchronized methods
- Configurable max capacity (default: 10,000 entries)

**RateLimiterService**
- Token bucket rate limiter per user
- Concurrent-safe using ConcurrentHashMap and AtomicLong
- Configurable tokens per minute (default: 60)
- Per-user bucket refills at fixed rate

**UrlService** (Orchestrator)
- Cache-aside pattern: cache hit → return, cache miss → DB → populate cache
- Handles URL creation, resolution, and click recording
- Validates URLs and checks for custom alias collisions
- Integrates with rate limiter and analytics

**AnalyticsService**
- Records clicks asynchronously (@Async)
- Aggregates statistics: clicks per day, top referrers, geographic distribution
- Extracts device type, country, referrer from request headers
- Supports time-range queries

**AuthService**
- User registration and login
- Password hashing with BCrypt
- JWT token generation and validation

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and get JWT token

### URL Operations
- `POST /api/urls` - Create short URL (requires auth)
- `GET /api/urls/{code}` - Get URL details (requires auth)
- `DELETE /api/urls/{code}` - Delete short URL (requires auth, owner only)
- `GET /api/urls/user/all` - Get all URLs for user (requires auth)
- `GET /api/urls/redirect/{code}` - Redirect to original URL (public)

### Analytics
- `GET /api/analytics/{code}` - Get full analytics for a URL (requires auth)
- `GET /api/analytics/{code}/clicks` - Get recent clicks (requires auth)
- `GET /api/analytics/{code}/range` - Get clicks in time range (requires auth)

## Setup & Configuration

### Prerequisites
- Java 17+
- Maven 3.6+
- MySQL 8.0+

### Database Setup

1. Create MySQL database:
```sql
CREATE DATABASE url_shortener CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Run schema migration:
```bash
mysql -u root -p url_shortener < src/main/resources/schema.sql
```

Or from MySQL CLI:
```sql
USE url_shortener;
SOURCE src/main/resources/schema.sql;
```

### Application Configuration

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/url_shortener?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: root

app:
  jwt:
    secret: "your-secret-key-change-in-production"
  cache:
    max-size: 10000
  rate-limiter:
    tokens-per-minute: 60
```

### Building & Running

```bash
# Build
mvn clean package

# Run
mvn spring-boot:run

# Or run the JAR
java -jar target/url-shortener-1.0.0.jar
```

The application will start on `http://localhost:8080`.

## Testing

Run unit tests:
```bash
mvn test
```

Test files cover:
- Base62Encoder encode/decode correctness
- LRUCache hit/miss/eviction
- RateLimiterService token bucket logic

## Request/Response Examples

### Register User
```bash
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "securepass123",
  "fullName": "John Doe"
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "userId": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "expiresIn": "1 day"
}
```

### Create Short URL
```bash
POST /api/urls
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
Content-Type: application/json

{
  "longUrl": "https://www.example.com/very/long/url/path?query=value",
  "customAlias": "mylink",
  "description": "My awesome link",
  "category": "project",
  "expiresAt": "2026-12-31T23:59:59"
}
```

Response:
```json
{
  "id": 42,
  "shortCode": "B5",
  "longUrl": "https://www.example.com/very/long/url/path?query=value",
  "clickCount": 0,
  "createdAt": "2026-06-19T10:30:00",
  "expiresAt": "2026-12-31T23:59:59",
  "description": "My awesome link",
  "category": "project"
}
```

### Get Analytics
```bash
GET /api/analytics/B5
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

Response:
```json
{
  "urlId": 42,
  "shortCode": "B5",
  "totalClicks": 150,
  "topReferrers": {
    "twitter.com": 75,
    "facebook.com": 45,
    "direct": 30
  },
  "countryDistribution": {
    "US": 85,
    "UK": 35,
    "CA": 30
  },
  "deviceTypeDistribution": {
    "mobile": 90,
    "desktop": 55,
    "tablet": 5
  }
}
```

## Performance Characteristics

- **URL Resolution**: O(1) average case (cache hit), O(log n) worst case (base62 decode) + O(1) DB lookup (indexed)
- **Cache Eviction**: O(1) with LRU doubly-linked list
- **Rate Limiting**: O(1) per request with ConcurrentHashMap
- **Click Recording**: Async, non-blocking
- **Analytics Queries**: Indexed on url_id and timestamp

## Deployment Considerations

1. **Connection Pooling**: HikariCP configured for 20 max connections
2. **JWT Secret**: Change in production via environment variable
3. **CORS**: Configure as needed for frontend
4. **SSL/TLS**: Enable in production
5. **Logging**: Set to DEBUG for development, INFO for production
6. **Database Backups**: Regular backups of click_events and urls tables

## Interview-Defensible Algorithms

This project demonstrates three key algorithmic concepts:

1. **Base62 Encoding** (Number Base Conversion)
   - Converts auto-increment IDs into short strings
   - Reversible encoding/decoding
   - Real-world application of base conversion

2. **LRU Cache** (LeetCode 146)
   - Fixed-capacity cache with optimal eviction policy
   - O(1) get, put, eviction via HashMap + doubly-linked list
   - Production-grade thread safety

3. **Token Bucket Rate Limiting** (Distributed Systems)
   - Per-user rate limiting with token refill
   - Handles burst vs sustained traffic
   - Thread-safe concurrent implementation

## License

MIT
