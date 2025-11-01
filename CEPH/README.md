# Ceph RGW Integration Guide

Этот каталог содержит инфраструктурные артефакты и инструкции для развёртывания
отдельного кластера Ceph с включённым RADOS Gateway (RGW), который предоставляет
S3-совместимое API для backend-сервиса из директории `backend/`.

## Состав каталога

| Файл/папка | Назначение |
|-----------|------------|
| `docker-compose.ceph.yml` | Compose-стек для локального развёртывания all-in-one инсталляции Ceph (MON, MGR, OSD, RGW). |
| `.env.example` | Пример значений переменных окружения для запуска compose-стека. |
| `backend.env.example` | Пример переменных окружения backend-сервиса для подключения к RGW. |
| `scripts/create-rgw-user.sh` | Скрипт для создания S3-пользователя и бакета в Ceph RGW. |

> **Важно:** каталог `CEPH/` отделён от существующего backend-а. Он не изменяет
> текущее поведение приложения и не подключается автоматически. Ceph необходимо
> запускать отдельно, а затем указать параметры подключения в backend-е.

## Быстрый старт (локальная среда)

### 1. Подготовьте переменные окружения

1. Скопируйте файл `.env.example` в `.env` и подставьте значения:
   ```bash
   cp CEPH/.env.example CEPH/.env
   ```
2. Укажите IP-адрес хоста в `CEPH_MON_IP`. Он должен быть доступен другим
   контейнерам и сервисам (например, адрес интерфейса `docker0` либо IP вашей
   машины в локальной сети). Значение `CEPH_PUBLIC_NETWORK` должно покрывать этот
   адрес (CIDR).

### 2. Запустите кластер Ceph с RGW

```bash
cd CEPH
docker compose -f docker-compose.ceph.yml up -d
```

Команда поднимет контейнер `ceph-all-in-one`, в котором будут размещены MON,
MGR, один OSD и RADOS Gateway. RGW слушает порт, указанный в переменной
`CEPH_RGW_PORT` (по умолчанию `7480`).

Проверить состояние можно так:
```bash
docker compose -f docker-compose.ceph.yml logs -f ceph-all-in-one
```
После появления строки `cluster is now HEALTH_OK` можно переходить к созданию
пользователя.

### 3. Создайте S3-пользователя и бакет

1. Скопируйте скрипт параметров для создания пользователя:
   ```bash
   cp scripts/create-rgw-user.sh scripts/create-rgw-user.local.sh
   chmod +x scripts/create-rgw-user.local.sh
   ```
2. Отредактируйте локальную копию, задав значения переменных (имя пользователя,
   бакета и т.п.).
3. Выполните скрипт:
   ```bash
   ./scripts/create-rgw-user.local.sh
   ```
   Скрипт вызовет `radosgw-admin` внутри контейнера и выведет сгенерированные
   `AWS_ACCESS_KEY_ID` и `AWS_SECRET_ACCESS_KEY`. Сохраните их — они потребуются
   backend-у.

### 4. Настройте backend для работы с RGW

1. Скопируйте `backend.env.example` в файл окружения, который считывается вашим
   способом запуска backend-а (например, `.env` рядом с `backend/docker-compose.yml`).
   ```bash
   cp CEPH/backend.env.example backend/.env.ceph
   ```
2. Заполните переменные значениями из шага выше (`CEPH_ENDPOINT`, ключи и т.д.).
3. Убедитесь, что backend использует эти переменные для настройки S3-клиента
   (см. TODO в `S3FileStorage.java`).

### 5. Проверка работы

* С помощью AWS CLI:
  ```bash
  aws --endpoint-url http://<CEPH_MON_IP>:<CEPH_RGW_PORT> s3 ls
  ```
* Через REST API backend-а — выполнить операции загрузки и скачивания файла.

## Производственная инсталляция

`docker-compose.ceph.yml` предназначен для разработки. Для продакшн-среды
рекомендуется развернуть Ceph с помощью `cephadm` или Kubernetes-оператора.
Ниже приведён обзор шагов для `cephadm`:

1. Подготовьте хосты с дополнительными дисками под OSD.
2. На управляющей машине установите `cephadm` и выполните:
   ```bash
   cephadm bootstrap --mon-ip <ip-адрес> --initial-dashboard-user admin --initial-dashboard-password <пароль>
   ```
3. Добавьте хосты под OSD, MON, MGR и RGW:
   ```bash
   ceph orch host add <hostname> <ip>
   ceph orch daemon add osd <hostname>:<device>
   ceph orch apply rgw default.default --placement="count:2"
   ```
4. Создайте пользователя RGW и бакет по аналогии со скриптом `create-rgw-user.sh`.
5. Прокиньте endpoint, ключи и bucket-name в backend через переменные окружения
   или менеджер секретов.

## Очистка

```bash
cd CEPH
docker compose -f docker-compose.ceph.yml down -v
```

Это остановит контейнер и удалит привязанные к нему тома (данные кластера Ceph).
Если вы хотите сохранить данные между запусками, удалите флаг `-v`.

## Дополнительные материалы

* [Документация Ceph Orchestrator](https://docs.ceph.com/en/latest/cephadm/)
* [Ceph RADOS Gateway](https://docs.ceph.com/en/latest/radosgw/)
* [AWS SDK for Java v2](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html)
