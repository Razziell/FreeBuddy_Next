# FreeBuddy Next

[English](#english) · [Русский](#русский)

## English

A native Android application for controlling supported Huawei FreeBuds.

> FreeBuddy Next is an independent open-source project and is not affiliated with, endorsed by, or sponsored by Huawei.
>
> Artificial intelligence tools were used to assist with code analysis, implementation, debugging, testing, and documentation. Technical decisions, validation on real devices, and responsibility for the final result remain with the project maintainer.

Current version: **0.4.4 (versionCode 49)**

### Supported headphones

| Model | Status | Notes |
|---|---|---|
| HUAWEI FreeBuds 4i | Supported and tested on a real device | Battery, basic ANC, auto-pause, double-tap, hold controls, device information and white color detection |
| HUAWEI FreeBuds SE 4 ANC | Supported and tested on a real device | Battery, ANC modes and levels, double/triple tap, independent left/right hold actions, built-in and custom EQ, low latency, device information and black color detection |
| HUAWEI FreeBuds 5i | Supported and tested | Battery, ANC, gestures, EQ, LDAC, low latency, Dual Connect, local sound-based finding, device information and color detection |
| HUAWEI FreeBuds 6i | Supported and tested on real devices | Battery, ANC, gestures, built-in and custom EQ, LDAC, low latency, Dual Connect, local sound-based finding, device information and color detection |

The protocol layer is model-specific and modular. `FreeBuds4iProtocol`, `FreeBudsSe4AncProtocol`, `FreeBuds5iProtocol` and `FreeBuds6iProtocol` compose reusable feature modules through the shared `HeadphonesProtocol` and `ProtocolModule` interfaces. Unsupported features are absent from a profile instead of being represented by inactive capability flags.

### Features

#### Headphone controls

- Battery levels for the left earbud, right earbud and charging case.
- Charging state reporting with a stable charging indicator next to the percentage.
- Active Bluetooth audio codec badge and codec information in device diagnostics.
- Noise cancellation modes:
  - Off;
  - Noise cancelling;
  - Transparency.
- Noise cancellation intensity levels, including Dynamic mode where supported.
- Auto-pause when an earbud is removed.
- Low-latency mode.
- LDAC toggle.
- Dual Connect:
  - paired-device list;
  - connection state;
  - connect and disconnect;
  - preferred device;
  - per-device auto-connect where supported;
  - unpairing.
- Gesture configuration, depending on the model:
  - double tap;
  - triple tap;
  - touch and hold;
  - ANC modes cycled by holding;
  - swipe.
- Local sound-based earbud finding for connected FreeBuds 5i and 6i:
  - independent left/right signal;
  - maximum-volume safety warning;
  - automatic stop when leaving the screen, backgrounding the app, or disconnecting.

#### Equalizer

- Four built-in presets: Default, Bass boost, Treble boost and Vocals.
- FreeBuds 6i and FreeBuds SE 4 ANC custom equalizer editor:
  - up to three presets;
  - 10 bands: 60 Hz, 125 Hz, 250 Hz, 500 Hz, 1 kHz, 2 kHz, 4 kHz, 8 kHz, 12 kHz and 16 kHz;
  - range from −6.0 dB to +6.0 dB;
  - 0.5 dB step;
  - create, rename, edit, select, reset and delete presets;
  - portable JSON import and export for FreeBuds 6i and FreeBuds SE 4 ANC through the Android system file picker.

#### Device information and diagnostics

Tap the headphone image to open device information:

- model and submodel;
- firmware and hardware versions;
- case, left-earbud and right-earbud serial numbers;
- estimated production dates decoded from serial numbers;
- warning when the earbuds appear to have been manufactured in different periods;
- unknown device-information TLVs;
- FreeBuds 6i and FreeBuds SE 4 ANC equalizer diagnostics;
- copyable raw MBB protocol responses.

Color detection currently uses confirmed device-information values:

- FreeBuds 4i: white; black and red remain available as manual choices until their codes are confirmed;
- FreeBuds SE 4 ANC: black is detected automatically; white and blue remain available as manual choices until their codes are confirmed;
- FreeBuds 5i: white, blue and black;
- FreeBuds 6i: white, black and purple.

A manual color override is stored per physical headset set. The case serial number is the primary identity, with Bluetooth MAC used as a fallback until the serial number is available.

#### Widgets

FreeBuddy Next provides four Glance widgets:

1. Battery · 3 columns;
2. Battery · 3 rows;
3. Battery · compact;
4. ANC modes.

Widget features:

- Material You colors on Android 12 and newer;
- static compatible palette on Android 9–11;
- per-widget background transparency;
- per-widget element transparency;
- independent background and element colors: default palette, system accent, or custom RGB;
- independent text/icon color: automatic, white, black, system accent, or custom RGB; text and icons stay opaque;
- hue-ring color picker with saturation/brightness field, HEX input and live configuration preview;
- shared favorites palette for up to 24 colors: tap to select, add/remove the selected color; favorites persist immediately, independently of picker/widget confirmation;
- 5% transparency steps;
- optional full labels, L/R labels, or hidden labels for the two regular battery widgets;
- fixed live configuration preview with equal-width, swipeable Background / Elements / Text tabs, label controls below text colors, and pinned Save/Cancel actions;
- localized dynamic previews on supported Android versions;
- exact pixel-based battery progress rendering;
- direct ANC mode switching from the home screen;
- accessible descriptions for TalkBack.

Bluetooth connection events schedule a bounded WorkManager refresh for installed widgets, with A2DP readiness checks and fresh battery/ANC reads. Interrupted widget publication is retried independently of changes in the battery percentages. Nearby ACL/A2DP events share a refresh cycle without cancelling an active read; new connection cycles bypass obsolete retries. Disconnect events reconcile the current profile state without waiting for a headset to appear. Recent reports from the current RFCOMM session are reused, and only missing widget data is requested. Adjacent data publications are batched over 75 ms and target battery or ANC providers separately; appearance changes still refresh all providers. Device information offers separate **Copy logs** (persistent lifecycle/background trace with event generations, cancellation and report source) and **Copy raw data** (device information and protocol responses, without that trace). Unknown protocol fields are collapsed by default.

On Android 12+, **Settings → App operation → Background operation** can associate each paired headset with Android through a system confirmation. The system-bound `CompanionDeviceService` observes device presence without a permanent notification and reuses the existing A2DP-gated RFCOMM repository. While bound and a device is present, a 15-second connection-state reconciliation can repair a lost session; it does not periodically read battery/ANC on a healthy connection. Android 16 uses association-based presence events; Android 12–15 use legacy callbacks. Observation choices are restored on process startup, and removing an app association does not unpair Bluetooth. This feature requires the device's companion-device framework; Android 9–11 keep the existing refresh path.

Diagnostics now include companion callbacks, service lifecycle, Activity start/stop, process/version markers and the latest historical exit reason when Android exposes it. Binding, reboot recovery and OEM power-management behavior still require on-device verification.

On HyperOS, allow **Background autostart** and set the application's battery mode to **No restrictions** if updates are delayed. WorkManager scheduling and broadcast delivery remain subject to system/OEM restrictions; force-stopping the app prevents normal background operation. This is an event-driven refresh, not a permanent Bluetooth foreground service or a guarantee of continuous live updates while the process is suspended.

#### Appearance and accessibility

- System, light and dark application themes.
- Dynamic Material You colors.
- Optional decorative emojis.
- Manual headset color override per physical headset set.
- Shared interface layout editor:
  - reorder main-screen blocks and headphone-settings sections;
  - show or hide optional blocks while keeping essential home-screen blocks visible;
  - scale the headphone image, battery rows and ANC buttons;
  - real component previews and reset to defaults.
- **Settings → App operation → Low battery notification** can alert when the left earbud, right earbud or charging case drops to or below its own threshold (5–80% in 5% steps), with duplicate-alert suppression while a component stays low or when a closed case temporarily stops reporting its level, plus a test notification action.
- **Settings → Backup** imports/exports `Settings_FBN.json`: interface layout, low-battery alert settings, plus per-widget background/element/text color modes and custom RGB, both opacity values, label modes, and the shared favorite-color palette. Import validates the complete file before confirmation. Legacy `Layout_FBN.json` files restore only layout, leaving widgets and favorites unchanged.
- Widget profiles are matched by provider type and ascending local widget ID (creation order), not screen position. Extra local instances remain unchanged. Profiles for widgets not yet recreated are retained as defaults for newly added instances of that type; backups never create or move launcher widgets and do not include Bluetooth associations or live battery data.
- Material 3 Expressive-inspired motion and interactions:
  - animated screen transitions and circular container transforms;
  - sliding ANC selection indicator;
  - visible press feedback for buttons, cards and dropdown items;
  - consistent tonal cards and dropdown menus.
- English, Russian, Polish and German localization.
- TalkBack semantics for headings, status changes, switches, radio buttons, sliders, widgets and interactive cards.

### Architecture

FreeBuddy Next is a native Android application. Flutter and Dart are no longer used.

Main technologies:

- Kotlin 2.2.20;
- Jetpack Compose and Material 3;
- Kotlin Coroutines and StateFlow;
- Android Bluetooth RFCOMM;
- Huawei MBB protocol with CRC16-XMODEM validation;
- AndroidX Glance App Widgets;
- Preferences DataStore;
- Gradle 8.14.4;
- Android Gradle Plugin 8.13.2;
- JDK 21.

Bluetooth socket operations (`connect`, `read`, `write`, and `close`) run on IO dispatchers. Writes are serialized, connection attempts are cancellable and time-limited, and Bluetooth broadcasts do not synchronously call application code on the Android main thread.

### Android requirements

```text
minSdk:     28 (Android 9)
targetSdk:  36
compileSdk: 36
```

Material You widget colors require Android 12 or newer. All core features and widgets remain available on Android 9–11 with fallback colors and previews.

### Building

#### Prerequisites

- JDK 21;
- Android SDK Platform 36;
- Android SDK Build-Tools 36.0.0;
- Android SDK Platform-Tools.

Set the SDK path in `android/local.properties`:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

#### Debug build

```bash
./android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug
```

#### Release verification and build

```bash
./android/gradlew -p android \
  clean \
  :app:testReleaseUnitTest \
  :app:lintRelease \
  :app:assembleRelease \
  --no-daemon \
  --no-configuration-cache
```

The APK is created at:

```text
android/app/build/outputs/apk/release/FreeBuddy_Next_<versionName>+<versionCode>.apk
```

#### Release signing

Create `android/key.properties`:

```properties
storeFile=/absolute/path/to/freebuddy-release.jks
storePassword=your_store_password
keyAlias=freebuddy
keyPassword=your_key_password
```

`key.properties`, keystores and build artifacts are ignored by Git. Keep the release key and its passwords backed up securely; future updates must use the same signing key.

#### Install with ADB

```bash
adb install -r -g android/app/build/outputs/apk/release/FreeBuddy_Next_<versionName>+<versionCode>.apk
```

### Privacy

FreeBuddy Next communicates directly with paired headphones over Bluetooth. It does not require an account and does not upload device diagnostics. Diagnostic data is copied only when the user explicitly presses the copy button.

Diagnostic dumps may contain Bluetooth addresses and real serial numbers. Remove or mask them before publishing logs publicly.

### Name and package ID change

The original application was named **FreeBuddy** and used this package ID:

```text
com.lastgimbus.the.freebuddy
```

The native Kotlin continuation was renamed to **FreeBuddy Next** and now uses:

```text
io.github.razziell.freebuddynext
```

Because the package ID changed, Android treats FreeBuddy Next as a separate application. It can be installed alongside the original FreeBuddy, but it cannot update it in place. Permissions, application settings, and home-screen widgets are not migrated automatically and must be configured again.

### Migration history

The application was rewritten from Flutter/Dart to native Kotlin starting with version 0.3.0. The previous Flutter implementation remains available in Git history. FreeBuddy Next continues the project under the new name and package ID shown above.

### License

Licensed under the Apache License 2.0. See [LICENSE](LICENSE).

---

## Русский

Нативное Android-приложение для управления поддерживаемыми Huawei FreeBuds.

> FreeBuddy Next — независимый проект с открытым исходным кодом, не связанный с Huawei и не поддерживаемый компанией Huawei.
>
> При анализе кода, разработке, отладке, тестировании и подготовке документации использовались инструменты искусственного интеллекта. Технические решения, проверка на реальных устройствах и ответственность за итоговый результат остаются за сопровождающим проекта.

Текущая версия: **0.4.4 (versionCode 49)**

### Поддерживаемые наушники

| Модель | Статус | Примечания |
|---|---|---|
| HUAWEI FreeBuds 4i | Поддерживаются и проверены на реальном устройстве | Заряд, базовый ANC, автопауза, двойное касание, удержание, сведения об устройстве и определение белого цвета |
| HUAWEI FreeBuds SE 4 ANC | Поддерживается и проверен на реальном устройстве | Заряд, режимы и уровни ANC, двойное/тройное касание, раздельные действия удержания, встроенный и пользовательский эквалайзер, низкая задержка, сведения об устройстве и определение чёрного цвета |
| HUAWEI FreeBuds 5i | Поддерживаются и протестированы | Заряд, ANC, жесты, эквалайзер, LDAC, низкая задержка, Dual Connect, локальный звуковой поиск, сведения об устройстве и определение цвета |
| HUAWEI FreeBuds 6i | Поддерживаются и проверены на реальных устройствах | Заряд, ANC, жесты, встроенный и пользовательский эквалайзер, LDAC, низкая задержка, Dual Connect, локальный звуковой поиск, сведения об устройстве и определение цвета |

Протокольный слой разделён по моделям и модулям. `FreeBuds4iProtocol`, `FreeBudsSe4AncProtocol`, `FreeBuds5iProtocol` и `FreeBuds6iProtocol` собираются из переиспользуемых функциональных модулей через интерфейсы `HeadphonesProtocol` и `ProtocolModule`. Неподдерживаемая функция отсутствует в профиле, а не представлена неактивным флагом возможностей.

### Возможности

#### Управление наушниками

- Заряд левого наушника, правого наушника и кейса.
- Состояние зарядки компонентов со стабильным положением значка зарядки рядом с процентами.
- Плашка активного Bluetooth-аудиокодека и кодек в диагностической информации.
- Режимы шумоподавления:
  - выключено;
  - шумоподавление;
  - прозрачность.
- Уровни шумоподавления, включая динамический режим на поддерживаемых моделях.
- Автоматическая пауза при снятии наушника.
- Режим низкой задержки.
- Переключатель LDAC.
- Двойное подключение:
  - список сопряжённых устройств;
  - состояние подключения;
  - подключение и отключение;
  - приоритетное устройство;
  - автоподключение для отдельных устройств на поддерживаемых моделях;
  - удаление сопряжения.
- Настройка жестов в зависимости от модели:
  - двойное касание;
  - тройное касание;
  - касание и удержание;
  - режимы ANC, переключаемые удержанием;
  - смахивание.
- Локальный звуковой поиск подключённых FreeBuds 5i и 6i:
  - независимый сигнал левого и правого наушника;
  - предупреждение о максимальной громкости;
  - автоматическая остановка при выходе с экрана, сворачивании приложения или отключении.

#### Эквалайзер

- Четыре встроенных пресета: По умолчанию, Басы, Высокие и Вокал.
- Редактор пользовательского эквалайзера FreeBuds 6i и FreeBuds SE 4 ANC:
  - до трёх пресетов;
  - 10 полос: 60 Гц, 125 Гц, 250 Гц, 500 Гц, 1 кГц, 2 кГц, 4 кГц, 8 кГц, 12 кГц и 16 кГц;
  - диапазон от −6,0 до +6,0 дБ;
  - шаг 0,5 дБ;
  - создание, переименование, редактирование, выбор, сброс и удаление пресетов;
  - переносимый импорт и экспорт JSON для FreeBuds 6i и FreeBuds SE 4 ANC через системный выбор файлов Android.

#### Сведения об устройстве и диагностика

Нажмите на изображение наушников, чтобы открыть сведения об устройстве:

- модель и подмодель;
- версии прошивки и аппаратной части;
- серийные номера кейса, левого и правого наушников;
- вычисленные даты производства по серийным номерам;
- предупреждение, если наушники предположительно произведены в разные периоды;
- неизвестные TLV-поля сведений об устройстве;
- диагностика эквалайзера FreeBuds 6i и FreeBuds SE 4 ANC;
- копирование сырых ответов протокола MBB.

Автоматическое определение цвета использует подтверждённые значения сведений об устройстве:

- FreeBuds 4i: белый; чёрный и красный пока доступны как ручной выбор до подтверждения кодов;
- FreeBuds SE 4 ANC: чёрный определяется автоматически; белый и синий доступны вручную до подтверждения их кодов;
- FreeBuds 5i: белый, голубой и чёрный;
- FreeBuds 6i: белый, чёрный и фиолетовый.

Принудительный цвет сохраняется отдельно для каждого физического комплекта. Основным идентификатором служит серийный номер кейса, а Bluetooth MAC используется как резерв до получения серийного номера.

#### Виджеты

FreeBuddy Next предоставляет четыре Glance-виджета:

1. «Заряд · 3 столбца»;
2. «Заряд · 3 строки»;
3. «Заряд · компактный»;
4. «Режимы шумоподавления».

Возможности виджетов:

- цвета Material You на Android 12 и новее;
- совместимая статическая палитра на Android 9–11;
- отдельная прозрачность внешнего фона каждого экземпляра;
- отдельная прозрачность внутренних элементов;
- независимый цвет фона и элементов: стандартная палитра, системный акцент или свой RGB;
- отдельный цвет текста и значков: автоматический, белый, чёрный, системный акцент или свой RGB; текст и значки остаются непрозрачными;
- выбор цвета с кольцом оттенков, полем насыщенности/яркости, HEX-вводом и живым превью настройки;
- общая палитра избранного до 24 цветов: выбор нажатием, добавление/удаление выбранного цвета; избранное сохраняется сразу, независимо от подтверждения пикера/настроек виджета;
- шаг прозрачности 5%;
- полные подписи, L/R или отсутствие подписей для двух основных батарейных виджетов;
- закреплённое живое превью с перелистыванием страниц «Фон» / «Элементы» / «Текст» / «Подписи» и постоянными кнопками сохранения/отмены;
- локализованные динамические эскизы на поддерживаемых версиях Android;
- точное пиксельное отображение уровня заряда;
- прямое переключение ANC с рабочего стола;
- описания для TalkBack.

События подключения Bluetooth запускают ограниченную по времени задачу WorkManager для установленных виджетов: она проверяет готовность A2DP и запрашивает свежие данные батареи и ANC. Незавершённая публикация в виджеты повторяется независимо от изменения процентов. Близкие события ACL/A2DP объединяются без отмены активного опроса; новый цикл подключения обходит устаревшие отложенные повторы. Отключение проверяет текущее состояние профиля без ожидания появления наушников. Свежие ответы текущей RFCOMM-сессии используются повторно, запрашиваются только недостающие данные. Близкие публикации объединяются в окно 75 мс и отдельно обновляют батарейные или ANC-виджеты; изменение оформления по-прежнему обновляет все виды. В копируемую диагностику включён небольшой сохраняемый журнал с поколениями событий, отменами и источником свежих данных.

На Android 12+ в **«О приложении» → «Фоновая работа»** можно привязать каждый сопряжённый комплект к Android через системное подтверждение. Системно привязанная `CompanionDeviceService` отслеживает присутствие без постоянного уведомления и использует существующий RFCOMM-repository с проверкой A2DP. Пока служба привязана и устройство присутствует, раз в 15 секунд проверяется состояние соединения для восстановления потерянной сессии; при исправном соединении периодические запросы батареи/ANC не отправляются. На Android 16 используются события присутствия по ассоциации, на Android 12–15 — прежние callbacks. Выбор наблюдения восстанавливается при запуске процесса; удаление привязки к приложению не удаляет Bluetooth-сопряжение. Нужна системная поддержка companion-устройств; на Android 9–11 сохраняется прежний механизм обновления.

В диагностику добавлены callbacks и жизненный цикл companion-службы, запуск/остановка Activity, отметки процесса/версии и последняя причина завершения процесса, если Android её предоставляет. Привязку службы, восстановление после перезагрузки и поведение энергосбережения производителя ещё нужно проверить на устройстве.

При задержках обновления на HyperOS разрешите **фоновый автозапуск** и установите режим батареи приложения **«Без ограничений»**. Планирование WorkManager и доставка событий зависят от ограничений системы и производителя; принудительная остановка приложения препятствует обычной фоновой работе. Это обновление по событиям, а не постоянный Bluetooth foreground service и не гарантия непрерывных обновлений при приостановленном процессе.

#### Оформление и доступность

- Системная, светлая и тёмная тема приложения.
- Динамические цвета Material You.
- Возможность скрыть декоративные эмодзи.
- Принудительный выбор цвета для каждого физического комплекта наушников.
- Общий редактор интерфейса:
  - изменение порядка блоков главного экрана и разделов настроек наушников;
  - скрытие необязательных блоков при сохранении обязательных элементов главного экрана;
  - масштабирование изображения, строк заряда и кнопок ANC;
  - реальные превью компонентов и сброс к стандартной раскладке;
  - импорт и экспорт полной раскладки в виде проверяемой резервной копии JSON.
- Анимации и взаимодействия в стиле Material 3 Expressive:
  - анимированные переходы экранов и круговые container transform;
  - скользящий индикатор выбранного режима ANC;
  - заметный отклик кнопок, карточек и пунктов выпадающих меню;
  - единые тональные карточки и выпадающие меню.
- Английская, русская, польская и немецкая локализации.
- Семантика TalkBack для заголовков, изменений статуса, переключателей, радиокнопок, ползунков, виджетов и интерактивных карточек.

### Архитектура

FreeBuddy Next является нативным Android-приложением. Flutter и Dart больше не используются.

Основные технологии:

- Kotlin 2.2.20;
- Jetpack Compose и Material 3;
- Kotlin Coroutines и StateFlow;
- Android Bluetooth RFCOMM;
- протокол Huawei MBB с проверкой CRC16-XMODEM;
- AndroidX Glance App Widgets;
- Preferences DataStore;
- Gradle 8.14.4;
- Android Gradle Plugin 8.13.2;
- JDK 21.

Операции Bluetooth-сокета (`connect`, `read`, `write` и `close`) выполняются в IO-потоках. Запись сериализована, подключение поддерживает отмену и тайм-аут, а Bluetooth broadcast не вызывает пользовательский код синхронно в главном потоке Android.

### Требования Android

```text
minSdk:     28 (Android 9)
targetSdk:  36
compileSdk: 36
```

Цвета Material You для виджетов требуют Android 12 или новее. Все основные функции и виджеты доступны на Android 9–11 с резервной палитрой и статическими эскизами.

### Сборка

#### Необходимые компоненты

- JDK 21;
- Android SDK Platform 36;
- Android SDK Build-Tools 36.0.0;
- Android SDK Platform-Tools.

Укажите путь SDK в `android/local.properties`:

```properties
sdk.dir=/абсолютный/путь/к/Android/Sdk
```

#### Debug-сборка

```bash
./android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug
```

#### Проверка и release-сборка

```bash
./android/gradlew -p android \
  clean \
  :app:testReleaseUnitTest \
  :app:lintRelease \
  :app:assembleRelease \
  --no-daemon \
  --no-configuration-cache
```

APK создаётся по пути:

```text
android/app/build/outputs/apk/release/FreeBuddy_Next_<versionName>+<versionCode>.apk
```

#### Подпись release

Создайте файл `android/key.properties`:

```properties
storeFile=/абсолютный/путь/к/freebuddy-release.jks
storePassword=пароль_хранилища
keyAlias=freebuddy
keyPassword=пароль_ключа
```

`key.properties`, хранилища ключей и результаты сборки игнорируются Git. Надёжно сохраните release-ключ и пароли: все будущие обновления должны использовать тот же ключ.

#### Установка через ADB

```bash
adb install -r -g android/app/build/outputs/apk/release/FreeBuddy_Next_<versionName>+<versionCode>.apk
```

### Конфиденциальность

FreeBuddy Next напрямую связывается с сопряжёнными наушниками по Bluetooth. Приложение не требует учётной записи и не отправляет диагностику устройства в сеть. Диагностические данные копируются только после явного нажатия пользователем кнопки копирования.

Диагностические дампы могут содержать Bluetooth-адреса и реальные серийные номера. Перед публичной публикацией логов удалите или замаскируйте эти данные.

### Смена названия и package ID

Исходное приложение называлось **FreeBuddy** и использовало package ID:

```text
com.lastgimbus.the.freebuddy
```

Нативное продолжение на Kotlin переименовано в **FreeBuddy Next** и теперь использует:

```text
io.github.razziell.freebuddynext
```

Из-за смены package ID Android считает FreeBuddy Next отдельным приложением. Его можно установить одновременно с исходным FreeBuddy, но установить как обновление поверх него нельзя. Разрешения, настройки приложения и виджеты на рабочем столе автоматически не переносятся — их необходимо настроить заново.

### История миграции

Переход с Flutter/Dart на нативный Kotlin начался с версии 0.3.0. Предыдущая Flutter-реализация остаётся доступной в истории Git. FreeBuddy Next продолжает проект под новым названием и package ID, указанными выше.

### Лицензия

Проект распространяется по лицензии Apache License 2.0. См. [LICENSE](LICENSE).
