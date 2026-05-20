package es.upm.careeradvisor.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import es.upm.careeradvisor.knowledge.CareerKnowledgeBase;
import es.upm.careeradvisor.knowledge.CareerKnowledgeBase.CareerEntry;

import java.util.*;

/**
 * Agente de Conocimiento / Procesamiento (AgenteConocimiento).
 *
 * Responsabilidad: Recibe los intereses del usuario (mensaje REQUEST),
 * aplica la base de conocimiento para calcular puntuaciones de similitud,
 * construye un ranking de carreras recomendadas y envía el resultado
 * al AgenteVisualizacion mediante un mensaje ACL INFORM.
 *
 * Registra en el DF el servicio "procesamiento-carreras".
 * Implementa filtro de mensajes en modo bloqueante (receive + block).
 */
public class AgenteConocimiento extends Agent {

    public static final String SERVICE_TYPE = "procesamiento-carreras";

    @Override
    protected void setup() {
        System.out.println("[AgenteConocimiento] Iniciando agente: " + getLocalName());
        registrarEnDF();
        addBehaviour(new ProcesarInteresesBehaviour());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            System.err.println("[AgenteConocimiento] Error al desregistrarse del DF: " + e.getMessage());
        }
        System.out.println("[AgenteConocimiento] Agente finalizado.");
    }

    // -------------------------------------------------------------------------
    // Registro en el DF
    // -------------------------------------------------------------------------
    private void registrarEnDF() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType(SERVICE_TYPE);
            sd.setName(SERVICE_TYPE);
            sd.addOntologies("career-advisor-ontology");
            sd.addLanguages("plain-text");
            dfd.addServices(sd);
            DFService.register(this, dfd);
            System.out.println("[AgenteConocimiento] Registrado en DF con servicio: " + SERVICE_TYPE);
        } catch (FIPAException e) {
            System.err.println("[AgenteConocimiento] Error al registrarse en el DF: " + e.getMessage());
            doDelete();
        }
    }

    // -------------------------------------------------------------------------
    // Busca un agente por tipo de servicio en el DF
    // -------------------------------------------------------------------------
    private AID buscarAgente(String tipoServicio) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(tipoServicio);
        template.addServices(sd);

        SearchConstraints sc = new SearchConstraints();
        sc.setMaxResults(1L);

        try {
            DFAgentDescription[] results = DFService.search(this, template, sc);
            if (results != null && results.length > 0) {
                AID agente = results[0].getName();
                System.out.println("[AgenteConocimiento] Agente encontrado para servicio '"
                    + tipoServicio + "': " + agente.getLocalName());
                return agente;
            }
        } catch (FIPAException e) {
            System.err.println("[AgenteConocimiento] Error buscando servicio '"
                + tipoServicio + "': " + e.getMessage());
        }
        System.err.println("[AgenteConocimiento] No se encontró agente para: " + tipoServicio);
        return null;
    }

    // -------------------------------------------------------------------------
    // Behaviour cíclico con filtro bloqueante: espera mensajes REQUEST
    // -------------------------------------------------------------------------
    private class ProcesarInteresesBehaviour extends CyclicBehaviour {

        // Filtro: solo mensajes ACL de tipo REQUEST con la ontología correcta
        private final MessageTemplate filtro = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
            MessageTemplate.MatchOntology("career-advisor-ontology")
        );

        @Override
        public void action() {
            // Recepción NO bloqueante + block() — implementa filtro en modo bloqueante
            ACLMessage msg = myAgent.receive(filtro);

            if (msg != null) {
                // Separar intereses y desintereses del contenido del mensaje
                String contenido = msg.getContent();
                String[] partes  = contenido.split(
                    java.util.regex.Pattern.quote(AgentePercepcion.SEPARADOR), 2);
                String intereses    = partes[0].trim();
                String desintereses = partes.length > 1 ? partes[1].trim() : "";

                System.out.println("[AgenteConocimiento] Mensaje REQUEST recibido de: "
                    + msg.getSender().getLocalName());
                System.out.println("[AgenteConocimiento] Intereses:    \"" + intereses    + "\"");
                System.out.println("[AgenteConocimiento] Desintereses: \"" + desintereses + "\"");

                // === Procesamiento con base de conocimiento ===
                List<Map.Entry<CareerEntry, Double>> ranking =
                    CareerKnowledgeBase.rankearCarreras(intereses, desintereses);

                String resultado = construirResultadoJSON(intereses, desintereses, ranking);
                System.out.println("[AgenteConocimiento] Procesamiento completado. "
                    + "Mejor carrera: " + ranking.get(0).getKey().name
                    + " (score=" + String.format("%.3f", ranking.get(0).getValue()) + ")");

                // Buscar AgenteVisualizacion en el DF
                AID agenteViz = buscarAgente(AgenteVisualizacion.SERVICE_TYPE);
                if (agenteViz == null) {
                    System.err.println("[AgenteConocimiento] AgenteVisualizacion no encontrado.");
                    return;
                }

                // Enviar resultado como INFORM
                ACLMessage respuesta = new ACLMessage(ACLMessage.INFORM);
                respuesta.addReceiver(agenteViz);
                respuesta.setOntology("career-advisor-ontology");
                respuesta.setLanguage("json");
                respuesta.setContent(resultado);
                myAgent.send(respuesta);

                System.out.println("[AgenteConocimiento] Resultado INFORM enviado a: "
                    + agenteViz.getLocalName());
            } else {
                // Sin mensaje — bloquear este behaviour hasta que llegue uno nuevo
                block();
            }
        }

        /**
         * Serializa el ranking a un formato JSON ligero para enviarlo al agente de visualización.
         * Formato:
         * {
         *   "intereses": "...",
         *   "ranking": [
         *     {"nombre":"...","emoji":"...","descripcion":"...","score":0.xx,"keywords_matched":N},
         *     ...
         *   ]
         * }
         */
        private String construirResultadoJSON(
                String intereses,
                String desintereses,
                List<Map.Entry<CareerEntry, Double>> ranking) {

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"intereses\":\"").append(escapeJson(intereses)).append("\",");
            sb.append("\"desintereses\":\"").append(escapeJson(desintereses)).append("\",");
            sb.append("\"ranking\":[");

            // Incluir hasta las 5 mejores carreras
            int maxResultados = Math.min(5, ranking.size());
            for (int i = 0; i < maxResultados; i++) {
                CareerEntry c = ranking.get(i).getKey();
                double score = ranking.get(i).getValue();

                // Contar keywords positivas y negativas coincidentes
                String textNorm  = intereses.toLowerCase()
                    .replace(",", " ").replace(".", " ").replace(";", " ")
                    .replaceAll("\\s+", " ");
                String deNorm    = desintereses.toLowerCase()
                    .replace(",", " ").replace(".", " ").replace(";", " ")
                    .replaceAll("\\s+", " ");
                long matched  = c.keywords.stream()
                    .filter(kw -> textNorm.contains(kw.toLowerCase())).count();
                long penalty  = c.keywords.stream()
                    .filter(kw -> deNorm.contains(kw.toLowerCase())).count();

                sb.append("{");
                sb.append("\"nombre\":\"").append(escapeJson(c.name)).append("\",");
                sb.append("\"emoji\":\"").append(escapeJson(c.emoji)).append("\",");
                sb.append("\"descripcion\":\"").append(escapeJson(c.description)).append("\",");
                sb.append("\"score\":").append(String.format(Locale.US, "%.4f", score)).append(",");
                sb.append("\"keywords_matched\":").append(matched).append(",");
                sb.append("\"keywords_penalty\":").append(penalty);
                sb.append("}");

                if (i < maxResultados - 1) sb.append(",");
            }

            sb.append("]}");
            return sb.toString();
        }

        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
        }
    }
}
