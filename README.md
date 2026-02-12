# RouteRecorder

Aplicación Android para registrar recorridos GPS y generar videos animados del trayecto.

## Arquitectura

- **Clean Architecture + MVVM** con módulos Gradle
- **Kotlin** + Jetpack Compose
- **Room** para persistencia local
- **Hilt** para inyección de dependencias
- **MediaCodec + Canvas** para generación de video on-device
- **Fused Location Provider** para tracking GPS

## Módulos

```
:app                    → Punto de entrada, DI, navegación
:core:domain            → Modelos, interfaces de repositorio, use cases
:core:data              → Room DB, implementaciones de repositorio
:core:common            → Utilidades (Kalman filter, formateo)
:feature:tracking       → UI de seguimiento activo
:feature:history        → Lista de recorridos
:feature:video          → Generación de video
:service:tracking       → Foreground Service GPS
:service:video-render   → Motor de renderizado de video
```

## Requisitos

- Android 8.0+ (API 26)
- Android Studio Hedgehog o superior
- Kotlin 1.9+

## Setup

1. Clonar repositorio
2. Abrir en Android Studio
3. Sync Gradle
4. Configurar Mapbox token en `gradle.properties`:
   ```
   MAPBOX_DOWNLOADS_TOKEN=sk.YOUR_TOKEN
   ```
5. Run en dispositivo físico (GPS no funciona en emulador estándar)
