# Changelog

This file is maintained in English first, followed by a complete Russian translation.

## [Unreleased]

- Updated application version to 0.4.6+56. Added configurable low-battery notifications with independent thresholds for the left earbud, right earbud and charging case (5–80% in 5% steps), a test notification action, duplicate-alert suppression and settings backup support. Synchronized connection/disconnection transitions and optimized MBB stream parsing, widget fallback caching and bitmap LRU caching.

- Updated application version to 0.4.5+55. Unified action-button feedback, refined the color picker, and separated Copy logs from Copy raw data. Unknown protocol fields are collapsed by default.

- Renamed About to Settings and added an App operation heading. Added a Backup section, moving layout import/export out of the editor and extending JSON backups with widget color modes/RGB, opacity, labels and shared favorites. Legacy layout files remain supported; widget profiles map by type/creation order and are retained for recreated widgets.

- Added opt-in Companion Device Manager association and a system-bound CompanionDeviceService on Android 12+, without a permanent notification. Includes per-headset consent/removal, Android 16 presence events, observation restoration, shared-connection recovery and expanded lifecycle diagnostics. OEM behavior is not yet device-verified.

- Added a persistent favorites palette below the color picker: up to 24 unique RGB colors shared across widget backgrounds, elements and text, with tap-to-select and add/remove actions.

- Added a separate Text page with automatic, white, black, system-accent and custom text/icon colors per widget. Text remains opaque; the three tabs occupy equal widths.

- Reorganized widget configuration into swipeable Background / Elements / Text pages with a fixed preview and bottom actions; short wide windows place the preview beside the controls. Label controls are below text colors and are omitted for ANC and compact widgets.

- Added independent per-widget background and element colors (default/system accent/custom), a hue-ring RGB picker with a saturation/brightness field and HEX input, and shared palettes for live previews and all four widget types. Existing colors and separate opacity settings are preserved by default.

- Documentation synchronized with the current implementation.
- Added bounded WorkManager widget refreshes for Bluetooth connection/disconnection events, A2DP readiness waiting and fresh battery/ANC report tracking.
- Added persistent publication revisions and a retry worker so unchanged battery readings can repair interrupted widget updates; Glance failures no longer abort the Bluetooth packet collector.
- Added a persistent background-event trace to copied device diagnostics and documented HyperOS background restrictions.
- Coalesced paired ACL/A2DP events without cancelling active reads, bypassed obsolete retry backoff on new events, and removed headset-readiness waiting from disconnect reconciliation.
- Reused recent current-session battery/ANC reports, batched adjacent publications over 75 ms, and updated only affected widget providers while retaining persistent retry revisions.

## [0.4.4+49] — 2026-08

### Added

- Active Bluetooth audio codec badge next to the connected-headset name and codec information in the device diagnostics and copied raw dump.
- Interface-layout JSON backup and restore, including block order, visibility and scale settings.
- Per-block visibility controls in the interface editor, with required home-screen blocks protected from being hidden.

### Changed

- Added Material 3 Expressive-inspired motion: directional screen transitions, circular container transforms for diagnostics and application settings, a sliding ANC selection indicator and stronger press feedback.
- Restyled application settings, theme choices, dropdown menus and widget configuration menus as consistent tonal cards with larger touch targets.
- Made the ANC card more compact and stabilized the charging indicator to the left of the percentage.
- Improved interface-editor previews, percentage-based scaling controls and import confirmation.

### Fixed

- Active codec detection now uses Android A2DP codec-change events instead of a restricted Android 16 binder call.
- Dropdown items and navigation cards now keep visible feedback long enough before closing or changing screens.
- Widget-layout import survives Activity recreation and cannot hide required home-screen blocks.

## [0.4.3+45] — 2026-08

### Changed

- Reworked the launcher icon from the new SVG source with a light adaptive
  background, transparent legacy mipmaps and a dedicated monochrome layer for
  Android 13 themed icons.
- Split adaptive-icon resources between API 26 and API 33 so older Android
  versions never parse the monochrome-only element.
- Regenerated Android 9–11 widget fallback previews in English, using the same
  values, progress proportions, palette and vector symbols as the widgets.

## [0.4.2+44] — 2026-08

### Fixed

