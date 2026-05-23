package es.upm.careeradvisor.knowledge;

import java.util.*;

/**
 * Base de conocimiento distribuida entre los agentes KB especializados.
 *
 * Cada dominio (Tecnología, Ciencias, Humanidades, Salud, Arte) tiene
 * su propio conjunto de carreras con palabras clave asociadas.
 *
 * Esta clase sólo define las estructuras de datos y la lógica de scoring;
 * cada AgenteKB instancia únicamente su dominio correspondiente.
 */
public class KnowledgeDomain {

    // ------------------------------------------------------------------ //
    //  Estructura de datos
    // ------------------------------------------------------------------ //

    public static class CareerEntry {
        public final String name;
        public final String description;
        public final String emoji;
        public final List<String> keywords;

        public CareerEntry(String name, String description, String emoji, String... keywords) {
            this.name        = name;
            this.description = description;
            this.emoji       = emoji;
            this.keywords    = Collections.unmodifiableList(Arrays.asList(keywords));
        }
    }

    public static class ScoredCareer {
        public final CareerEntry career;
        public final double score;           // neta: positivos - negativos (normalizado)
        public final long   positive;
        public final long   negative;

        public ScoredCareer(CareerEntry career, double score, long positive, long negative) {
            this.career   = career;
            this.score    = score;
            this.positive = positive;
            this.negative = negative;
        }
    }

    // ------------------------------------------------------------------ //
    //  Dominios
    // ------------------------------------------------------------------ //

    public enum Domain { TECNOLOGIA, CIENCIAS, HUMANIDADES, SALUD, ARTE }

    private static final Map<Domain, List<CareerEntry>> DOMAINS = new EnumMap<>(Domain.class);

