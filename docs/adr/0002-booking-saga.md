# ADR 0002. Сага бронирования: оркестрация в Booking Service на локальных транзакциях

## Контекст
Бронирование затрагивает две базы (Booking Service и Hotel Service). Глобальные распределённые транзакции по ТЗ запрещены; при ошибке или таймауте Hotel Service нужна компенсация.

## Решение
Оркестрирующая сага в Booking Service, каждый шаг — отдельная локальная транзакция (`BookingStore`, `@Transactional`):
1. `createPending` — бронирование фиксируется в статусе PENDING до обращения к Hotel Service;
2. `BookingSaga.holdSlot` вызывает `POST /api/rooms/{id}/confirm-availability`;
3. успех → `markConfirmed` (CONFIRMED); отказ Hotel Service (4xx) → `markCancelled` без компенсации (слот не занят); нет ответа после повторов / открыт circuit breaker → компенсация `POST /api/rooms/{id}/release` и `markCancelled`, ответ 503.

Компенсация выполняется и при таймауте, потому что ответ мог потеряться уже после того, как слот был занят; `release` идемпотентен, поэтому лишний вызов безопасен. Сбой самой компенсации логируется (`compensation failed`), бронирование всё равно отменяется.

Готовые оркестраторы (Axon, Eventuate, Temporal, Camunda) не используются: им нужна своя инфраструктура (брокер, event store, сервер), что избыточно для двух шагов.

## Последствия
- Переходы PENDING → CONFIRMED / CANCELLED и CONFIRMED → CANCELLED (отмена) закреплены в `Booking.confirm()/cancel()`.
- Все шаги логируются с `bookingId` в MDC и `requestId` в сообщении.
- Тесты: `BookingServiceImplTest` (шаги и компенсация), `BookingFlowIntegrationTest` (WireMock: успех, 409, таймаут, повтор запроса).
