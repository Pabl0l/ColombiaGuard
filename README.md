# ColombiaGuard

Filtro inteligente de llamadas entrantes para Colombia, construido sobre `CallScreeningService` de Android.
Bloquea, silencia o permite llamadas **antes de que el teléfono suene**, sin enviar ningún dato a Internet.

## ¿Qué hace?

| Clasificación | Acción |
|---|---|
| `ALLOW` | La llamada suena normalmente |
| `WHITELISTED` | Número en lista blanca, siempre pasa |
| `SILENCE` | La llamada entra pero no suena |
| `BLOCK` | La llamada se rechaza |

Decisiones de filtrado:

- **Rangos numéricos colombianos**: identifica operador (Claro, Movistar, Tigo, WOM, ETB, etc.) y descarta números inválidos.
- **Detección de spam por patrones**: llamadas repetidas, horarios nocturnos, duración cero, spoofing de red.
- **Anti-spoofing**: usa `callerNumberVerificationStatus` de la red; una verificación fallida es señal fuerte de número falsificado.
- **Whitelist / blacklist** persistentes con Room.
- **Emergencias siempre permitidas** (123, 112, 119, 144…) incluso si están en la lista negra.
- **Fail-safe**: ante cualquier error o excepción, la llamada se permite (nunca se cuelga por un bug).
- **Notificaciones** de llamadas bloqueadas/silenciadas (Android 13+).

## Arquitectura

Clean Architecture en capas, sin frameworks de DI (inyección manual en `ColombiaGuardApp`):

```
app/src/main/java/com/colombiaguard/
├── service/            # CallScreeningService, Application (compositor DI), BootReceiver
├── ui/                 # Compose: home, whitelist/blacklist, theme
├── domain/
│   ├── entity/         # CallInfo, FilterConfig, ScreeningResult, análisis de patrones y rangos
│   ├── repository/     # Interfaces (abstracciones)
│   └── usecase/        # ScreenCallUseCase y casos de uso de gestión
└── data/
    ├── local/          # Room: entidades, DAOs, base de datos
    └── repository/     # Implementaciones de repositorio
```

Principios aplicados: **SRP** (un propósito por clase), **DIP** (los use cases dependen de interfaces),
**OCP** (nuevas reglas de filtrado sin tocar las existentes), **fail-safe** en el servicio de screening.

## Stack

- Kotlin 2.1.0 · Android Gradle Plugin 8.7.3 · Gradle 8.14.3
- Jetpack Compose (BOM 2024.12.01) + Material 3
- Room 2.6.1 (KSP)
- Coroutines 1.9.0
- `minSdk` 29 · `targetSdk` 36 · Java 17

## Compilar

```bash
# Requiere JDK 17+ y el Android SDK (ANDROID_HOME o local.properties)
./gradlew assembleDebug

# APK resultante
app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` con `sdk.dir=...` es local de cada máquina y **no se versiona**.

## Activar el filtro

1. Instalar la APK.
2. Ajustes → Aplicaciones → Acceso especial → **Aplicaciones de ID de llamada y spam** → ColombiaGuard.
3. Conceder permisos de teléfono y contactos.
4. En Android 13+, aceptar notificaciones para ver el registro de bloqueos.

Sin ese paso de configuración el sistema **no** entrega las llamadas a la app: es una restricción de Android,
no un fallo de ColombiaGuard.

## Privacidad

- No hay backend ni telemetría: todo el análisis ocurre en el dispositivo.
- Los números se guardan solo en una base Room local.
- La app no declara el permiso `INTERNET`.

## Roadmap

- [ ] Exportar/importar listas y configuración
- [ ] Tests unitarios de `ScreenCallUseCase` y `CallPatternAnalyzer`
- [ ] Pantalla de configuración completa (niveles estricto / normal / permisivo)
- [ ] Base de datos de reportes de spam comunitaria (opt-in)

## Licencia

Uso personal y educativo. Verifica la normativa local antes de distribuir la aplicación.
