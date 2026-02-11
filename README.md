# quant

Multi-module Spring Boot quant service:
- `quant-model`: entities/dto/enums/constants
- `quant-persistence`: mapper layer
- `quant-market`: market data sync services
- `quant-factor`: factor layer (scaffold)
- `quant-backtest`: backtest layer (scaffold)
- `quant-app`: Spring Boot entry module

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
mvn -pl quant-app -am spring-boot:run
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

AKShare HTTP (AKTools) service will be exposed on `http://localhost:18080`.
Quick check:

```bash
curl 'http://localhost:18080/api/public/stock_zh_a_hist?symbol=600519&period=daily&start_date=20260101&end_date=20260210&adjust='
```

## APIs

Sync all A-share stock codes from xueqiu (paged + throttled + retried):

```bash
curl -X POST 'http://localhost:8080/api/sync/stock-codes'
```

Proxy AKShare daily quotes via this service:

```bash
curl 'http://localhost:8080/api/akshare/stock-zh-a-hist?symbol=600519&startDate=20260101&endDate=20260210&adjust='
```

Sync config keys:
- `quant.sync.stock-code.page-size`
- `quant.sync.stock-code.throttle-ms`
- `quant.sync.stock-code.max-retries`
- `quant.sync.stock-code.retry-backoff-ms`
- `quant.sync.stock-code.max-pages`
