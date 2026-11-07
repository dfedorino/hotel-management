# ADR 0005. Безопасность: HS256 JWT, проверка в каждом сервисе, сервисный токен

## Контекст
Каждый сервис должен проверять JWT сам (Resource Server), Gateway — маршрутизировать и передавать токен. Внутренние эндпойнты саги не должны быть доступны пользователям.

## Решение
- Токены выпускает Booking Service (`JwtTokenIssuer`, HS256, срок 1 час): `sub` — имя пользователя, `uid` — id, `roles` — роль.
- Общий секрет (`JWT_SECRET`) проверяют все: Hotel Service, Booking Service и Gateway. Код проверки вынесен в `jwt-resource-server-spring-boot-starter`: декодер HS256, `roles` → `ROLE_*`, ответы 401/403 в формате ErrorResponse, `@EnableMethodSecurity`, бин `JwtResourceServerCustomizer` для цепочки фильтров.
- Gateway проверяет только подлинность и срок токена (запрос без валидного токена не доходит до сервисов); роли проверяют сервисы через `@PreAuthorize`.
- Для `confirm-availability`/`release` Booking Service подписывает сервисный токен с ролью `INTERNAL` (срок 5 минут, кэшируется в `ServiceTokenCache`); эти эндпойнты доступны `INTERNAL` и `ADMIN` (ручные корректировки), а через Gateway не публикуются.
- CSRF выключен: единственные учётные данные — bearer-токен в заголовке, браузер не подставляет его сам, сессий нет.

Асимметричная подпись (RS256 + JWKS) и сервер авторизации (`OAuth2AccessTokenInterceptor`, client credentials) отклонены как избыточные для учебного проекта: ТЗ требует выпуск токенов Booking Service'ом.

## Последствия
- Секрет по умолчанию в `application.yml` годится только для локального запуска; вне его задаётся `JWT_SECRET`.
- Тесты `AccessControlIntegrationTest`, `AuthAndUsersIntegrationTest`, `GatewayRoutingIntegrationTest` проверяют 401 (нет, чужой, просроченный токен) и 403 по ролям.
