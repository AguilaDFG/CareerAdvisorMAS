# CareerAdvisor-MAS

**Sistema multiagente para la recomendación de carreras universitarias basado en intereses personales**

> Práctica de Sistemas Inteligentes — UPM ETSII 2025–26  
> Plataforma: **JADE** (Java Agent DEvelopment Framework)  
> Protocolo de interacción: **FIPA Contract-Net**

---

## Índice

1. [Descripción del sistema](#descripción-del-sistema)
2. [Arquitectura](#arquitectura)
3. [Agentes y mensajes ACL](#agentes-y-mensajes-acl)
4. [Requisitos e instalación](#requisitos-e-instalación)
5. [Ejecución](#ejecución)
6. [Datos de ejemplo](#datos-de-ejemplo)
7. [Declaración de IA](#declaración-de-ia)
8. [Modo de trabajo](#modo-de-trabajo)

---

## Descripción del sistema

CareerAdvisor-MAS analiza los intereses personales del usuario (en lenguaje natural) y recomienda las carreras universitarias más adecuadas. El sistema está construido sobre JADE e implementa el **protocolo FIPA Contract-Net** para que múltiples agentes especializados en distintos dominios de conocimiento compitan para aportar la mejor recomendación.

---

## Arquitectura

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Plataforma JADE                                  │
│  ┌────────────────────┐  REQUEST(intereses+desintereses)                │
│  │  AgentePercepcion  │ ─────────────────────────────────►              │
│  │                    │                                                  │
│  │  • Lee ficheros    │          ┌─────────────────────────┐            │
│  │    intereses.txt   │          │    AgenteCoordinador    │            │
│  │    desintereses.txt│          │  (Contract-Net          │            │
│  │  • Diálogo Swing   │          │   INICIADOR)            │            │
│  └────────────────────┘          └──┬──────────────────────┘            │
│                                     │                                   │
│          CFP (intereses+desintereses)│ → broadcast a todos los KB       │
│            ┌────────────────────────┼──────────────────┐               │
│            ▼                        ▼                   ▼               │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐      │
│  │ AgenteKB         │  │ AgenteKB         │  │ AgenteKB         │      │
│  │ _tecnologia      │  │ _ciencias        │  │ _humanidades     │      │
│  │ (PARTICIPANTE)   │  │ (PARTICIPANTE)   │  │ (PARTICIPANTE)   │      │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘      │
│           │  PROPOSE/REFUSE     │                      │                │
│  ┌──────────────────┐           │                      │               │
│  │ AgenteKB         │           │                      │               │
│  │ _salud           │           │                      │               │
│  └────────┬─────────┘           │                      │               │
│  ┌──────────────────┐           │                      │               │
│  │ AgenteKB         │           │                      │               │
│  │ _arte            │           │                      │               │
│  └────────┬─────────┘           │                      │               │
│           └─────────────────────┘──────────────────────┘               │
│                  PROPOSE(ranking JSON del dominio)                      │
│                             │                                           │
│          AgenteCoordinador  │                                           │
│          • Elige KB ganador │                                           │
│          • ACCEPT_PROPOSAL  → KB ganador  → INFORM (confirmación)      │
│          • REJECT_PROPOSAL  → resto de KB                              │
│          • Agrega ranking global                                        │
│                             │  INFORM(ranking global JSON)             │
│                             ▼                                           │
│                  ┌──────────────────────┐                              │
│                  │  AgenteVisualizacion │                              │
│                  │  • GUI Swing         │                              │
│                  │  • Salida consola    │                              │
│                  └──────────────────────┘                              │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  Directory Facilitator (DF)  — servicios registrados:           │   │
│  │  percepcion-intereses · coordinador-carreras                    │   │
│  │  kb-domain-tecnologia · kb-domain-ciencias · kb-domain-salud    │   │
│  │  kb-domain-humanidades · kb-domain-arte · visualizacion-res.    │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Agentes y mensajes ACL

### Agentes del sistema

| Agente | Rol | Behaviour JADE | Servicio DF |
|--------|-----|---------------|-------------|
| `AgentePercepcion` | Percepción / adquisición | `OneShotBehaviour` | `percepcion-intereses` |
| `AgenteCoordinador` | Coordinación Contract-Net (iniciador) | `Behaviour` (máquina de estados) | `coordinador-carreras` |
| `AgenteKB_tecnologia` | Base de conocimiento — Tecnología | `CyclicBehaviour` | `kb-domain-tecnologia` |
| `AgenteKB_ciencias` | Base de conocimiento — Ciencias | `CyclicBehaviour` | `kb-domain-ciencias` |
| `AgenteKB_humanidades` | Base de conocimiento — Humanidades | `CyclicBehaviour` | `kb-domain-humanidades` |
| `AgenteKB_salud` | Base de conocimiento — Salud | `CyclicBehaviour` | `kb-domain-salud` |
| `AgenteKB_arte` | Base de conocimiento — Arte | `CyclicBehaviour` | `kb-domain-arte` |
| `AgenteVisualizacion` | Visualización / interfaz | `CyclicBehaviour` | `visualizacion-resultados` |

### Flujo de mensajes ACL

```
AgentePercepcion
  │  REQUEST  (ontology=career-advisor, content=intereses||DESINTERESES||desintereses)
  ▼
AgenteCoordinador
  │  CFP  × 5  (broadcast a todos los AgenteKB)
  ▼
AgenteKB_*  →  PROPOSE(ranking JSON del dominio)  o  REFUSE
  ▼
AgenteCoordinador
  │  ACCEPT_PROPOSAL → KB ganador
  │  REJECT_PROPOSAL → resto
  ▼
AgenteKB ganador  →  INFORM (confirmación)
  ▼
AgenteCoordinador
  │  INFORM (ranking global JSON, top-10 carreras)
  ▼
AgenteVisualizacion  →  GUI Swing + consola
```

**Filtros de mensajes bloqueantes** implementados en todos los agentes receptores:

```java
// AgenteCoordinador — espera REQUEST
MessageTemplate.and(MatchPerformative(REQUEST), MatchOntology("career-advisor"))

// AgenteKB — espera CFP / ACCEPT / REJECT
MessageTemplate.and(MatchPerformative(CFP),             MatchOntology("career-advisor"))
MessageTemplate.and(MatchPerformative(ACCEPT_PROPOSAL), MatchOntology("career-advisor"))
MessageTemplate.and(MatchPerformative(REJECT_PROPOSAL), MatchOntology("career-advisor"))

// AgenteVisualizacion — espera INFORM
MessageTemplate.and(MatchPerformative(INFORM), MatchOntology("career-advisor"))
```

Todos usan el patrón `receive(filtro) + block()` para espera eficiente sin polling activo.

---

## Requisitos e instalación

### Dependencias

| Herramienta | Versión mínima | Enlace |
|-------------|---------------|--------|
| Java JDK    | 8             | https://adoptium.net/ |
| JADE        | 4.5           | https://jade.tilab.com/ |
| Eclipse IDE | 2022-06+      | https://www.eclipse.org/ (opcional) |

### Pasos

**1. Clonar el repositorio**
```bash
git clone https://github.com/<usuario>/CareerAdvisorMAS.git
cd CareerAdvisorMAS
```

**2. Obtener jade.jar**

Descarga `jade.jar` desde https://jade.tilab.com/ y colócalo en:
```
CareerAdvisorMAS/lib/jade.jar
```

**3. (Opcional) Importar en Eclipse**

`File → Import → Existing Projects into Workspace` → selecciona la carpeta del proyecto.  
Eclipse detecta `.classpath` y `.project` automáticamente.

---

## Ejecución

### Opción A — Doble clic (recomendada)

| SO | Fichero |
|----|---------|
| Windows | `ejecutar.bat` |
| macOS / Linux | `ejecutar.sh` |

El script compila automáticamente la primera vez y arranca la plataforma JADE.

### Opción B — Eclipse

Clic derecho en `MainLauncher.java` → `Run As → Java Application`  
O bien importa `CareerAdvisorMAS.launch` y ejecuta directamente.

### Opción C — Línea de comandos

```bash
# Compilar
mkdir -p bin
find src -name "*.java" > /tmp/sources.txt
javac -encoding UTF-8 -cp lib/jade.jar -d bin @/tmp/sources.txt

# Ejecutar
java -cp "bin:lib/jade.jar" es.upm.careeradvisor.MainLauncher
# Windows: java -cp "bin;lib/jade.jar" es.upm.careeradvisor.MainLauncher
```

### Flujo de uso

1. Se abre la GUI de JADE (RMA) y se crean los 8 agentes.
2. Si existen `resources/intereses.txt` y `resources/desintereses.txt`, se usan directamente.
3. Si no existen, aparecen **dos diálogos Swing** consecutivos para introducir texto libre.
4. El sistema ejecuta el protocolo Contract-Net entre los 5 agentes KB.
5. Se abre la **ventana de resultados** con el ranking de carreras y sus puntuaciones.

---

## Datos de ejemplo

**`resources/intereses.txt`**
```
Me encanta programar y resolver problemas lógicos con algoritmos.
Disfruto creando aplicaciones web y explorando la inteligencia artificial.
También me gustan las matemáticas, los videojuegos y la seguridad informática.
En mi tiempo libre aprendo sobre redes neuronales y bases de datos.
```

**`resources/desintereses.txt`**
```
No me gusta la biología ni trabajar con pacientes o en hospitales.
Tampoco me atrae el derecho, la política ni los procesos judiciales.
```

**Salida esperada en consola:**
```
╔══════════════════════════════════════════════════════════╗
║        CARREERADVISOR MAS — RANKING GLOBAL               ║
╠══════════════════════════════════════════════════════════╣
║ Intereses:    Me encanta programar y resolver problem...
║ Desintereses: No me gusta la biología ni trabajar co...
╠══════════════════════════════════════════════════════════╣
║ 🥇 Ingeniería Informática       24.1%  +7/-0  [tecnologia]
║ 🥈 Matemáticas                   8.6%  +2/-0  [tecnologia]
║ 🥉 Ing. Telecomunicaciones       6.9%  +2/-0  [tecnologia]
║  4. Diseño Gráfico               3.4%  +1/-0  [arte]
║  5. Administración Empresas      3.4%  +1/-0  [humanidades]
╚══════════════════════════════════════════════════════════╝
```

Otros perfiles de prueba:

| Intereses | Carrera esperada |
|-----------|-----------------|
| `biología, naturaleza, ecosistemas, animales` | Biología (Ciencias) |
| `pintar, dibujar, arte, creatividad, diseño` | Bellas Artes (Arte) |
| `salud, cuidar, pacientes, medicina, urgencias` | Medicina / Enfermería (Salud) |
| `ley, justicia, derechos humanos, argumentar` | Derecho (Humanidades) |

---

## Estructura del proyecto

```
CareerAdvisorMAS/
├── src/es/upm/careeradvisor/
│   ├── MainLauncher.java                  # Punto de entrada JADE
│   ├── agents/
│   │   ├── AgentePercepcion.java          # Percepción (OneShotBehaviour)
│   │   ├── AgenteCoordinador.java         # Contract-Net iniciador
│   │   ├── AgenteKB.java                  # KB especializado (5 instancias)
│   │   └── AgenteVisualizacion.java       # Visualización (CyclicBehaviour)
│   ├── knowledge/
│   │   └── KnowledgeDomain.java           # Base de conocimiento (5 dominios, 20 carreras)
│   └── gui/
│       └── ResultadoGUI.java              # Interfaz Swing con ranking
├── resources/
│   ├── intereses.txt                      # Datos de ejemplo
│   └── desintereses.txt                   # Datos de ejemplo
├── lib/
│   └── jade.jar                           # (añadir manualmente)
├── .classpath / .project                  # Configuración Eclipse
├── CareerAdvisorMAS.launch                # Run config Eclipse
├── ejecutar.bat                           # Lanzador Windows (doble clic)
├── ejecutar.sh                            # Lanzador macOS/Linux (doble clic)
└── README.md
```

---

## Declaración de IA

Durante el desarrollo del proyecto se utilizaron herramientas de IA generativa (principalmente Claude, de Anthropic) como apoyo técnico para distintas tareas de implementación y documentación.
La IA se empleó principalmente para:
 - Apoyo en la implementación de partes del código Java y estructuras base del sistema;
 - Revisión y depuración de errores relacionados con la API de JADE;
 - Generación de ejemplos de documentación y mejora de la organización del README;
 - Discusión de alternativas de diseño para la arquitectura multiagente.
 

El diseño final del sistema, la definición de la arquitectura, la adaptación de los behaviours, la integración entre agentes y la validación del funcionamiento fueron realizados y revisados por el equipo.

La IA se utilizó como herramienta de apoyo y productividad durante el desarrollo, manteniendo en todo momento supervisión y revisión manual sobre el código y las decisiones técnicas incorporadas al proyecto..

---

## Modo de Trabajo

El proyecto se desarrolló de forma colaborativa entre los miembros del grupo, definiendo conjuntamente los objetivos, la arquitectura general y el comportamiento esperado de los agentes..

A lo largo del desarrollo se realizaron pruebas, correcciones y revisiones de forma iterativa, validando el funcionamiento del sistema multiagente sobre JADE y ajustando la comunicación entre agentes, behaviours y servicios registrados en el DF.


Las herramientas de IA generativa se utilizaron como apoyo durante tareas de implementación, documentación y depuración, integrando posteriormente el código en el proyecto tras su revisión y adaptación por parte del equipo..
