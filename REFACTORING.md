# Идеи рефакторинга

1. ~~Убрать ResponseEntity при генерации DTO и API в openapi-generator maven plugin~~ — сделано: `useResponseEntity=false`, коды 201/204 задаёт сгенерированный `@ResponseStatus`.
2. ~~Сделать сборку модулей независимой, чтобы при пуше изменений в hotel-service не собирались все модули~~ — сделано: `build.yml` собирает только изменённые модули (`-pl <модули> -am -amd`, список вычисляет `.github/scripts/changed-modules.sh`); при изменении корневого pom, `config/`, wrapper или CI собирается всё. Полный анализ SonarCloud — в `sonar.yml` ежедневно и вручную.