- The main screen and the headphone settings screen no longer open scrolled
  to the middle of the page when blocks were reordered in the interface
  editor: the lists now compose only after the saved order is loaded.
- The noise cancellation card no longer slightly changes its height when
  switching between Off, Noise cancelling and Transparency: the header row
  has a fixed minimum height regardless of the level chevron.

## [0.4.2+43] — 2026-08

### Added

- ANC levels for FreeBuds SE 4 ANC (Normal, Comfort, Ultra, Dynamic): the
  profile now uses the advanced ANC module, matching real device replies and
  the official Huawei configuration.
- Custom EQ preset import/export for FreeBuds SE 4 ANC. Preset files remain
  strictly model-scoped: a 6i file cannot be imported into SE 4 ANC and vice
  versa.
- Shorter EQ preset file names: `EQ_6i_FBN.json` and `EQ_SE4_ANC_FBN.json`.

### Changed

- Redesigned the noise cancellation card header: a single-line title on the
  left and the current mode or selected ANC level on the right; tapping the
  level opens a compact dropdown.
- The delete button in the custom EQ editor moved to the always-visible
  bottom bar next to Cancel.

### Fixed

- ANC mode and level changes now re-read the actual headset state after
  400 ms, so the UI no longer sticks to a stale or optimistic value.
- A hold write failure no longer skips the follow-up state re-read for
  split-hold models.

## [0.4.2+41] — 2026-08

### Added

- HUAWEI FreeBuds SE 4 ANC profile, validated on a real device:
  - battery and device information;
  - basic ANC;
  - double and triple tap;
  - independent left/right hold actions and ANC hold modes;
  - built-in and custom 10-band equalizer;
  - low-latency mode;
  - automatic black color detection.
- Shared interface layout editor:
  - reorder main-screen blocks and headphone-settings sections;
  - scale the headphone image, battery rows and ANC buttons;
  - real previews of the affected components;
  - reset to the default layout.
- JSON import and export for FreeBuds 6i and FreeBuds SE 4 ANC custom equalizer presets through Android Storage Access Framework.
- Official FreeBuds 6i extended equalizer presets: Classics (`200`) and Hi-Fi (`201`).

### Changed

- Interface layout and scale settings are shared across supported headset models.
- Unsupported model-specific sections remain reorderable in the editor but are visually marked as unavailable.
- Updated Gradle Wrapper to 8.14.4.
- System Back navigation now returns through internal screens before leaving the application.

### Fixed

- Corrected split-hold partial responses so changing one earbud does not erase the other side.
- Corrected local earbud-finding start/stop command semantics and guaranteed signal shutdown when leaving the screen or disconnecting.
- Improved editor slider persistence and preview layout behavior.

## [0.3.9+36] — 2026-08

### Added

- Local acoustic search for connected FreeBuds 5i and 6i with separate left/right controls and a loud-volume warning.

- Initial HUAWEI FreeBuds 4i profile with device information, battery, basic ANC, auto-pause, double-tap, and hold controls.
- Initial HUAWEI FreeBuds SE 4 ANC profile with battery, basic ANC, double/triple tap, independent hold actions, built-in EQ and low latency.

### Fixed

- Automatic detection of the purple HUAWEI FreeBuds 6i variant from device-information code `0x07`.

## [0.3.7+32] — 2026-08

### Changed

- Renamed the application to **FreeBuddy Next**.
- Changed the Android application ID and Kotlin namespace to `io.github.razziell.freebuddynext`.
- Updated widget configuration components, resources, tests and documentation for the new package.

### Important

- Android treats FreeBuddy Next as a separate application. It can be installed alongside the previous `com.lastgimbus.the.freebuddy` package.
- Existing permissions, application settings and placed widgets are not migrated automatically from the previous package.

## [0.3.6+31] — 2026-08

### Added

- Full Huawei FreeBuds 6i profile with model-specific capabilities, colors and images.
- Custom 10-band FreeBuds 6i equalizer editor:
  - frequencies from 60 Hz to 16 kHz;
  - range from −6.0 dB to +6.0 dB;
  - 0.5 dB step;
  - up to three on-device presets;
  - create, rename, edit, select, reset and delete operations.
