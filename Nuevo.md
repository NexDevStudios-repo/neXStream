# plan.md — neXStream (Nex Dev Studios) · v3
## Consolidación a modo único: **IPTV Xtream**

> **Contexto:** la app **ya está hecha** y hoy tiene **dos modos** (Plugins/Addons y Xtream). No se empieza de cero.
> **Objetivo:** dejar **un solo modo, IPTV Xtream**, y que toda la app lo refleje como si siempre hubiera sido su única función **nativa**.
> Esto implica quitar **la lógica, los datos, las dependencias, los textos, las referencias, la documentación y los recursos** del modo Plugins/Addons, y quitar también **toda la infraestructura de "modos"** que ya no tiene razón de existir.

---

## 0. REGLAS DE ORO (el agente debe cumplirlas en TODA tarea)

### 0.1 Antes de empezar cualquier tarea
1. Leer las skills de `.agents/` y aplicar las que correspondan.
2. Consultar `referencias/` (apps Xtream) como guía de cómo se ve y se comporta una app Xtream nativa (terminología, flujos, estados vacíos, errores).
3. Los iconos salen **solo** de `iconos nuevos/`.
4. Hacer un respaldo antes de empezar: rama/tag `pre-single-mode` en el repositorio.

### 0.2 Prohibiciones de diseño (JAMÁS)
- **No cambiar** diseño, estilos, colores, tipografías, espaciados, animaciones ni navegación.
- **No crear componentes nuevos** si hay uno reutilizable. Si se elimina una pantalla o sección, lo que quede se reorganiza con los componentes existentes.
- **No inventar** pantallas, banners, textos promocionales, onboarding ni funciones nuevas. Este trabajo es de **sustracción y limpieza**, no de agregar.
- No agregar switches/selectores en Home ni en ninguna pantalla.
- No cambiar la arquitectura de la app salvo lo necesario para quitar el modo Plugins.

### 0.3 Regla de limpieza total ("como si nunca hubiera existido")
Lo que se elimina se elimina **completo**:
- Sin referencias colgadas: imports, llamadas, DI, rutas de navegación, entradas de Ajustes, strings (**todos los idiomas**), drawables, permisos, dependencias de Gradle, ProGuard, tests, documentación.
- Prohibido dejar `TODO()`, funciones vacías, stubs, código comentado, `@Suppress` para esconder referencias muertas, ni "lo dejo por si acaso".
- El proyecto debe **compilar después de cada paso**. Nunca se acumulan errores para el final.

**Protocolo de borrado (de afuera hacia adentro):**
1. **Puntos de entrada**: pantallas, botones, filas de Ajustes, rutas de navegación, deep links.
2. **ViewModels / estado / casos de uso**.
3. **Repositorios y lógica de negocio**.
4. **Red / API / DTOs / mappers**.
5. **Persistencia** (tablas, DAOs, claves de preferencias/DataStore, archivos en disco).
6. **DI**: módulos, providers, bindings.
7. **Recursos**: strings en todos los `values-xx`, drawables, estilos.
8. **Build**: dependencias, plugins, `BuildConfig`, ABIs/libs nativas, ProGuard, empaquetado.
9. **Verificar**: búsqueda global de palabras clave = 0 resultados · `./gradlew :androidApp:assembleFullDebug` verde · lint sin "unused/unresolved".
10. Commit: `fase-N: eliminado <pieza>`.

### 0.4 Cuidado con los falsos positivos
Algunos nombres genéricos (`Stream`, `Meta`, `Catalog`, `Provider`, `Source`) pueden estar **reutilizados por el modo Xtream** a través de los adaptadores. **No se borra por nombre: se borra por alcanzabilidad.** Una pieza se elimina solo si, tras quitar el modo Plugins, **ya nada del flujo Xtream la usa** (verificar con "Find usages" del IDE). Si un tipo compartido se queda, se limpia de campos y semántica propios de Plugins (id de addon, manifest, behavior hints, etc.) cuando ya nada los lea.

---

## 1. Estado esperado de partida (verificar, no asumir)

La app debería traer ya resuelto lo definido en versiones anteriores del plan. **La Fase 1 lo verifica** y, si algo falta, se completa con las mismas reglas:

