# OpenFlux Android fork

Основан на [OpenFluxAndroid от p1neappleXpress](https://github.com/p1neappleXpress/OpenFluxAndroid). Оригинальный транспорт: [OpenFlux](https://github.com/p1neappleXpress/OpenFlux).

Android-клиент для [OpenFlux](https://github.com/meepo161/openfluxfork) с транспортом `vyandex`, импортом локального Netscape `cookies.txt` и автоматическим восстановлением соединения.

Инструкция по подключению: [CONNECT.ru.md](CONNECT.ru.md). В каталоге `apk/` находится тестовая arm64-сборка. APK не содержит документ Яндекса, cookie-файл или ключ канала.

Для сборки из исходников нужны Android Studio/JDK 17, Android SDK, NDK r27+ и Go из `go.mod` OpenFlux. Скрипт `app/src/main/build-openflux.sh` собирает Go-бинарник для `jniLibs`, затем `./gradlew assembleDebug` собирает APK.
