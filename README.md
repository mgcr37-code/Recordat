# Recordatorios para Android

Aplicación Android independiente. Guarda recordatorios exclusivamente en SQLite local. No solicita permisos de calendario, cuenta, nube ni Internet.

## Funciones

- Crear y editar por texto o dictado mediante el reconocedor de voz disponible en el teléfono.
- Elegir fecha y hora en controles visibles; reconoce expresiones sencillas como «mañana a las 15:30» y fechas `30/09/2026` con hora. Revisa siempre los campos antes de guardar.
- Avisos a la hora indicada y opcionalmente 15 o 30 minutos, una hora o un día antes.
- Repetición diaria, semanal y mensual; posponer 10 minutos y marcar como realizado desde la notificación.
- Editar, eliminar, reactivar; reprogramar después de reiniciar o cambiar la hora del teléfono.

## Generar el APK

Instala Android Studio y abre esta carpeta como proyecto. Espera a que descargue Gradle y Android SDK 35. En **Build → Build APK(s)** obtendrás `app/build/outputs/apk/debug/app-debug.apk`. Instálalo en el teléfono y concede notificaciones y «Alarmas y recordatorios» cuando Android lo solicite. Requiere Android 8.0 o superior.

El dictado depende del servicio de voz instalado en Android y puede necesitar conexión de red según el proveedor. La entrada escrita y las alarmas funcionan sin Internet. Si Android no concede alarmas exactas, el sistema puede entregar los avisos con retraso. Algunos fabricantes restringen aplicaciones en segundo plano; permite el funcionamiento en segundo plano si observas retrasos.

## Compilar sin Android Studio mediante GitHub Actions

Sube el **contenido de esta carpeta** (incluida `.github/workflows/apk.yml`) a un repositorio privado de GitHub. Abre **Actions → Generar APK → Run workflow**. Cuando termine, abre esa ejecución y descarga el artefacto **Recordatorios-APK**. El APK de depuración está firmado con una clave temporal de GitHub Actions: si recompilas en otra ejecución, posiblemente debas desinstalar la versión anterior antes de instalar la nueva (eso borra los recordatorios locales). Para conservar datos entre actualizaciones se requiere firmar siempre con la misma clave propia.