| Ítem | Esperado | Verificar |
|---|---|---|
| Solo Android (sin iOS) | ✔ | Sin `iosApp`, `iosMain`, `MPVKit`, `store.json`, targets iOS |
| 100% local (sin Supabase/backend) | ✔ | Sin `supabase`, `postgrest`, `gotrue`, `nuvio.tv` en `src/` |
| Rebranding neXStream / Nex Dev Studios | ✔ | `grep -ri nuvio` solo en `LICENSE`/`NOTICE.md` |
| Iconos nuevos | ✔ | Sin assets antiguos de Nuvio |
| Modo Tablet = experiencia TV (detección de TV, D-pad, launcher leanback) | ✔ | Funciona en emulador Android TV |
| Modo Xtream completo (live+EPG, películas, series, player) | ✔ | Funciona de punta a punta |

---

## 2. Visión técnica (cómo lo haría un programador experto)

Hoy el diseño es:

```
UI → Repositorios → [ dispatcher por SourceMode ] ─┬─ PLUGINS (addons, catálogos, scrapers, streams…)
                                                   └─ XTREAM  (API + mappers → modelos de la UI)
```

Debe quedar:

```
UI → Repositorios → Xtream (API + mappers → modelos de la UI)
```

Pasos conceptuales:
1. **Congelar el comportamiento Xtream** con pruebas (que sirvan de red de seguridad).
2. **Análisis de alcanzabilidad**: marcar qué código es *solo Plugins*, cuál es *compartido* y cuál es *solo Xtream*.
3. **Quitar la capa de modos**: el enum/estado `SourceMode`, el dispatcher, el selector de modo en Ajustes, el diálogo "¿qué tipo de fuente?" y las claves de modo en el almacenamiento. Con un solo modo, **nada de esto tiene sentido**.
4. **Eliminar el modo Plugins** capa por capa (protocolo 0.3).
5. **Simplificar** los repositorios: llamar directo a la implementación Xtream; borrar interfaces/abstracciones que solo existían para soportar dos modos (si no las usan tests o DI que se conserven).
6. **Purgar textos y recursos** y reescribir la terminología para que la app hable "Xtream nativo" (sección 6).
7. **Limpiar build, manifest y documentación**.
8. **Guardas automáticas** (lint/CI) que impidan que vuelva a colarse algo del modo anterior.

---

## 3. Qué se elimina (alcance de la sustracción)

**Todo lo que solo servía al modo Plugins/Addons**, incluyendo (a confirmar con la auditoría):

| Área | Ejemplos a buscar |
|---|---|
| Sistema de addons/plugins | instalación por URL de manifest, gestión de addons, `manifest`, catálogos de addons, repositorios de plugins, motor de plugins/scrapers y su runtime (p. ej. motores JS embebidos) |
| Resolución de streams | selector de streams/fuentes, pantalla/sheet "elegir enlace", ranking/filtrado de streams, `behaviorHints`, proveedores |
| Debrid | Real-Debrid, AllDebrid, Premiumize, TorBox, etc. (login, API, ajustes, UI) |
| Torrents | motor de torrents, `magnet:`, servicios y notificaciones asociadas, binarios nativos |
| Stremio | esquemas/deep links tipo `stremio://`, intent-filters de manifests, textos y enlaces a Stremio |
| Metadatos/servicios solo para Plugins | TMDB, Trakt, Simkl, MDBList, etc. **si solo alimentaban el modo Plugins** (ver decisiones, sección 10) |
| Infraestructura de modos | `SourceMode`, dispatcher, selector de modo, diálogo de tipo de fuente, claves de modo/fuente en storage |
| Persistencia de Plugins | tablas/DAOs/DataStore/archivos de addons, plugins, catálogos y caché de streams |
| Recursos | strings, drawables, ilustraciones y estados vacíos propios de addons/plugins |
| Build | dependencias exclusivas, libs nativas/ABIs, `vendor/` si solo servía a Plugins, ProGuard, permisos y servicios en primer plano de torrents |
| Documentación | README, CHANGELOG, ficha de tienda, FAQ/ayuda in-app, capturas |
| Tests | tests de todo lo borrado |

