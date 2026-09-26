# EasyLauncher

Минималистичный лаунчер для Android: главный экран из текстовых названий приложений, без лишних значков
и отвлекающих элементов, но с полезными виджетами и гибкой настройкой.

*A minimalist Android launcher — English description below.*

## Скачать

**[⬇ Скачать последнюю версию (APK)](https://github.com/Andrey-154/EasyLauncher/releases/latest)**

Откройте страницу релиза, скачайте `EasyLauncher-release.apk` и установите его (понадобится разрешить
установку из неизвестных источников). После установки выберите EasyLauncher как главный экран.

Для автообновлений можно добавить ссылку на этот репозиторий в приложение
[Obtainium](https://github.com/ImranR98/Obtainium).

Требуется Android 8.0 или новее.

## Возможности

**Главный экран**
- Избранные приложения в бесконечной карусели (от 1 до 8 строк), со звуком и вибрацией прокрутки
- Папки: на главном экране или в карусели, разные стили значков и окон
- Круглые кнопки быстрых действий (фонарик и другие), которые можно поставить в любое место
- Несколько стилей часов, заметка на главном экране
- Погода с прогнозом на сегодня и завтра, поиск города и определение местоположения
- Экранное время и лимиты на приложения с цветовой индикацией
- Быстрые настройки по долгому нажатию на часы

**Список приложений и поиск**
- Поиск с подсветкой совпадений и исправлением опечаток (в том числе в русской раскладке)
- Калькулятор прямо в строке поиска и поиск в интернете, если приложение не найдено
- Сортировка по алфавиту или по частоте использования
- Скрытые приложения с защитой отпечатком пальца или PIN-кодом

**Оформление**
- Готовые темы в одно касание и сохранение своих
- Свои цвета текста, акцента и фона, размытие за окнами
- Отмена последнего действия в течение 5 секунд
- Удобные настройки по разделам с поиском
- Резервная копия всех настроек в файл

Интерфейс на русском и английском языках.

## Конфиденциальность

У EasyLauncher нет своего сервера. Он ничего не собирает и не отправляет. Интернет нужен только для погоды:
запросы идут напрямую в [Open-Meteo](https://open-meteo.com/). Разрешения на статистику использования,
специальные возможности и администратора устройства необязательны: они включаются только для отдельных функций
(экранное время, блокировка экрана двойным касанием).

## Сборка

Нужны JDK 17+ и Android SDK (проще всего через Android Studio).

```
build.bat      отладочная сборка
release.bat    релизная сборка, APK появится в output\
```

Для подписи релиза положите свой ключ в `keystore/` и создайте `keystore.properties`
(`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). Без него релиз не подписывается.

## Благодарности

EasyLauncher основан на [Minimo Launcher](https://github.com/VaibhavLakhera/Minimo) © 2024 Vaibhav Lakhera
(лицензия MIT). Данные о погоде: [Open-Meteo.com](https://open-meteo.com/) (CC BY 4.0).

## Лицензия

[MIT](LICENSE)

---

## English

EasyLauncher is a minimalist, text-based Android launcher built on top of
[Minimo Launcher](https://github.com/VaibhavLakhera/Minimo). It adds:
- an endless favourites carousel;
- folders and movable quick-action buttons (e.g. a flashlight);
- a weather widget with forecast;
- screen time with per-app limits;
- a calculator and typo-tolerant search;
- hidden apps protected by biometrics;
- themes, custom colours, undo and settings backup.

**[Download the latest APK](https://github.com/Andrey-154/EasyLauncher/releases/latest)** (Android 8.0+).

It has no server of its own and collects nothing. Weather is fetched directly from Open-Meteo.

Licensed under the [MIT License](LICENSE).
