package es.upm.careeradvisor.knowledge;

import java.util.*;

/**
 * Base de conocimiento que mapea palabras clave a carreras universitarias.
 * Cada carrera tiene una lista de palabras clave asociadas y una descripción.
 */
public class CareerKnowledgeBase {

    public static class CareerEntry {
        public final String name;
        public final String description;
        public final List<String> keywords;
        public final String emoji;

        public CareerEntry(String name, String description, String emoji, String... keywords) {
            this.name = name;
            this.description = description;
            this.emoji = emoji;
            this.keywords = new ArrayList<>(Arrays.asList(keywords));
        }
    }

    private static final List<CareerEntry> CAREERS = new ArrayList<>();

    static {
        CAREERS.add(new CareerEntry(
            "Ingeniería Informática",
            "Diseño y desarrollo de software, sistemas y aplicaciones tecnológicas.",
            "💻",
            "programación", "código", "software", "ordenador", "computador", "algoritmo",
            "web", "aplicación", "app", "tecnología", "digital", "datos", "base de datos",
            "inteligencia artificial", "ia", "robot", "automatización", "videojuego", "juego",
            "hacking", "seguridad", "red", "internet", "sistema", "desarrollo", "java",
            "python", "matemáticas", "lógica", "problema", "solución", "innovación"
        ));

        CAREERS.add(new CareerEntry(
            "Medicina",
            "Diagnóstico, tratamiento y prevención de enfermedades humanas.",
            "🏥",
            "salud", "medicina", "médico", "enfermedad", "paciente", "hospital", "biología",
            "cuerpo humano", "anatomía", "cirugía", "curar", "cuidar", "ayudar", "personas",
            "farmacia", "investigación médica", "urgencias", "sangre", "diagnóstico",
            "tratamiento", "vacuna", "epidemia", "virus", "bacteria", "célula", "genética"
        ));

        CAREERS.add(new CareerEntry(
            "Derecho",
            "Estudio e interpretación de las leyes y normas que rigen la sociedad.",
            "⚖️",
            "ley", "derecho", "justicia", "abogado", "tribunal", "juicio", "norma",
            "contrato", "constitución", "político", "sociedad", "conflicto", "defensa",
            "acusación", "legislación", "gobierno", "estado", "derechos humanos",
            "ética", "moral", "argumentación", "debate", "negociación", "notaría"
        ));

        CAREERS.add(new CareerEntry(
            "Psicología",
            "Comprensión del comportamiento humano y los procesos mentales.",
            "🧠",
            "mente", "psicología", "comportamiento", "emociones", "sentimientos",
            "terapia", "bienestar", "mental", "ansiedad", "depresión", "motivación",
            "personalidad", "cognición", "aprendizaje", "infancia", "adolescencia",
            "relaciones", "comunicación", "escucha", "empatía", "ayudar", "personas",
            "estrés", "trauma", "sueño", "percepción", "memoria", "neurociencia"
        ));

        CAREERS.add(new CareerEntry(
            "Ingeniería Civil",
            "Diseño y construcción de infraestructuras como puentes, edificios y carreteras.",
            "🏗️",
            "construcción", "edificio", "puente", "carretera", "infraestructura",
            "estructura", "hormigón", "arquitectura", "diseño", "planos", "física",
            "resistencia", "materiales", "medio ambiente", "sostenibilidad", "urbanismo",
            "ciudad", "proyecto", "obra", "ingeniería", "cálculo", "topografía"
        ));

        CAREERS.add(new CareerEntry(
            "Arquitectura",
            "Diseño estético y funcional de espacios y edificios.",
            "🏛️",
            "diseño", "arquitectura", "edificio", "espacio", "arte", "estética",
            "planos", "construir", "forma", "luz", "materiales", "sostenible",
            "urbanismo", "ciudad", "interior", "exterior", "modelo", "maqueta",
            "creativo", "creatividad", "visual", "3d", "render", "dibujar"
        ));

        CAREERS.add(new CareerEntry(
            "Administración de Empresas (ADE)",
            "Gestión, organización y estrategia empresarial.",
            "📊",
            "empresa", "negocio", "gestión", "administración", "económica", "finanzas",
            "dinero", "mercado", "marketing", "venta", "cliente", "estrategia",
            "liderazgo", "equipo", "organización", "emprender", "startup", "inversión",
            "contabilidad", "recursos humanos", "proyecto", "objetivo", "resultado",
            "comercio", "economía", "bolsa", "beneficio", "rentabilidad"
        ));

        CAREERS.add(new CareerEntry(
            "Biología",
            "Estudio de los seres vivos, sus procesos y su relación con el entorno.",
            "🔬",
            "biología", "naturaleza", "animal", "planta", "ecosistema", "célula",
            "genética", "evolución", "especie", "medio ambiente", "laboratorio",
            "microscopio", "investigación", "experimento", "conservación", "marino",
            "bosque", "flora", "fauna", "microbiología", "biotecnología", "adn"
        ));

        CAREERS.add(new CareerEntry(
            "Periodismo y Comunicación",
            "Producción de contenido informativo y gestión de medios de comunicación.",
            "📰",
            "periodismo", "noticia", "comunicación", "medios", "radio", "televisión",
            "escribir", "redacción", "reportaje", "entrevista", "información", "sociedad",
            "opinión", "redes sociales", "fotografía", "vídeo", "prensa", "digital",
            "lenguaje", "narrar", "contar historias", "política", "cultura", "actualidad"
        ));

        CAREERS.add(new CareerEntry(
            "Educación / Magisterio",
            "Formación y enseñanza a niños, jóvenes y adultos.",
            "📚",
            "enseñar", "educación", "niños", "profesor", "maestro", "escuela",
            "aprendizaje", "pedagogía", "infancia", "desarrollo", "juego", "didáctica",
            "motivar", "aula", "alumno", "conocimiento", "transmitir", "paciencia",
            "vocación", "ayudar", "guiar", "formación", "valores", "creatividad"
        ));

        CAREERS.add(new CareerEntry(
            "Ingeniería de Telecomunicaciones",
            "Diseño de sistemas de transmisión de información y redes de comunicación.",
            "📡",
            "telecomunicaciones", "señal", "antena", "red", "internet", "comunicación",
            "fibra óptica", "satélite", "móvil", "5g", "electrónica", "radio",
            "frecuencia", "protocolo", "router", "hardware", "física", "ondas",
            "transmisión", "datos", "conectividad", "iot", "dispositivo"
        ));

        CAREERS.add(new CareerEntry(
            "Bellas Artes",
            "Expresión artística a través de la pintura, escultura, fotografía y otras disciplinas.",
            "🎨",
            "arte", "pintura", "escultura", "dibujar", "crear", "expresar", "creatividad",
            "color", "forma", "museo", "galería", "fotografía", "diseño", "ilustración",
            "animación", "cómic", "cultura", "estética", "instalación", "performance",
            "artista", "imaginación", "visual", "plasticidad", "artesanía"
        ));

        CAREERS.add(new CareerEntry(
            "Química",
            "Estudio de la materia, sus propiedades y transformaciones.",
            "⚗️",
            "química", "molécula", "átomo", "reacción", "laboratorio", "experimento",
            "compuesto", "elemento", "periódica", "orgánica", "inorgánica", "síntesis",
            "farmacia", "materiales", "industria", "plástico", "combustible", "energía",
            "análisis", "pureza", "concentración", "solución", "investigación"
        ));

        CAREERS.add(new CareerEntry(
            "Matemáticas",
            "Estudio de estructuras abstractas, números, geometría y análisis.",
            "📐",
            "matemáticas", "número", "ecuación", "álgebra", "cálculo", "geometría",
            "estadística", "probabilidad", "lógica", "demostración", "teorema",
            "función", "gráfica", "análisis", "abstracción", "modelo", "simulación",
            "cifrado", "criptografía", "física", "ingeniería", "economía", "finanzas"
        ));

        CAREERS.add(new CareerEntry(
            "Enfermería",
            "Cuidado integral de pacientes y promoción de la salud.",
            "🩺",
            "cuidar", "enfermería", "paciente", "salud", "hospital", "medicina",
            "atención", "urgencias", "quirófano", "medicación", "herida", "dolor",
            "persona", "ayudar", "empatía", "equipo médico", "prevención", "higiene",
            "vacunación", "bienestar", "curación", "seguimiento", "familia"
        ));
    }

