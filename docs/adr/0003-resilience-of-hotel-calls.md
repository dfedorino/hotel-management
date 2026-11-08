# ADR 0003. Устойчивость вызовов Hotel Service: таймауты, повторы с backoff, circuit breaker

## Контекст

ТЗ требует таймаут и ограниченное число повторов с увеличением интервала для удалённых вызовов,
критерий 2 — Retry/Timeout/CircuitBreaker.

## Решение

- Таймауты OpenFeign: соединение 1 с, чтение 2 с (`spring.cloud.openfeign.client.config.default`).
- Resilience4j на адаптере `HotelServiceAdapter` (`@Retry`, `@CircuitBreaker`, экземпляр
  `hotel-service` в `application.yml`):
    - 3 попытки с паузами 200 и 400 мс — только для сетевых ошибок (`feign.RetryableException`) и
      5xx (`FeignServerException`);
    - 4xx (`HotelServiceClientException`) не повторяются и не учитываются circuit breaker'ом;
    - circuit breaker размыкается, если упала половина из последних 10 вызовов, и через 10 с
      пропускает пробные вызовы.
- Повторы безопасны: `confirm-availability` и `release` идемпотентны по `requestId` на стороне Hotel
  Service.
- Сага зависит от порта `HotelAvailability`, а не от Feign/Resilience4j (ArchUnit-правило
  `sagaSeesHotelServiceOnlyThroughItsPort`).

## Последствия

- Политика настраивается в `application.yml` без изменения кода.
- Тесты `HotelServiceAdapterIntegrationTest` (WireMock): повтор 503/500, отказ после трёх таймаутов,
  409 без повтора, размыкание circuit breaker.
