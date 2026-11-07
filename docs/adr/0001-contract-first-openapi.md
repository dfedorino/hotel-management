# ADR 0001. Contract-first: OpenAPI-спецификация как источник API

## Контекст
Booking Service вызывает Hotel Service, у каждого сервиса свой REST API и своя документация (Swagger обязателен). Если писать контроллеры, DTO и Feign-клиенты вручную, контракт, сервер и клиент со временем расходятся, а Swagger, собранный по коду, показывает то, что получилось, а не то, что задумано.

## Решение
- Спецификации лежат в модулях-контрактах: `hotel-service-api/src/main/resources/openapi/hotel-service.yaml`, `booking-service-api/src/main/resources/openapi/booking-service.yaml`.
- `openapi-generator-maven-plugin` (генератор `spring`, `useResponseEntity=false`) генерирует модели и интерфейсы `*Api`; контроллеры их реализуют (`implements HotelsApi` и т. д.) — расхождение с контрактом ломает компиляцию.
- Для `hotel-service-api` дополнительно генерируются Feign-клиенты `*ApiClient` (library `spring-cloud`, собственный шаблон: имя сервиса `hotel-service` для Eureka) и подключается `HotelServiceErrorDecoder` (4xx → типизированные исключения).
- `booking-service-api` содержит только контракт и серверную часть — потребителей у Booking Service нет.
- Swagger UI показывает исходный файл контракта (`/openapi/*.yaml`), а не спецификацию, собранную springdoc по коду; доступ к документации отключается переменной `API_DOCS_ENABLED=false`.

## Последствия
- Изменение API начинается с правки YAML; сервер и клиент обновляются генерацией.
- ArchUnit-тест `controllersImplementTheGeneratedContract` запрещает контроллеры мимо контракта.
- Сгенерированный код исключён из Checkstyle/PMD/SpotBugs/JaCoCo/Sonar.