    private static String normalizar(String texto) {
        if (texto == null) return "";
        return texto.toLowerCase()
            .replace(",", " ").replace(".", " ").replace(";", " ")
            .replace("!", " ").replace("?", " ").replaceAll("\\s+", " ");
    }

    private static int contarCoincidencias(String textoNorm, CareerEntry carrera) {
        int coincidencias = 0;
        for (String kw : carrera.keywords) {
            if (textoNorm.contains(kw.toLowerCase())) {
                coincidencias++;
            }
        }
        return coincidencias;
    }

    /**
     * Calcula la puntuación neta de una carrera:
     *   score = (coincidencias_intereses - coincidencias_desintereses) / total_keywords
     * El resultado se recorta en [-1.0, 1.0].
     *
     * @param intereses    texto de intereses del usuario (puede ser "")
     * @param desintereses texto de desintereses del usuario (puede ser "")
     */
    public static double calcularPuntuacion(String intereses, String desintereses, CareerEntry carrera) {
        int positivos = contarCoincidencias(normalizar(intereses),    carrera);
        int negativos = contarCoincidencias(normalizar(desintereses), carrera);
        double score  = (double)(positivos - negativos) / carrera.keywords.size();
        return Math.max(-1.0, Math.min(1.0, score));
    }

    /**
     * Devuelve todas las carreras ordenadas por puntuación neta descendente.
     *
     * @param intereses    texto de intereses   (puede ser "")
     * @param desintereses texto de desintereses (puede ser "")
     */
    public static List<Map.Entry<CareerEntry, Double>> rankearCarreras(String intereses, String desintereses) {
        List<Map.Entry<CareerEntry, Double>> ranking = new ArrayList<>();
        for (CareerEntry c : CAREERS) {
            double score = calcularPuntuacion(intereses, desintereses, c);
            ranking.add(new AbstractMap.SimpleEntry<>(c, score));
        }
        ranking.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return ranking;
    }

    public static List<CareerEntry> getAllCareers() {
        return Collections.unmodifiableList(CAREERS);
    }
}