- Extended device diagnostics:
  - unknown TLV fields;
  - lossless mixed binary/text rendering such as `0x0145 ("E")`;
  - raw MBB response history;
  - copy-to-clipboard action;
  - FreeBuds 6i EQ and preset diagnostics.
- Estimated production dates for the case and both earbuds, decoded from their serial numbers.
- Warning when the left and right earbuds appear to have been manufactured in different periods.
- Automatic color detection based on confirmed device-information codes:
  - FreeBuds 5i: white, blue and black;
  - FreeBuds 6i: black.
- Per-headset manual color override, keyed primarily by the case serial number with MAC fallback.
- TalkBack accessibility semantics for headings, live connection states, switches, radio buttons, sliders, ANC controls, widgets and interactive cards.

### Changed

- Split model-specific logic into `HeadphonesProtocol`, `FreeBuds5iProtocol` and `FreeBuds6iProtocol`.
- Aligned setting read/write packets with verified OpenFreebuds Huawei MBB handlers.
- Made ANC hold-mode constraints explicit and automatically restore a valid mode set when ANC cycling is enabled.
- Stabilized widget and lifecycle updates:
  - one combined widget update for battery and ANC changes;
  - stable DataStore flows;
  - bounded A2DP liveness polling;
  - safe ANC-widget behavior when the process or connection is unavailable.
- Updated Android build stack to AGP 8.13.2 while keeping Gradle 8.14.3, Kotlin 2.2.20 and compile/target SDK 36.

### Fixed

- Invalid empty read requests for battery, ANC, gestures, EQ, low latency and Dual Connect.
- Hold and swipe settings now write both earbuds where required.
- Low-latency read-back no longer races the headset state update.
- ANC mode toggles no longer remain permanently disabled after switching hold action to None and back.
- RFCOMM writes are serialized and connection cleanup cannot cancel itself.
- Connection failures no longer leave the interface permanently in Connecting state.
- Revoked Bluetooth permission no longer causes an unhandled `SecurityException`.
- Diagnostic packet history no longer forces a full Compose recomposition for every incoming packet.

## [0.3.3+22] — 2026-08

### Added

- Separate background and element transparency controls for every widget instance.
- Per-widget battery label modes: full names, L/R, or hidden.
- Model-aware headset color settings.

### Changed

- Improved compact widget sizing and battery progress rendering.
- Reworked dynamic and fallback widget previews to match real widget geometry and percentages.

## [0.3.2+20] — 2026-08

### Added

- ANC mode widget.
- Compact battery widget.
- Individual per-widget transparency configuration with live preview.
- English, Russian, Polish and German localization.
- Application theme selection: system, light or dark.
- Optional decorative emojis.
- Device-information dialog with firmware, hardware and serial numbers.
- Separate black, white and blue FreeBuds 5i images.

### Changed

- Improved Material You contrast, widget previews and responsive widget layouts.
- Added configurable labels to the regular battery widgets.

## [0.3.1+18] — 2026-08

### Added

- Native Glance battery widgets with Material You colors.
- Three-row and three-column battery layouts.
- Dual Connect device-management interface.
- Full Russian localization and improved Polish localization.

### Fixed

- Bluetooth connection/disconnection ANRs caused by synchronous Dart/JNI callbacks.
- Multi-headset pairing races and stale connection state.
- `100%` wrapping in battery widgets.
- Background task leaks and duplicate WorkManager jobs.

## [0.3.0+16] — 2026-08

### Breaking change

- Rewrote the application from Flutter/Dart to native Kotlin and Jetpack Compose.

### Added

- Native RFCOMM transport using coroutines and IO dispatchers.
- Huawei MBB packet parser with CRC16-XMODEM validation.
- Compose Material 3 interface.
- StateFlow-based connection state and model-specific command handling.
- Native AndroidX Glance widgets.

### Changed

- Minimum Android version raised to Android 9 (API 28).
- Preserved package ID and release-signing compatibility with earlier versions.

## [0.2.0] — 2023-09-24

- Added the original battery home-screen widget.
- Adopted wallpaper-derived Material 3 colors on Android 12.
- Moved headphone settings to a dedicated screen.
- Improved landscape layout.

## [0.1.4] — 2023-04-02