**Se conserva** todo lo que usa el flujo Xtream, el player, el modo Tablet/TV, los ajustes generales y el branding.

---

## 4. Fases

### Fase 1 — Auditoría y red de seguridad (sin borrar nada)
1. Crear `pre-single-mode` (tag/rama de respaldo).
2. **Verificar el estado de partida** (tabla de la sección 1) y listar lo que falte.
3. Generar `docs/AUDIT_SINGLE_MODE.md` con el **análisis de alcanzabilidad**:
   - **A. Solo Plugins** (se elimina): archivo/clase, qué hace, quién la llama.
   - **B. Compartido** (se conserva y se limpia): tipos/servicios que usa Xtream y también Plugins; qué campos/semántica de Plugins hay que quitar.
   - **C. Solo Xtream** (se conserva).
   - **D. Infraestructura de modos** (se elimina o simplifica).
4. **Inventario por palabras clave** (en `src/`, `res/` de todos los idiomas, manifests, Gradle, docs, CI): `addon`, `addons`, `plugin`, `plugins`, `complemento`, `complementos`, `stremio`, `manifest`, `scraper`, `torrent`, `magnet`, `debrid`, `realdebrid`, `alldebrid`, `premiumize`, `torbox`, `provider`, `resolver`, `behaviorHints`, `SourceMode`, `mode`, nombre del motor de plugins/torrents, y los servicios de terceros detectados. Para cada hallazgo: clasificar A/B/C/D.
5. **Inventario de UI por pantalla** (Home, Búsqueda, Detalle, Biblioteca, Player, Ajustes, Onboarding/estado vacío, Acerca de, diálogos, notificaciones, widgets/shortcuts) con lo que muestra hoy del modo Plugins.
6. **Inventario de persistencia**: tablas, claves y archivos de Plugins; claves de modo/fuente; plan de migración (ver decisión en sección 10).
7. **Inventario de build**: dependencias, libs nativas, ABIs, `vendor/`, ProGuard, permisos, servicios, intent-filters.
8. **Pruebas de caracterización de Xtream**: asegurar que existen pruebas (o crearlas) de los flujos Xtream críticos: alta de servidor, carga de categorías, live+EPG, película, serie, reproducción, favoritos/progreso. Deben pasar **antes y después** de cada fase.
9. Medir línea base: tamaño del APK, nº de dependencias, tiempo de compilación.

**Salida:** `docs/AUDIT_SINGLE_MODE.md`. **Sin cambios de código** salvo las pruebas de caracterización.

---

### Fase 2 — Colapsar la infraestructura de modos
Objetivo: la app deja de tener el concepto de "modo". Xtream pasa a ser **el** comportamiento.

- Eliminar `SourceMode` (enum/estado/flujo) y su persistencia.
- Eliminar el **dispatcher** por modo en repositorios: los repositorios llaman directo a Xtream.
- Eliminar de **Ajustes** la fila/selector de **"Modo de fuente"**.
- Eliminar el diálogo/paso **"¿Plugins o Xtream?"** al agregar fuente: el botón de agregar lleva **directo al formulario Xtream** (URL, usuario, contraseña, nombre opcional) con los componentes ya existentes.
- Quitar las **claves de modo** del almacenamiento: favoritos, progreso, historial, caché y búsquedas quedan ligados solo a la **fuente/servidor Xtream** (mantener ese aislamiento por servidor si ya existe y el usuario puede tener varios).
- Eliminar el reinicio/recarga "por cambio de modo" y cualquier lógica de transición.
- Simplificar interfaces/abstracciones que existían solo por tener dos implementaciones.

**Verificación:** `grep -ri "SourceMode\|modo de fuente\|source mode"` = 0 · build verde · pruebas de Xtream verdes.

---

### Fase 3 — Eliminar el modo Plugins/Addons (lógica completa)
Aplicar el protocolo 0.3 a cada pieza **clase A** de la auditoría, **en este orden** para que siempre compile:

