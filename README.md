# CareerAdvisor-MAS 🎓

**Sistema multiagente para la recomendación de carreras universitarias basado en intereses personales**

> Práctica de Sistemas Inteligentes — UPM ETSII 2025–26  
> Plataforma: **JADE** (Java Agent DEvelopment Framework)

---

## Índice

1. [Descripción del sistema](#descripción-del-sistema)
2. [Arquitectura](#arquitectura)
3. [Requisitos](#requisitos)
4. [Instalación](#instalación)
5. [Ejecución](#ejecución)
6. [Datos de ejemplo](#datos-de-ejemplo)
7. [Flujo de mensajes ACL](#flujo-de-mensajes-acl)
8. [Estructura del proyecto](#estructura-del-proyecto)
9. [Declaración de IA](#declaración-de-ia)
10. [Modo de Trabajo](#modo-de-trabajo)

---

## Descripción del sistema

CareerAdvisor-MAS es un **sistema multiagente** que analiza los intereses personales del usuario (expresados en lenguaje natural) y recomienda las carreras universitarias o formaciones profesionales más adecuadas. Es una demo con 15 carreras, pero se podrían añadir todas las que se quisieran

El sistema consta de **tres agentes especializados** que colaboran intercambiando mensajes ACL y se localizan dinámicamente a través del **Directory Facilitator (DF)** de JADE. Es un modelo sencillo bassado en palabras clave para la recomendación.

---

## Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                    Plataforma JADE (Main Container)             │
│                                                                 │
│  ┌──────────────────┐   ACL REQUEST    ┌──────────────────────┐ │
│  │                  │ ───────────────► │                      │ │
│  │ AgentePercepcion │                  │  AgenteConocimiento  │ │
│  │                  │                  │                      │ │
│  │  • Lee fichero   │                  │  • Base conocimiento │ │
│  │  • Diálogo Swing │                  │  • Ranking carreras  │ │
│  │  • Busca en DF   │                  │  • Calcula scores    │ │
│  └──────────────────┘                  └──────────┬───────────┘ │
│                                                   │             │
│                                        ACL INFORM │             │
│                                                   ▼             │
│                                        ┌──────────────────────┐ │
│                                        │  AgenteVisualizacion │ │
│                                        │                      │ │
│                                        │  • Parsea resultado  │ │
│                                        │  • GUI Swing         │ │
│                                        │  • Salida consola    │ │
│                                        └──────────────────────┘ │
│                                                                 │
│  ┌───────────┐   ┌───────────────────────────────────────────┐  │
│  │    DF     │   │  Servicios registrados:                   │  │
│  │ (Yellow   │   │  • percepcion-intereses (AgentePercepcion)│  │
│  │  Pages)   │   │  • procesamiento-carreras (AgenteConoc.)  │  │
│  │           │   │  • visualizacion-resultados (AgenteViz.)  │  │
│  └───────────┘   └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Agentes

| Agente | Tipo | Comportamiento JADE | Servicio DF |
|--------|------|---------------------|-------------|
| `AgentePercepcion` | Percepción/Adquisición | `OneShotBehaviour` | `percepcion-intereses` |
| `AgenteConocimiento` | Procesamiento/Inteligencia | `CyclicBehaviour` + filtro bloqueante | `procesamiento-carreras` |
| `AgenteVisualizacion` | Visualización UI | `CyclicBehaviour` + filtro bloqueante | `visualizacion-resultados` |

---

## Requisitos

- **Java** 8 o superior (JDK)
- **JADE** 4.5+ (`jade.jar`) — [descargar en jade.tilab.com](https://jade.tilab.com/)
- **Eclipse IDE** (recomendado) o cualquier IDE Java
- Sistema operativo: Windows / macOS / Linux

---

## Instalación

### 1. Clonar el repositorio

```bash
git clone https://https://github.com/AguilaDFG/CareerAdvisorMAS.git
cd CareerAdvisorMAS
```

### 2. Obtener JADE

Descarga `jade.jar` desde https://jade.tilab.com/ y colócalo en la carpeta `lib/`:

```
CareerAdvisorMAS/
└── lib/
    └── jade.jar      ← aquí
```

### 3. Importar en Eclipse

1. `File → Import → Existing Projects into Workspace`
2. Selecciona la carpeta `CareerAdvisorMAS`
3. Eclipse detectará `.classpath` y `.project` automáticamente
4. Comprueba que `lib/jade.jar` aparece en el Build Path

### 4. Compilar

Eclipse compila automáticamente. Si usas línea de comandos:

```bash
mkdir -p bin
javac -cp lib/jade.jar -d bin \
  src/es/upm/careeradvisor/knowledge/CareerKnowledgeBase.java \
  src/es/upm/careeradvisor/gui/ResultadoGUI.java \
  src/es/upm/careeradvisor/agents/AgenteVisualizacion.java \
  src/es/upm/careeradvisor/agents/AgenteConocimiento.java \
  src/es/upm/careeradvisor/agents/AgentePercepcion.java \
  src/es/upm/careeradvisor/MainLauncher.java
```

---

## Ejecución

### Opción A — Eclipse (recomendado)

1. Clic derecho en `MainLauncher.java` → `Run As → Java Application`
2. O bien importa `CareerAdvisorMAS.launch` y ejecuta directamente

### Opción B — Línea de comandos

```bash
java -cp bin:lib/jade.jar es.upm.careeradvisor.MainLauncher
# Windows:
java -cp "bin;lib/jade.jar" es.upm.careeradvisor.MainLauncher
```

### Opción C — JADE Boot directo

```bash
java -cp "bin;lib/jade.jar" jade.Boot \
  -gui \
  agenteVisualizacion:es.upm.careeradvisor.agents.AgenteVisualizacion \
  agenteConocimiento:es.upm.careeradvisor.agents.AgenteConocimiento \
  agentePercepcion:es.upm.careeradvisor.agents.AgentePercepcion
```

### Flujo de uso

1. Se abre la GUI de JADE (RMA) y se inician los tres agentes.
2. Si existen `resources/intereses.txt` y `resources/desintereses.txt`, se usan sus contenidos directamente.
3. Si no existe, aparece un **diálogo Swing** para introducir tus intereses.
4. Tras unos instantes, se abre la **ventana de resultados** con el ranking de carreras.

---

## Datos de ejemplo

Los ficheros `resources/intereses.txt` y `resources/desintereses.txt` contienen un ejemplo:

```
Me encanta la programación y resolver problemas lógicos con algoritmos.
Disfruto creando aplicaciones web y explorando la inteligencia artificial.
También me gustan las matemáticas, los videojuegos y la seguridad informática.
En mi tiempo libre aprendo sobre redes neuronales y bases de datos.
```

**Resultado esperado:**

```
╔══════════════════════════════════════════════════════════╗
║          CARREERADVISOR MAS — RESULTADOS                 ║
╠══════════════════════════════════════════════════════════╣
║ Intereses: Me encanta la programación y resolver probl...
╠══════════════════════════════════════════════════════════╣
║ 🥇 Ingeniería Informática            24.1%  (7 kw)
║ 🥈 Matemáticas                        8.6%  (2 kw)
║ 🥉 Ingeniería de Telecomunicaciones   6.9%  (2 kw)
║  4. Administración de Empresas (ADE)  3.4%  (1 kw)
║  5. Biología                          3.4%  (1 kw)
╚══════════════════════════════════════════════════════════╝
```

Otros ejemplos de intereses para probar:

- *Medicina*: `"Me apasiona la biología, la salud humana y ayudar a los pacientes"`
- *Arte*: `"Me encanta pintar, dibujar y la fotografía artística"`
- *Derecho*: `"Me interesa la justicia, las leyes y los derechos humanos"`
- *Psicología*: `"Me fascina la mente humana, las emociones y la terapia"`

---

## Flujo de mensajes ACL

```
AgentePercepcion                AgenteConocimiento          AgenteVisualizacion
      │                                 │                            │
      │  [consulta DF: procesamiento]   │                            │
      │ ──────────────────────────────► │ (DF responde con AID)      │
      │                                 │                            │
      │  REQUEST (ontology=career-adv.) │                            │
      │  content: "intereses del user"  │                            │
      │ ──────────────────────────────► │                            │
      │                                 │ [calcula ranking]          │
      │                                 │ [consulta DF: visualiz.]   │
      │                                 │ ──────────────────────────►│(DF responde)
      │                                 │                            │
      │                                 │  INFORM (ontology=career.) │
      │                                 │  content: JSON con ranking │
      │                                 │ ──────────────────────────►│
      │                                 │                            │ [muestra GUI]
```

**Filtros de mensajes implementados:**

- `AgenteConocimiento`: `MessageTemplate.and(MatchPerformative(REQUEST), MatchOntology("career-advisor-ontology"))`
- `AgenteVisualizacion`: `MessageTemplate.and(MatchPerformative(INFORM), MatchOntology("career-advisor-ontology"))`

Ambos usan el patrón `receive(filtro) + block()` para espera eficiente sin polling activo.

---

## Estructura del proyecto

```
CareerAdvisorMAS/
├── src/
│   └── es/upm/careeradvisor/
│       ├── MainLauncher.java                  # Punto de entrada
│       ├── agents/
│       │   ├── AgentePercepcion.java          # Agente de percepción
│       │   ├── AgenteConocimiento.java        # Agente de procesamiento
│       │   └── AgenteVisualizacion.java       # Agente de visualización
│       ├── knowledge/
│       │   └── CareerKnowledgeBase.java       # Base de conocimiento (15 carreras)
│       └── gui/
│           └── ResultadoGUI.java              # Interfaz gráfica Swing
├── resources/
│   └── intereses.txt                          # Datos de ejemplo
│   └── desintereses.txt                       # Datos de ejemplo
├── lib/
│   └── jade.jar                               # (añadir manualmente)
├── .classpath                                 # Configuración Eclipse
├── .project                                   # Proyecto Eclipse
├── CareerAdvisorMAS.launch                    # Configuración de ejecución
└── README.md
```

---

## Declaración de IA

Este proyecto ha utilizado herramientas de inteligencia artificial (Claude Sonnet, Anthropic) como apoyo en las siguientes tareas:

- **Generación de código base**: Los ficheros Java han sido generados con asistencia de IA a partir de la descripción del sistema y los requisitos de la práctica.
- **Revisión y depuración**: La IA ha ayudado a identificar patrones de diseño adecuados para JADE (comportamientos, filtros de mensajes, registro en DF).
- **Documentación**: El README ha sido redactado con apoyo de IA.

Todo el código ha sido revisado, comprendido y adaptado por el equipo humano. La IA se ha usado como herramienta de apoyo, no como sustituto del aprendizaje.

---

## Modo de Trabajo

El trabajo se ha realizado principalmente en llamada de manera simultánea. Así se han detallado los objetivos del sistema y el flujo de agentes. Una vez definidos, se ha usado Claude para generar el código del proyecto y se ha revisado su funcionalidad en grupo. Por último, se ha procedido a preparar la presentación.