- Completed gesture settings.
- Removed the experimental sleep mode.

## [0.1.0] — 2023-01-29

- Migrated the original Flutter application to a custom Bluetooth plugin.
- Improved Android 12 Bluetooth handling.
- Added Material 3 UI, animations, tests, auto-pause and demo mode.

## [0.0.1] — 2022-09-01

- Initial release with battery display and basic ANC control.

---

# Журнал изменений — Русская версия

Сначала в этом файле приводится английская версия, ниже — её полный русский перевод.

## [Не выпущено]

- Версия приложения обновлена до 0.4.6+56. Добавлены настраиваемые уведомления о низком заряде отдельно для левого наушника, правого наушника и зарядного кейса (порог 5–80% с шагом 5%), кнопка тестового уведомления, защита от повторных срабатываний и сохранение в резервной копии. Синхронизированы переходы подключения/отключения, оптимизированы парсер MBB-потока, кэш профилей виджетов и LRU-кэш битмапов.

- Версия приложения обновлена до 0.4.5+55. Унифицированы кнопки и анимации нажатия, доработан пикер цветов. Копирование логов отделено от сырых данных; неизвестные поля протокола по умолчанию свёрнуты.

- Экран «О приложении» переименован в «Настройки», добавлен заголовок «Работа приложения». В новый раздел «Резервная копия» перенесён импорт/экспорт расположения; JSON-копия теперь включает режимы и RGB цветов виджетов, прозрачность, подписи и общее избранное. Старые копии расположения поддерживаются; оформление сопоставляется по типу и порядку создания виджетов и сохраняется для повторно добавленных экземпляров.

- Добавлены добровольная привязка через Companion Device Manager и системно привязанная CompanionDeviceService на Android 12+ без постоянного уведомления: подтверждение/удаление связи для каждого комплекта, события присутствия Android 16, восстановление наблюдения и общего соединения, расширенная диагностика жизненного цикла. Поведение на прошивках производителей ещё не проверено на устройстве.

- Внизу пикера добавлена сохраняемая палитра избранного: до 24 уникальных RGB-цветов для фона, элементов и текста всех виджетов, выбор нажатием и добавление/удаление цвета.

- Добавлена отдельная вкладка «Текст» с автоматическим, белым, чёрным, системным акцентным или своим цветом текста и значков каждого виджета. Текст остаётся непрозрачным; три вкладки равномерно занимают всю ширину.

- Настройки виджетов разделены на перелистываемые страницы «Фон» / «Элементы» / «Текст» с закреплённым превью и нижними кнопками; в низких широких окнах превью расположено рядом с настройками. Подписи находятся под цветом текста и скрыты у ANC и компактного виджета.

- Добавлены независимые цвета фона и элементов каждого виджета (стандартный/системный акцент/свой), круговой RGB-пикер с полем насыщенности/яркости и HEX-вводом, общая палитра для живого превью и всех четырёх видов виджетов. Прежние цвета и отдельные настройки прозрачности по умолчанию сохранены.

- Документация синхронизирована с текущей реализацией.
- Добавлены ограниченные по времени задачи WorkManager для обновления виджетов при подключении/отключении Bluetooth, ожидание готовности A2DP и отслеживание свежих ответов батареи и ANC.
- Добавлены сохраняемые ревизии публикации и задача повторной отправки: неизменившиеся проценты теперь позволяют восстановить прерванное обновление виджета; ошибки Glance больше не обрывают обработку Bluetooth-пакетов.
- В копируемую диагностику добавлен сохраняемый журнал фоновых событий; описаны ограничения фоновой работы HyperOS.
- Парные события ACL/A2DP объединены без отмены активного опроса; новые события обходят устаревшие отложенные повторы; при отключении больше не ожидается появление наушников.
- Добавлены повторное использование свежих ответов текущей сессии, объединение близких публикаций в окно 75 мс и обновление только затронутых видов виджетов с сохранением ревизий для повторной отправки.

## [0.4.4+49] — 2026-08

### Добавлено

- Плашка активного Bluetooth-аудиокодека рядом с названием подключённых наушников, а также кодек в диагностическом меню и копируемом дампе.
- Резервное копирование и восстановление раскладки интерфейса в JSON, включая порядок, видимость блоков и масштабы.
- Управление видимостью блоков в редакторе интерфейса с защитой обязательных элементов главного экрана от скрытия.