1. **UI y navegación**: pantallas de addons/plugins, catálogos de addons, selector de streams/enlaces, flujos de instalación por URL/manifest, entradas en menús y Ajustes, deep links.
2. **Estado y casos de uso** asociados.
3. **Repositorios y servicios**: gestión de addons, resolución de streams, catálogos remotos.
4. **Motor de plugins/scrapers** y su runtime.
5. **Red**: clientes, DTOs, parsers de manifest/catálogo/stream.
6. **Persistencia** de Plugins (tablas, DAOs, claves, archivos en disco).
7. **DI**: módulos y bindings.
8. **Tests** de lo eliminado.

En cada paso: compilar, correr pruebas Xtream, commit.

---

### Fase 4 — Eliminar funciones satélite que solo vivían por Plugins
- **Debrid** (todos los proveedores): login, API, ajustes, UI, almacenamiento.
- **Torrents**: motor, `magnet:`, servicios, notificaciones, binarios nativos, permisos de primer plano asociados.
- **Servicios de terceros** que solo alimentaban Plugins (TMDB/Trakt/Simkl/MDBList u otros, según decisión de la sección 10): API, ajustes, login/tokens, UI, persistencia.
- **Integraciones de sistema** de Stremio: esquemas `stremio://`, intent-filters de manifests, handlers de `magnet:`.
- Componentes de UI que quedaron **huérfanos** (sin usos): eliminarlos. Si se usan en otro lado, se conservan.

**Verificación:** análisis de "código sin usar" (lint / inspección del IDE / `unused` de Gradle) sin hallazgos relacionados.

---

### Fase 5 — Limpieza de datos locales
- Eliminar todo rastro de Plugins en la base de datos y preferencias.
- **Migración según decisión (sección 10):**
  - *Si la app aún no se distribuyó:* permitir recreación destructiva del esquema y limpiar claves obsoletas; documentarlo.
  - *Si hay instalaciones existentes:* migración que **borra** tablas/claves/archivos de Plugins y **conserva** servidores Xtream, favoritos, progreso e historial.
- Quitar directorios de caché/archivos en disco de addons, plugins y streams.
- Pruebas de migración (instalar versión anterior → actualizar → verificar que Xtream sigue intacto y no quedan restos).

---

### Fase 6 — Purga de textos, recursos y terminología ("Xtream nativo")
La app debe **hablar como una app IPTV Xtream desde el origen**. Nada de "addon", "plugin", "complemento", "Stremio", "manifest", "scraper", "debrid", "torrent" en ningún texto visible.

**6.1 Glosario de terminología (ajustar con lo que ya use la app y `referencias/`)**

| Concepto antiguo | Concepto nuevo (nativo Xtream) |
|---|---|
| Addon / Plugin / Complemento | *(eliminado)* |
| Fuente / Source | **Servidor** (o **Cuenta IPTV**; elegir uno y usarlo en toda la app) |
| Catálogo | **Categoría** |
| Streams / Enlaces / Proveedores | *(eliminado: reproducción directa)* |
| Instalar addon por URL | **Agregar servidor** (URL, usuario, contraseña) |
| Gestionar addons | **Gestionar servidores** |
| Tipos de contenido | **TV en vivo · Películas · Series** |
| Estado vacío "sin addons" | **"Sin servidores"** con la acción de agregar uno (mismo componente de estado vacío) |

**6.2 Tareas**
- **Strings**: eliminar las claves de Plugins/Addons/Debrid/Torrents/Stremio en **todos** los `values-xx`. Reescribir las claves compartidas con la terminología nueva en **todos los idiomas existentes** (traducir correctamente; no dejar mezclas ni claves huérfanas). Si falta traducción confiable en algún idioma, dejar el fallback en el idioma base y **anotarlo** en `docs/I18N_TODO.md`.
- **Pantallas**: revisar una por una que no queden etiquetas, títulos, descripciones, hints, mensajes de error, tooltips, diálogos, notificaciones, "Acerca de", créditos ni onboarding con lenguaje de Plugins.
- **Mensajes de error**: los de Xtream (credenciales inválidas, cuenta expirada, límite de conexiones, servidor caído, timeout) con los componentes de error ya existentes.
- **Recursos gráficos**: eliminar ilustraciones/íconos propios de addons/plugins/debrid/torrents.
- **Código interno**: renombrar clases/paquetes/variables con nombres propios del modo anterior **solo si** (a) son claramente de Plugins/Stremio, (b) quedan en uso por Xtream y (c) el rename es seguro con las herramientas de refactor del IDE. Si no, no se toca (evitar churn innecesario). Documentar lo decidido.
- **Atribución legal** (`LICENSE`, `NOTICE.md`) se conserva intacta: es obligación GPL-3.0, no "referencia al modo".

