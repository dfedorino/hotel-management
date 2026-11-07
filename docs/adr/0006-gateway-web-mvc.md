# ADR 0006. Gateway на Spring Cloud Gateway Server Web MVC

## Контекст
Остальные сервисы — servlet-приложения. Вариант WebFlux требовал отдельной реактивной реализации безопасности и трассировки и не позволял переиспользовать starter.

## Решение
- `spring-cloud-starter-gateway-server-webmvc`, маршруты — функциональные `RouterFunction` (`RoutesConfig`), виртуальные потоки Java 21 для проксируемых запросов.
- Маршруты (пути до сервисов доходят без изменений):
  - `/api/bookings`, `/api/bookings/**` → `lb://booking-service`;
  - `/api/hotels`, `/api/hotels/**` → `lb://hotel-service`;
  - `/api/rooms`, `/api/rooms/{один сегмент}` → `lb://hotel-service` — внутренние `/api/rooms/{id}/confirm-availability` и `/release` не маршрутизируются (404).
- `lb://` разрешается через Eureka и Spring Cloud LoadBalancer.
- Безопасность — тот же starter, что у сервисов; `X-Trace-Id` в каждом ответе; ошибки самого Gateway (нет маршрута, сервис недоступен) — в формате ErrorResponse (`GatewayErrorAttributes`, `UnreachableServiceFilter`).

## Последствия
- Один стиль кода и тестов (MockMvc) во всех сервисах.
- Блокирующая модель «поток на запрос» компенсируется виртуальными потоками.