### Изменено

- Добавлены анимации в стиле Material 3 Expressive: направленные переходы экранов, круговые container transform для диагностики и настроек приложения, скользящий индикатор ANC и усиленный отклик на нажатия.
- Настройки приложения, варианты темы, выпадающие меню и меню настройки виджетов оформлены едиными тональными карточками с увеличенными областями нажатия.
- Карточка ANC стала компактнее, а значок зарядки получил стабильную позицию слева от процентов.
- Улучшены превью редактора интерфейса, процентные регуляторы масштаба и подтверждение импорта.

### Исправлено

- Активный кодек теперь определяется по системным событиям A2DP вместо ограниченного binder-вызова Android 16.
- Пункты выпадающих меню и навигационные карточки сохраняют заметный отклик перед закрытием или сменой экрана.
- Импорт раскладки переживает пересоздание Activity и не может скрыть обязательные блоки главного экрана.

## [0.4.3+45] — 2026-08

### Изменено

- Иконка приложения переработана из нового SVG: добавлены светлый фон adaptive
  icon, прозрачные legacy-mipmap и отдельный monochrome-слой для тематических
  значков Android 13.
- Ресурсы adaptive icon разделены для API 26 и API 33, поэтому старые версии
  Android не разбирают элемент, предназначенный только для monochrome-иконки.
- Fallback-эскизы виджетов для Android 9–11 перегенерированы на английском с
  теми же значениями, пропорциями заполнения, палитрой и векторными символами,
  что и реальные виджеты.

## [0.4.2+44] — 2026-08

### Исправлено

- Главный экран и экран настроек наушников больше не открываются
  проскролленными к середине страницы после перестановки блоков в редакторе
  интерфейса: списки строятся только после загрузки сохранённого порядка.
- Карточка шумоподавления больше не меняет слегка высоту при переключении
  между «Выключено», «Шумоподавление» и «Прозрачность»: строка заголовка
  имеет фиксированную минимальную высоту независимо от шеврона уровня.

## [0.4.2+43] — 2026-08

### Добавлено

- Уровни ANC для FreeBuds SE 4 ANC (обычный, комфортный, максимальный,
  динамический): профиль переведён на расширенный модуль ANC в соответствии
  с ответами реального устройства и официальной конфигурацией Huawei.
- Импорт и экспорт пользовательских EQ-пресетов для FreeBuds SE 4 ANC. Файлы
  остаются строго модельными: файл 6i нельзя импортировать в SE 4 ANC и
  наоборот.
- Более короткие имена файлов пресетов: `EQ_6i_FBN.json` и
  `EQ_SE4_ANC_FBN.json`.

### Изменено

- Переработан заголовок карточки шумоподавления: однострочное название слева
  и текущий режим или выбранный уровень ANC справа; нажатие на уровень
  открывает компактное меню.
- Кнопка удаления в редакторе пользовательского EQ перенесена в постоянно
  видимую нижнюю панель рядом с «Отмена».

### Исправлено

- После смены режима или уровня ANC приложение через 400 мс перечитывает
  фактическое состояние наушников, поэтому интерфейс больше не остаётся в
  устаревшем или оптимистичном состоянии.
- Сбой записи действия удержания больше не пропускает последующее чтение
  состояния у моделей с раздельным удержанием.

## [0.4.2+41] — 2026-08

### Добавлено

- Профиль HUAWEI FreeBuds SE 4 ANC, проверенный на реальном устройстве:
  - заряд и сведения об устройстве;
  - режимы и уровни ANC;
  - двойное и тройное касание;
  - раздельные действия удержания и режимы ANC для левого/правого наушника;
  - встроенный и пользовательский 10-полосный эквалайзер;
  - режим низкой задержки;
  - автоматическое определение чёрного цвета.
- Общий редактор интерфейса:
  - изменение порядка блоков главного экрана и разделов настроек наушников;
  - масштабирование изображения, строк заряда и кнопок ANC;
  - реальные превью изменяемых компонентов;
  - сброс к стандартной раскладке.