**Verificación:** `grep -ri` de las palabras clave del inventario = 0 en `src/`, `res/` (todos los idiomas), manifests, Gradle y docs, **excepto** `LICENSE`/`NOTICE.md`.

---

### Fase 7 — Reorganizar UI con lo que ya existe (sin diseñar nada nuevo)
Con las secciones de Plugins fuera, cada pantalla queda coherente usando **solo los componentes existentes**:

| Pantalla | Resultado esperado |
|---|---|
| **Home** | Continuar viendo + categorías del servidor activo (live, películas, series). Sin switches |
| **Búsqueda** | Busca en canales, películas y series del servidor activo |
| **Detalle** | Película/serie/canal con la info del panel; botón de reproducir directo (sin selector de streams) |
| **Biblioteca / favoritos** | Favoritos, historial y progreso del servidor |
| **Player** | Live (zapping, EPG, volver al vivo) y VOD/series; sin selector de fuentes/streams, sin debrid, sin torrent |
| **Ajustes** | Servidores (agregar/editar/eliminar/activar), reproducción, EPG, caché, idioma, tema, Acerca de. **Sin** modos, addons, debrid ni torrents |
| **Estado vacío / primer uso** | "Sin servidores" + acción directa de agregar servidor Xtream |

Reglas:
- Secciones de Ajustes que quedan vacías o con un solo ítem: **no crear estructura nueva**; reubicar el ítem donde ya haya un patrón equivalente.
- La navegación y el orden de pestañas no cambian salvo que una pestaña haya sido **exclusiva** de Plugins (en ese caso se elimina y no se reemplaza).
- Verificar el modo Tablet/TV: foco D-pad sigue siendo coherente tras quitar elementos (sin "huecos" de foco ni destinos de foco inexistentes).

---

### Fase 8 — Build, manifest y dependencias
- **Gradle**: quitar dependencias exclusivas de Plugins/Debrid/Torrents/terceros eliminados; limpiar `libs.versions.toml`; eliminar plugins de build que ya no se usen.
- **Nativos**: quitar `.so`/ABIs/`jniLibs`/`vendor/` exclusivos; ajustar `abiFilters`/splits si corresponde.
- **Manifest**: quitar intent-filters (`stremio://`, `magnet:`, manifests por http), servicios en primer plano de torrents, permisos que ya no se usen. Dejar `INTERNET`, estado de red y lo que el player necesite. Mantener leanback/launcher de TV.
- **ProGuard/R8**: eliminar reglas de librerías removidas.
- **BuildConfig / flags / secretos**: eliminar los de servicios borrados (también en CI).
- **Medición**: comparar con la línea base de la Fase 1 (tamaño del APK, nº de dependencias, tiempo de build); debe haber **reducción**. Revisar con el APK Analyzer que no queden clases/paquetes del motor de plugins/torrents.

---

### Fase 9 — Documentación y branding final
- `README.md`, `CHANGELOG.md`, ficha de tienda y cualquier ayuda/FAQ in-app describen **solo** una app IPTV Xtream. Sin menciones a addons, plugins, Stremio, debrid ni torrents.
- Actualizar `docs/` (auditorías, notas) para que reflejen el estado final.
- Mantener `LICENSE` (GPL-3.0) y `NOTICE.md` con la atribución a Nuvio y *"© Nex Dev Studios"*.
- Confirmar nombre, estudio e iconos nuevos en todas las superficies.

---

### Fase 10 — Guardas automáticas y QA final
**Guardas (CI + lint):**
- Un chequeo que **falle el build** si aparecen en `src/`, `res/`, manifests, Gradle o docs las palabras clave prohibidas (lista del inventario), con lista de excepciones limitada a `LICENSE` y `NOTICE.md`.
- Lint/inspección de **código sin usar** y **recursos sin usar** como paso obligatorio.