    static {
        // ── TECNOLOGÍA ──────────────────────────────────────────────────
        DOMAINS.put(Domain.TECNOLOGIA, Arrays.asList(
            new CareerEntry("Ingeniería Informática",
                "Diseño y desarrollo de software, sistemas y aplicaciones tecnológicas.", "💻",
                "programación", "código", "software", "ordenador", "algoritmo", "web",
                "aplicación", "app", "tecnología", "digital", "datos", "base de datos",
                "inteligencia artificial", "ia", "robot", "automatización", "videojuego",
                "hacking", "seguridad", "red", "internet", "sistema", "desarrollo",
                "java", "python", "matemáticas", "lógica", "problema", "innovación"),
            new CareerEntry("Ingeniería de Telecomunicaciones",
                "Diseño de sistemas de transmisión de información y redes de comunicación.", "📡",
                "telecomunicaciones", "señal", "antena", "red", "internet", "fibra óptica",
                "satélite", "móvil", "5g", "electrónica", "radio", "frecuencia", "protocolo",
                "router", "hardware", "física", "ondas", "transmisión", "datos", "iot"),
            new CareerEntry("Ingeniería Industrial",
                "Optimización de procesos productivos y sistemas industriales.", "⚙️",
                "industria", "producción", "fábrica", "proceso", "máquina", "automatización",
                "robótica", "eficiencia", "logística", "manufactura", "calidad", "energía",
                "mecánica", "eléctrica", "gestión", "proyecto", "ingeniería", "optimización"),
            new CareerEntry("Matemáticas",
                "Estudio de estructuras abstractas, números, geometría y análisis.", "📐",
                "matemáticas", "número", "ecuación", "álgebra", "cálculo", "geometría",
                "estadística", "probabilidad", "lógica", "demostración", "teorema",
                "función", "análisis", "abstracción", "modelo", "simulación",
                "criptografía", "física", "economía")
        ));

        // ── CIENCIAS ─────────────────────────────────────────────────────
        DOMAINS.put(Domain.CIENCIAS, Arrays.asList(
            new CareerEntry("Biología",
                "Estudio de los seres vivos, sus procesos y relación con el entorno.", "🔬",
                "biología", "naturaleza", "animal", "planta", "ecosistema", "célula",
                "genética", "evolución", "especie", "medio ambiente", "laboratorio",
                "microscopio", "investigación", "experimento", "conservación",
                "flora", "fauna", "microbiología", "biotecnología", "adn"),
            new CareerEntry("Química",
                "Estudio de la materia, sus propiedades y transformaciones.", "⚗️",
                "química", "molécula", "átomo", "reacción", "laboratorio", "experimento",
                "compuesto", "elemento", "orgánica", "inorgánica", "síntesis",
                "farmacia", "materiales", "industria", "análisis", "investigación"),
            new CareerEntry("Física",
                "Estudio de las leyes que rigen el universo y la materia.", "🌌",
                "física", "energía", "fuerza", "movimiento", "onda", "campo",
                "relatividad", "cuántica", "partícula", "universo", "astronomía",
                "laboratorio", "experimento", "matemáticas", "modelo", "simulación"),
            new CareerEntry("Ciencias Ambientales",
                "Análisis y gestión del medio ambiente y la sostenibilidad.", "🌿",
                "medio ambiente", "sostenibilidad", "ecología", "contaminación",
                "clima", "cambio climático", "naturaleza", "recursos", "biodiversidad",
                "reciclaje", "energía renovable", "agua", "suelo", "bosque")
        ));

        // ── HUMANIDADES ──────────────────────────────────────────────────
        DOMAINS.put(Domain.HUMANIDADES, Arrays.asList(
            new CareerEntry("Derecho",
                "Estudio e interpretación de las leyes y normas que rigen la sociedad.", "⚖️",
                "ley", "derecho", "justicia", "abogado", "tribunal", "juicio", "norma",
                "contrato", "constitución", "político", "sociedad", "conflicto", "defensa",
                "legislación", "gobierno", "estado", "derechos humanos",
                "ética", "argumentación", "debate", "negociación"),
            new CareerEntry("Periodismo y Comunicación",
                "Producción de contenido informativo y gestión de medios de comunicación.", "📰",
                "periodismo", "noticia", "comunicación", "medios", "radio", "televisión",
                "escribir", "redacción", "reportaje", "entrevista", "información",
                "redes sociales", "fotografía", "vídeo", "prensa", "digital",
                "lenguaje", "narrar", "contar historias", "política", "actualidad"),
            new CareerEntry("Educación / Magisterio",
                "Formación y enseñanza a niños, jóvenes y adultos.", "📚",
                "enseñar", "educación", "niños", "profesor", "maestro", "escuela",
                "aprendizaje", "pedagogía", "infancia", "didáctica",
                "motivar", "aula", "alumno", "conocimiento", "paciencia",
                "vocación", "ayudar", "guiar", "formación", "valores"),
            new CareerEntry("Administración de Empresas (ADE)",
                "Gestión, organización y estrategia empresarial.", "📊",
                "empresa", "negocio", "gestión", "administración", "finanzas",
                "dinero", "mercado", "marketing", "venta", "cliente", "estrategia",
                "liderazgo", "equipo", "organización", "emprender", "startup",
                "inversión", "contabilidad", "recursos humanos", "economía", "beneficio"),
            new CareerEntry("Psicología",
                "Comprensión del comportamiento humano y los procesos mentales.", "🧠",
                "mente", "psicología", "comportamiento", "emociones", "sentimientos",
                "terapia", "bienestar", "mental", "ansiedad", "motivación",
                "personalidad", "cognición", "aprendizaje", "relaciones",
                "comunicación", "escucha", "empatía", "ayudar", "neurociencia")
        ));

        // ── SALUD ────────────────────────────────────────────────────────
        DOMAINS.put(Domain.SALUD, Arrays.asList(
            new CareerEntry("Medicina",
                "Diagnóstico, tratamiento y prevención de enfermedades humanas.", "🏥",
                "salud", "medicina", "médico", "enfermedad", "paciente", "hospital",
                "biología", "cuerpo humano", "anatomía", "cirugía", "curar", "cuidar",
                "ayudar", "farmacia", "investigación médica", "urgencias",
                "diagnóstico", "tratamiento", "vacuna", "célula", "genética"),
            new CareerEntry("Enfermería",
                "Cuidado integral de pacientes y promoción de la salud.", "🩺",
                "cuidar", "enfermería", "paciente", "salud", "hospital",
                "atención", "urgencias", "medicación", "herida", "dolor",
                "persona", "ayudar", "empatía", "prevención", "higiene",
                "vacunación", "bienestar", "curación", "familia"),
            new CareerEntry("Farmacia",
                "Estudio, preparación y dispensación de medicamentos.", "💊",
                "farmacia", "medicamento", "fármaco", "química", "salud",
                "laboratorio", "paciente", "dosis", "composición", "biología",
                "investigación", "industria farmacéutica", "tratamiento", "seguridad"),
            new CareerEntry("Fisioterapia",
                "Rehabilitación física y prevención de lesiones mediante terapia manual.", "🏃",
                "fisioterapia", "rehabilitación", "lesión", "músculo", "movimiento",
                "ejercicio", "deporte", "dolor", "cuerpo", "articulación",
                "terapia", "paciente", "recuperación", "prevención", "salud")
        ));

        // ── ARTE ─────────────────────────────────────────────────────────
        DOMAINS.put(Domain.ARTE, Arrays.asList(
            new CareerEntry("Bellas Artes",
                "Expresión artística a través de pintura, escultura, fotografía y otras disciplinas.", "🎨",
                "arte", "pintura", "escultura", "dibujar", "crear", "expresar",
                "creatividad", "color", "forma", "museo", "galería", "fotografía",
                "diseño", "ilustración", "animación", "cómic", "cultura",
                "estética", "artista", "imaginación", "visual"),
            new CareerEntry("Arquitectura",
                "Diseño estético y funcional de espacios y edificios.", "🏛️",
                "diseño", "arquitectura", "edificio", "espacio", "arte", "estética",
                "planos", "construir", "forma", "luz", "materiales", "sostenible",
                "urbanismo", "ciudad", "modelo", "maqueta", "creativo", "3d"),
            new CareerEntry("Diseño Gráfico",
                "Comunicación visual mediante imagen, tipografía y composición.", "✏️",
                "diseño", "gráfico", "visual", "tipografía", "color", "logo",
                "ilustración", "digital", "creatividad", "marca", "publicidad",
                "fotografía", "arte", "ordenador", "software", "herramienta"),
            new CareerEntry("Música",
                "Formación artística e interpretación musical.", "🎵",
                "música", "instrumento", "tocar", "cantar", "composición",
                "armonía", "ritmo", "melodía", "concierto", "escuchar",
                "creatividad", "arte", "cultura", "banda", "orquesta")
        ));
    }

