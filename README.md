# quant

Spring Boot quant service with:
- Spring Web
- Spring Data JPA + MySQL
- Spring Data Redis (cache)
- Spring HTTP Interface (`@HttpExchange`) for outbound HTTP

## Local run

Requirements:
- JDK 21
- Maven 3.9+
- Podman + podman-compose provider

Build:

```bash
mvn clean package
```

Run app only:

```bash
mvn spring-boot:run
```

## Deploy with Podman Compose

```bash
mvn clean package
podman compose -f compose.yml up -d --build
```

Stop:

```bash
podman compose -f compose.yml down
```

## APIs

Create strategy:

```bash
curl -X POST 'http://localhost:8080/api/strategies' \
  -H 'Content-Type: application/json' \
  -d '{"name":"mean-reversion","description":"sample strategy"}'
```

List strategies:

```bash
curl 'http://localhost:8080/api/strategies'
```

Query market price via HTTP Interface:

```bash
curl 'http://localhost:8080/api/market/BTCUSDT'
```