- Импорт и экспорт пользовательских пресетов эквалайзера FreeBuds 6i и
  FreeBuds SE 4 ANC в JSON через системный Storage Access Framework Android.
- Официальные расширенные пресеты FreeBuds 6i: «Классика» (`200`) и Hi-Fi
  (`201`).

### Изменено

- Раскладка интерфейса и масштабы теперь общие для всех поддерживаемых моделей.
- Неподдерживаемые моделью разделы можно перемещать в редакторе, но они
  визуально помечены как недоступные.
- Gradle Wrapper обновлён до 8.14.4.
- Системная навигация «Назад» сначала возвращает по внутренним экранам и только
  затем закрывает приложение.

### Исправлено

- Частичные ответы раздельного удержания больше не стирают настройку второго
  наушника.
- Исправлена семантика запуска/остановки локального поиска; сигнал
  гарантированно останавливается при выходе с экрана или отключении.
- Улучшено сохранение значений ползунков и расположение превью редактора.

## [0.3.9+36] — 2026-08

### Добавлено

- Локальный звуковой поиск подключённых FreeBuds 5i и 6i с раздельным управлением левым/правым наушником и предупреждением о громкости.

- Начальный профиль HUAWEI FreeBuds 4i со сведениями об устройстве, зарядом, базовым ANC, автопаузой, двойным касанием и управлением удержанием.
- Начальный профиль HUAWEI FreeBuds SE 4 ANC с зарядом, базовым ANC, двойным/тройным касанием, раздельными действиями удержания, встроенным эквалайзером и низкой задержкой.

### Исправлено

- Автоматическое определение фиолетового варианта HUAWEI FreeBuds 6i по коду сведений об устройстве `0x07`.

## [0.3.7+32] — 2026-08

### Изменено

- Приложение переименовано в **FreeBuddy Next**.
- Android application ID и Kotlin namespace изменены на `io.github.razziell.freebuddynext`.
- Конфигурации компонентов виджетов, ресурсы, тесты и документация обновлены для нового пакета.

### Важно

- Разрешения, настройки приложения и размещённые виджеты из прежнего пакета автоматически не переносятся.

## [0.3.6+31] — 2026-08

### Добавлено

- Полноценный профиль Huawei FreeBuds 6i с отдельными возможностями, цветами и изображениями.
- Редактор пользовательского 10-полосного эквалайзера FreeBuds 6i:
  - частоты от 60 Гц до 16 кГц;
  - диапазон от −6,0 до +6,0 дБ;
  - шаг 0,5 дБ;
  - до трёх пресетов в памяти наушников;
  - создание, переименование, редактирование, выбор, сброс и удаление.
- Расширенная диагностика устройства:
  - неизвестные TLV-поля;
  - отображение смешанных бинарных и текстовых значений без потери данных, например `0x0145 ("E")`;
  - журнал сырых ответов MBB;
  - копирование в буфер обмена;
  - диагностика эквалайзера и пресетов FreeBuds 6i.
- Вычисление предполагаемой даты производства кейса и обоих наушников по серийным номерам.
- Предупреждение, если левый и правый наушники предположительно произведены в разные периоды.
- Автоматическое определение цвета по подтверждённым кодам сведений об устройстве:
  - FreeBuds 5i: белый, голубой и чёрный;
  - FreeBuds 6i: чёрный.
- Принудительный выбор цвета отдельно для каждого комплекта: основной идентификатор — серийный номер кейса, резервный — MAC.
- Семантика TalkBack для заголовков, динамических состояний подключения, переключателей, радиокнопок, ползунков, ANC, виджетов и интерактивных карточек.

### Изменено

- Модельно-зависимая логика разделена между `HeadphonesProtocol`, `FreeBuds5iProtocol` и `FreeBuds6iProtocol`.
- Команды чтения и записи настроек приведены в соответствие с проверенными обработчиками Huawei MBB из Openfreebuds.
- Ограничения режимов ANC при удержании сделаны явными; при включении смены ANC автоматически восстанавливается допустимый набор режимов.
- Улучшена стабильность виджетов и жизненного цикла:
  - одно объединённое обновление виджетов при изменении заряда и ANC;
  - стабильные потоки DataStore;
  - ограниченный опрос состояния A2DP;
  - безопасное поведение ANC-виджета при отсутствии процесса или соединения.
