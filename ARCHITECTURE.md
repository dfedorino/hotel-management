# Архитектура hotel-management

## 1. Архитектура компонентов

```mermaid
flowchart LR
    Client([Клиент<br/>USER / ADMIN])

    subgraph Infra[Инфраструктура]
        GW["API Gateway<br/>(Spring Cloud Gateway)<br/>/api/bookings/** , /api/hotels/**"]
        EU["Eureka Server<br/>(Service Registry)"]
    end

    subgraph HS[Hotel Management Service]
        HC[Controllers<br/>/api/hotels, /api/rooms]
        HSV[Services<br/>CRUD, статистика, блокировка слота]
        HR[Repositories]
        HDB[(H2<br/>hotels, rooms)]
        HC --> HSV --> HR --> HDB
    end

    subgraph BS[Booking Service]
        BC[Controllers<br/>/user, /booking, /bookings]
        BSV[Services<br/>бронирование, saga, подбор номера]
        BR[Repositories]
        BDB[(H2<br/>users, bookings)]
        BC --> BSV --> BR --> BDB
    end

    Client -- "HTTP + JWT" --> GW
    GW -- "/api/bookings/**" --> BC
    GW -- "/api/hotels/**" --> HC

    GW -. регистрация / discovery .-> EU
    BS -. регистрация .-> EU
    HS -. регистрация .-> EU

    BSV -- "REST: confirm-availability / release<br/>(timeout + retry + backoff, INTERNAL)" --> HC

    BS -. "Resource Server: проверка JWT" .- BS
    HS -. "Resource Server: проверка JWT" .- HS
```

## 2. Сага создания бронирования (PENDING → CONFIRMED / CANCELLED)

```mermaid
sequenceDiagram
    autonumber
    actor U as USER
    participant GW as API Gateway
    participant B as Booking Service
    participant BDB as Booking H2
    participant H as Hotel Service
    participant HDB as Hotel H2

    U->>GW: POST /booking (JWT, autoSelect, dates)
    GW->>B: маршрутизация + JWT
    B->>B: проверка JWT, валидация дат
    B->>BDB: локальная TX: INSERT booking (PENDING)
    B->>H: POST /api/rooms/{id}/confirm-availability<br/>(requestId, bookingId)

    alt Успех
        H->>HDB: блокировка слота, times_booked++
        H-->>B: 200 OK
        B->>BDB: статус CONFIRMED
        B-->>U: 201 Created
    else Ошибка / timeout (после retry с backoff)
        H--xB: ошибка / timeout
        B->>H: POST /api/rooms/{id}/release (requestId)
        H->>HDB: снять блокировку (идемпотентно)
        B->>BDB: статус CANCELLED
        B-->>U: ошибка (409 / 5xx)
    else Конфликт дат
        H-->>B: 409 Conflict
        B->>BDB: статус CANCELLED
        B-->>U: 409 Conflict
    end
```

## 3. Автоподбор номера (алгоритм занятости)

```mermaid
flowchart TD
    A[POST /booking] --> B{autoSelect?}
    B -- false --> C[Использовать roomId из запроса]
    B -- true --> D["GET /api/rooms/recommend<br/>свободные номера"]
    D --> E["Сортировка:<br/>times_booked ASC, затем id ASC"]
    E --> F[Взять первый номер]
    C --> G[Создать PENDING + confirm-availability]
    F --> G
    G --> H{Подтверждено?}
    H -- да --> I[CONFIRMED<br/>times_booked++]
    H -- нет --> J[CANCELLED + release]
```

## 4. Модель данных

```mermaid
erDiagram
    USERS ||--o{ BOOKINGS : "делает"
    HOTELS ||--o{ ROOMS : "содержит"
    ROOMS ||--o{ BOOKINGS : "room_id (логическая связь между сервисами)"

    USERS {
        long id PK
        string username
        string password
        string role "USER / ADMIN"
    }
    BOOKINGS {
        long id PK
        long user_id FK
        long room_id
        date start_date
        date end_date
        string status "PENDING / CONFIRMED / CANCELLED"
        timestamp created_at
    }
    HOTELS {
        long id PK
        string name
        string address
    }
    ROOMS {
        long id PK
        long hotel_id FK
        string number
        boolean available
        int times_booked
    }
```

## 5. Состояния бронирования

```mermaid
stateDiagram-v2
    [*] --> PENDING: создание (локальная TX)
    PENDING --> CONFIRMED: Hotel подтвердил доступность
    PENDING --> CANCELLED: ошибка / timeout / 409 → компенсация (release)
    CONFIRMED --> CANCELLED: DELETE /booking/{id}
    CONFIRMED --> [*]
    CANCELLED --> [*]
```

Примечания:
- Связь `ROOMS ↔ BOOKINGS` логическая: таблицы лежат в разных сервисах и разных H2, внешнего ключа между ними нет.
- Переход `CONFIRMED → CANCELLED` выведен из `DELETE /booking/{id}`; в ТЗ он явно не описан.
