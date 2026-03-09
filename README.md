
# Auto Miner Fabric Mod

Клиентский мод Fabric для Minecraft 1.21.8, ориентированный на настраиваемое автоматическое копание, с runtime-контроллером, GUI, HUD, пресетами, прокладкой пути, поиском целей и правилами безопасности.

## Стек

- Minecraft `1.21.8`
- Fabric Loader `0.17.3`
- Fabric API `0.136.0+1.21.8`
- Cloth Config `19.0.147`
- Java `21`

## Сборка

### Windows

```powershell
.\gradlew.bat build
````

### macOS / Linux

```bash
./gradlew build
```

Итоговый jar-файл:

```text
build/libs/autominer-1.0.0.jar
```

## Запуск в dev-режиме

### Windows

```powershell
.\gradlew.bat runClient
```

### macOS / Linux

```bash
./gradlew runClient
```

## Установка

1. Установите Java 21.
2. Установите Minecraft Fabric Loader `0.17.3` для версии `1.21.8`.
3. Скопируйте `build/libs/autominer-1.0.0.jar` в папку `.minecraft/mods`.
4. Запустите профиль Fabric.

## Основные клавиши управления

* `` ` ``: включить / выключить мод
* `P`: пауза / продолжить
* `Right Shift`: открыть GUI
* `V`: выбрать блок под прицелом как единственную цель
* `B`: добавить блок под прицелом в список целей
* `N`: удалить блок под прицелом из списка целей
* `H`: включить / выключить HUD
* `K`: быстро сохранить пресет
* `L`: загрузить активный пресет
* `Delete`: аварийная остановка

Все бинды можно изменить в GUI, и они сохраняются в конфиг автоматически.

## Структура проекта

* `src/main/java/dev/danik/autominer/core`: инициализация мода и связывание runtime-логики
* `src/main/java/dev/danik/autominer/config`: JSON-модель конфига и менеджер конфигурации
* `src/main/java/dev/danik/autominer/input`: определения биндов и менеджер ввода
* `src/main/java/dev/danik/autominer/mining`: контроллер, состояние сессии, исполнитель копания, инструменты, инвентарь
* `src/main/java/dev/danik/autominer/targeting`: поиск целей, кэширование, оценка кандидатов
* `src/main/java/dev/danik/autominer/navigation`: локальное планирование пути и перемещение по точкам
* `src/main/java/dev/danik/autominer/safety`: условия остановки/паузы и проверки опасностей
* `src/main/java/dev/danik/autominer/gui`: runtime-GUI с несколькими вкладками
* `src/main/java/dev/danik/autominer/hud`: отрисовка HUD на экране
* `src/main/java/dev/danik/autominer/render`: оверлеи целей / области
* `src/main/java/dev/danik/autominer/preset`: система встроенных и пользовательских пресетов
* `src/main/java/dev/danik/autominer/util`: математика, реестр, цвет, хелперы для блоков

## Точки расширения

* Добавление новых эвристик поиска целей в `TargetSearchService`
* Добавление более продвинутых правил навигации в `NavigationService`
* Добавление новых правил остановки в `SafetyMonitor`
* Добавление новых runtime-действий или состояний автоматизации в `AutoMinerController`
* Добавление вкладок GUI или виджетов настроек в `AutoMinerScreen`
* Добавление встроенных профилей в `BuiltInPresets`