**Pruebas:**
- Unit/integración: flujos Xtream (alta de servidor, categorías, live+EPG, VOD, series, favoritos, progreso), migración de datos, aislamiento por servidor.
- **Instalación limpia**: abrir → estado vacío "Sin servidores" → agregar servidor Xtream → Home poblada → reproducir.
- **Actualización** desde una build con dos modos (si aplica): sin crashes ni restos.
- Dispositivos: teléfono, tablet y TV (solo D-pad en TV).
- **Prueba de tráfico** (proxy): solo hay conexiones al servidor Xtream del usuario (y sus imágenes/EPG); nada hacia servicios de addons, debrid, torrents, Supabase ni Nuvio.
- Recorrido pantalla por pantalla leyendo **todos los textos** (en cada idioma soportado) para confirmar que nada habla de Plugins.
- Rendimiento con listas grandes (≥ 50k ítems).

---

## 5. Checklist de limpieza del modo Plugins
- [ ] `SourceMode`, dispatcher y selector de modo eliminados
- [ ] Diálogo "¿Plugins o Xtream?" eliminado; agregar fuente va directo a Xtream
- [ ] Addons/plugins: UI, estado, repos, red, persistencia, DI y tests eliminados
- [ ] Selector de streams/enlaces eliminado (reproducción directa)
- [ ] Debrid eliminado
- [ ] Torrents y binarios nativos eliminados
- [ ] Deep links `stremio://` y `magnet:` eliminados
- [ ] Servicios de terceros exclusivos de Plugins eliminados
- [ ] Strings de Plugins eliminados en **todos** los idiomas
- [ ] Terminología Xtream aplicada en toda la app
- [ ] Dependencias, libs nativas, ProGuard, permisos y servicios limpios
- [ ] README/CHANGELOG/ficha de tienda sin referencias al modo anterior
- [ ] Guardas de CI activas

---

## 6. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Borrar algo que Xtream sí usa | Borrado por alcanzabilidad (0.4), pruebas de caracterización antes/después, commits pequeños |
| Código acoplado entre modos | Colapsar primero la infraestructura de modos (Fase 2) y luego borrar de afuera hacia adentro |
| Referencias o textos huérfanos | Inventario por palabras clave + guardas de CI + lectura de pantallas en todos los idiomas |
| Datos viejos en instalaciones existentes | Migración que limpia Plugins y conserva Xtream (Fase 5) |
| Foco D-pad roto al quitar elementos | Probar en TV al terminar cada fase de UI |
| La IA inventa UI para "rellenar" huecos | Reglas 0.2: solo sustracción y reutilización |
| Falsos positivos por nombres genéricos | No borrar por nombre; analizar usos (0.4) |

---

## 7. Definición de "terminado"
- [ ] La app tiene **un único modo: IPTV Xtream**, sin rastro del concepto de "modo".
- [ ] Al abrir por primera vez: **"Sin servidores"** → agregar servidor Xtream directo.
- [ ] Cero lógica, datos, dependencias, recursos y textos de Plugins/Addons/Debrid/Torrents/Stremio.
- [ ] La app se siente **nativa Xtream**: terminología consistente en todas las pantallas e idiomas.
- [ ] UI, colores, estilos y componentes sin cambios (solo se quitó lo exclusivo de Plugins).
- [ ] Build limpio: sin errores, sin referencias muertas, sin `TODO()`, sin stubs.
- [ ] APK más liviano y con menos dependencias que la línea base.
- [ ] Funciona en teléfono, tablet y TV (modo Tablet con control remoto).
- [ ] Prueba de tráfico limpia.
- [ ] Documentación y ficha de tienda coherentes con una app IPTV Xtream.
- [ ] `LICENSE` y `NOTICE.md` intactos.
- [ ] Guardas de CI activas.

---

## 8. Mapa por fases (resumen)