- Android Gradle Plugin обновлён до 8.13.2 при сохранении Gradle 8.14.3, Kotlin 2.2.20 и compile/target SDK 36.

### Исправлено

- Некорректные пустые запросы чтения заряда, ANC, жестов, эквалайзера, низкой задержки и Dual Connect.
- Настройки удержания и смахивания теперь записываются для обоих наушников там, где это требуется протоколом.
- Повторное чтение низкой задержки больше не опережает обновление состояния наушников.
- Переключатели режимов ANC больше не остаются постоянно выключенными после переключения действия удержания на «Нет» и обратно.
- Записи RFCOMM сериализованы, а очистка соединения не может отменить сама себя.
- Ошибка подключения больше не оставляет интерфейс навсегда в состоянии «Подключение».
- Отзыв разрешения Bluetooth больше не вызывает необработанный `SecurityException`.
- Журнал диагностических пакетов больше не вызывает полную рекомпозицию Compose на каждый входящий пакет.

## [0.3.3+22] — 2026-08

### Добавлено

- Отдельная регулировка прозрачности внешнего фона и внутренних элементов каждого экземпляра виджета.
- Режимы подписей батарейных виджетов: полные названия, L/R или без подписей.
- Настройки цвета с учётом модели наушников.

### Изменено

- Улучшены размеры компактного виджета и отображение прогресса заряда.
- Динамические и статические эскизы виджетов приведены в соответствие с реальной геометрией и процентами.

## [0.3.2+20] — 2026-08

### Добавлено

- Виджет режимов ANC.
- Компактный виджет заряда.
- Индивидуальная прозрачность каждого виджета с живым превью.
- Английская, русская, польская и немецкая локализации.
- Выбор темы приложения: системная, светлая или тёмная.
- Возможность скрыть декоративные эмодзи.
- Диалог сведений об устройстве с прошивкой, аппаратной версией и серийными номерами.
- Отдельные изображения чёрных, белых и голубых FreeBuds 5i.

### Изменено

- Улучшены контраст Material You, превью и адаптивная разметка виджетов.
- Добавлена настройка подписей основных батарейных виджетов.

## [0.3.1+18] — 2026-08

### Добавлено

- Нативные Glance-виджеты заряда с цветами Material You.
- Виджеты с тремя строками и тремя столбцами.
- Интерфейс управления устройствами Dual Connect.
- Полная русская локализация и улучшенный польский перевод.

### Исправлено

- ANR при подключении и отключении Bluetooth, вызванные синхронными Dart/JNI callback.
- Гонки при нескольких сопряжённых парах и устаревшее состояние соединения.
- Перенос `100%` в виджетах заряда.
- Утечки фоновых задач и дублирующиеся задания WorkManager.

## [0.3.0+16] — 2026-08

### Критическое изменение

- Приложение переписано с Flutter/Dart на нативный Kotlin и Jetpack Compose.

### Добавлено

- Нативный RFCOMM-транспорт на корутинах и IO-потоках.
- Парсер Huawei MBB с проверкой CRC16-XMODEM.
- Интерфейс Compose Material 3.
- Состояние соединения на StateFlow и модельно-зависимые команды.
- Нативные AndroidX Glance-виджеты.

### Изменено

- Минимальная версия Android повышена до Android 9 (API 28).
- Сохранены package ID и совместимость release-подписи с предыдущими версиями.

## [0.2.0] — 2023-09-24

- Добавлен первоначальный виджет заряда на главном экране.
- Добавлены цвета Material 3 из обоев на Android 12.
- Настройки наушников перенесены на отдельный экран.
- Улучшена альбомная ориентация.

## [0.1.4] — 2023-04-02

- Завершена реализация настроек жестов.
- Удалён экспериментальный режим сна.

## [0.1.0] — 2023-01-29

- Исходное Flutter-приложение переведено на собственный Bluetooth-плагин.
- Улучшена работа Bluetooth на Android 12.
- Добавлены Material 3, анимации, тесты, автопауза и демонстрационный режим.

## [0.0.1] — 2022-09-01

- Первый выпуск с отображением заряда и базовым управлением ANC.