    // ------------------------------------------------------------------ //
    //  API pública
    // ------------------------------------------------------------------ //

    public static List<CareerEntry> getCareers(Domain domain) {
        return Collections.unmodifiableList(DOMAINS.getOrDefault(domain, Collections.emptyList()));
    }

    /**
     * Calcula la puntuación neta de una carrera para un dominio dado.
     * score = (coincidencias_intereses - coincidencias_desintereses) / total_keywords
     * Recortado en [-1.0, 1.0].
     */
    public static ScoredCareer score(CareerEntry career, String intereses, String desintereses) {
        String posNorm = normalizar(intereses);
        String negNorm = normalizar(desintereses);

        long pos = career.keywords.stream()
            .filter(kw -> posNorm.contains(kw.toLowerCase())).count();
        long neg = career.keywords.stream()
            .filter(kw -> negNorm.contains(kw.toLowerCase())).count();

        double raw = (double)(pos - neg) / career.keywords.size();
        double clamped = Math.max(-1.0, Math.min(1.0, raw));
        return new ScoredCareer(career, clamped, pos, neg);
    }

    /**
     * Evalúa todas las carreras de un dominio y devuelve la lista
     * ordenada por puntuación descendente.
     */
    public static List<ScoredCareer> rankDomain(Domain domain,
                                                 String intereses,
                                                 String desintereses) {
        List<ScoredCareer> list = new ArrayList<>();
        for (CareerEntry c : getCareers(domain)) {
            list.add(score(c, intereses, desintereses));
        }
        list.sort((a, b) -> Double.compare(b.score, a.score));
        return list;
    }

    private static String normalizar(String texto) {
        if (texto == null) return "";
        return texto.toLowerCase()
            .replace(",", " ").replace(".", " ").replace(";", " ")
            .replace("!", " ").replace("?", " ").replaceAll("\\s+", " ");
    }
}