| Fase | Qué se hace | Salida |
|---|---|---|
| 1 | Auditoría + red de seguridad | `AUDIT_SINGLE_MODE.md` + pruebas Xtream |
| 2 | Quitar infraestructura de modos | Un solo flujo, agregar fuente → Xtream directo |
| 3 | Eliminar lógica Plugins/Addons | Código A fuera |
| 4 | Eliminar debrid, torrents, terceros, deep links | Satélites fuera |
| 5 | Limpiar datos y migrar | Sin restos en storage |
| 6 | Purga de textos y terminología | App "Xtream nativa" |
| 7 | Reorganizar UI con lo existente | Pantallas coherentes |
| 8 | Build, manifest, dependencias | APK más liviano |
| 9 | Documentación y branding | Docs coherentes |
| 10 | Guardas y QA final | CI + pruebas |

---

## 9. Decisiones pendientes (confirmar antes de la Fase 2)

1. **Servicios de terceros** (TMDB, Trakt, Simkl, MDBList, etc.): *Recomendado:* eliminar todo lo que solo servía a Plugins; conservar TMDB **solo** si el modo Xtream ya lo usa para enriquecer metadatos (y sin backend propio).
2. **Migración de datos:** ¿la app ya se distribuyó/instaló en dispositivos reales? Si **no** → recreación destructiva del esquema; si **sí** → migración que conserva Xtream.
3. **Término visible:** **"Servidor"** o **"Cuenta IPTV"** (elegir uno y aplicarlo a toda la app y todos los idiomas).
4. **Renombrado interno** de clases/paquetes de Stremio que Xtream reutiliza: *Recomendado:* solo si es seguro con el refactor del IDE; si no, dejar el nombre.
5. **Idiomas:** ¿se mantienen todos los idiomas actuales (con traducción correcta) o se limita a los que se puedan traducir bien?
6. **Varios servidores Xtream:** *Recomendado:* conservar la capacidad existente (lista de servidores, uno activo).

---

## 10. Prompt maestro para el IDE (copiar y pegar)

```text
Lee plan.md completo y síguelo fase por fase. Antes de cada tarea revisa las
skills en .agents/ y las apps de ejemplo en referencias/ (guía de cómo se ve
una app Xtream nativa). Los iconos finales están en "iconos nuevos/".

Situación: neXStream (Nex Dev Studios) YA está construida sobre NuvioMobile y
hoy tiene DOS modos: Plugins/Addons y Xtream. NO empezamos de cero. Vamos a
dejar UN SOLO modo: IPTV Xtream, y a eliminar por completo el modo
Plugins/Addons: su lógica, datos, dependencias, recursos, textos, referencias
y documentación, junto con toda la infraestructura de "modos" (SourceMode,
dispatcher, selector en Ajustes, diálogo de tipo de fuente). La app debe
quedar como si Xtream fuera su única función nativa.

Reglas innegociables:
- No cambies diseño, colores, estilos, tema ni navegación.
- No inventes componentes ni pantallas: este trabajo es de SUSTRACCIÓN y
  limpieza; reutiliza lo existente.
- Borra por alcanzabilidad, no por nombre: no elimines lo que el flujo Xtream
  siga usando (verifica con Find usages).
- Borra de afuera hacia adentro (UI/navegación → ViewModels → repos → red →
  persistencia → DI → recursos → Gradle), completo y sin referencias colgadas.
  Sin TODO(), sin stubs, sin código comentado. Compila y corre las pruebas de
  Xtream después de cada paso. Commit pequeño por pieza.
- Elimina las claves de Plugins en TODOS los idiomas y aplica terminología
  Xtream nativa (Servidor, Categoría, TV en vivo, Películas, Series).
- Agregar fuente va directo al formulario Xtream (sin preguntar tipo).
- Mantén LICENSE y NOTICE.md (atribución GPL-3.0).
- Verifica que el modo Tablet/TV con D-pad sigue funcionando.

Empieza por la Fase 1: crea el tag pre-single-mode, verifica el estado de
partida, genera docs/AUDIT_SINGLE_MODE.md (alcanzabilidad A/B/C/D, inventario
por palabras clave, UI por pantalla, persistencia, build) y crea las pruebas de
caracterización de Xtream. NO borres código todavía. Al terminar, propón el
detalle de la Fase 2.
```