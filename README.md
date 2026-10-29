# AuthVk

Авторизация через VK ID с обменом токена в отдельной ключнице. Ключница возвращает:

```json
{
  "sessionId": "s....",
  "sessionSecret": "...",
  "personalEncryptionKey": "...",
  "personalEncryptionKeyVersion": 1,
  "name": "Анна"
}
```

`AuthVk` передаёт персональный ключ в `PersonalCrypto`, не публикуя его через
`AuthInterface`. Сессия считается успешно сохранённой только после сохранения
ключа. При частичном сбое и при logout обе локальные части очищаются.

## Настройка адресов

В `local.properties` основного проекта задаются два адреса, обязательно с `/`
в конце:

```properties
KEY_SERVICE_BASE_URL=https://keys.example.com/
AUTH_API_BASE_URL=https://auth.example.com/
```

Для совместимости во время перехода обе настройки используют старый
`API_BASE_URL`, если отдельное значение отсутствует.

Запрос входа уходит в ключницу, а подписанный logout — непосредственно в Auth.
Тело и заголовки ответа ключницы не записываются в HTTP-лог.

## Создание ViewModel

```kotlin
val vm: VkAuthViewModel = viewModel(
    factory = VkAuthViewModelFactory(
        context = app.applicationContext,
        auth = app.auth,
        personalCrypto = app.personalCrypto,
        api = app.authApi,
        diagnostics = app.diagnostics
    )
)
```

`app.personalCrypto` создаётся в composition root приложения:

```kotlin
val personalCrypto: PersonalCrypto = AndroidPersonalCrypto(applicationContext)
```

Сначала нужно опубликовать `pro.progr:personal-crypto:0.0.1-alpha` и
`pro.progr:diagnostics:0.1.0-alpha`, затем собирать и публиковать
`pro.progr:auth-vk:1.1.2-alpha`.

`Diagnostics` передаётся снаружи, чтобы приложение само выбирало приёмник
диагностики. AuthVk добавляет безопасные breadcrumbs по этапам авторизации,
сообщает о неожиданных ошибках входа, проблемах с Keystore и восстановлении
зашифрованного секрета. Токены, ключи, идентификаторы пользователя и содержимое
ответов в события не записываются. Отмена входа и сетевые ошибки при logout не
отправляются как non-fatal. Ошибки сети во время явно запрошенного пользователем
входа отправляются как non-fatal, чтобы их можно было увидеть без ожидания
следующего сбоя.

Правила резервного копирования окончательно задаёт основное приложение. Оно
должно исключить `auth.prefs.xml`, `tink_prefs.xml`,
`personal_crypto.prefs.xml` и `personal_crypto_tink_prefs.xml`.
