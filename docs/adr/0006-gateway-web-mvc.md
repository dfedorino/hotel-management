# ADR 0006. Gateway на Spring Cloud Gateway Server Web MVC

## Контекст

Т.к. сервисы — это servlet-приложения, был выбран Spring Cloud Gateway Server Web MVC.

## Решение

- `spring-cloud-starter-gateway-server-webmvc`, маршруты — функциональные `RouterFunction`
  (`RoutesConfig`), виртуальные потоки Java 21 для проксируемых запросов.
- Маршруты (пути до сервисов доходят без изменений):
    - `/api/bookings`, `/api/bookings/**` → `lb://booking-service`;
    - `/api/hotels`, `/api/hotels/**` → `lb://hotel-service`;
    - `/api/rooms`, `/api/rooms/{один сегмент}` → `lb://hotel-service` — внутренние
      `/api/rooms/{id}/confirm-availability` и `/release` не маршрутизируются (404).
- `lb://` разрешается через Eureka и Spring Cloud LoadBalancer.
- Безопасность — тот же starter, что у сервисов; `X-Trace-Id` в каждом ответе; ошибки самого Gateway
  (нет маршрута, сервис недоступен) — в формате ErrorResponse (`GatewayErrorAttributes`,
  `UnreachableServiceFilter`).

## Последствия

- Блокирующая модель «поток на запрос» компенсируется виртуальными потоками.
