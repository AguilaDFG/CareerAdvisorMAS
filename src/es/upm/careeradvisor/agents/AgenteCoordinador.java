package es.upm.careeradvisor.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;
import java.util.regex.*;

/**
 * AgenteCoordinador
 * ──────────────────
 * Orquesta el protocolo FIPA Contract-Net como Iniciador:
 *
 *  1. Recibe REQUEST de AgentePercepcion con intereses+desintereses.
 *  2. Busca todos los agentes KB en el DF.
 *  3. Envía CFP a todos los KB simultáneamente.
 *  4. Recoge PROPOSE / REFUSE de cada KB con timeout.
 *  5. Elige la mejor propuesta global (carrera con score más alto).
 *  6. Envía ACCEPT_PROPOSAL al KB ganador y REJECT_PROPOSAL al resto.
 *  7. Espera el INFORM de confirmación del KB ganador.
 *  8. Agrega el ranking global (todas las propuestas recibidas)
 *     y lo envía como INFORM al AgenteVisualizacion.
 *
 * Servicio DF registrado : "coordinador-carreras"
 * Ontología ACL          : "career-advisor"
 */
public class AgenteCoordinador extends Agent {

    public static final String SERVICE_TYPE = "coordinador-carreras";

    @Override
    protected void setup() {
        System.out.println("[Coordinador] Iniciando: " + getLocalName());
        registrarDF();
        addBehaviour(new EsperarRequestBehaviour());
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (FIPAException ignored) {}
        System.out.println("[Coordinador] Finalizado.");
    }

    // ── Registro DF ──────────────────────────────────────────────────────
    private void registrarDF() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType(SERVICE_TYPE);
            sd.setName(SERVICE_TYPE);
            dfd.addServices(sd);
            DFService.register(this, dfd);
            System.out.println("[Coordinador] Registrado en DF: " + SERVICE_TYPE);
        } catch (FIPAException e) {
            System.err.println("[Coordinador] Error DF: " + e.getMessage());
            doDelete();
        }
    }

    // ── Búsqueda de agentes KB en el DF ──────────────────────────────────
    private List<AID> buscarAgentesKB() {
        List<AID> agentes = new ArrayList<>();
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            // Prefijo común a todos los dominios KB
            sd.setType(AgenteKB.SERVICE_PREFIX + "*");
            // JADE soporta wildcard '*' al final en búsquedas DF
            template.addServices(sd);
            SearchConstraints sc = new SearchConstraints();
            sc.setMaxResults(20L);
            DFAgentDescription[] results = DFService.search(this, template, sc);
            for (DFAgentDescription r : results) agentes.add(r.getName());
        } catch (FIPAException e) {
            // Fallback: buscar cada dominio individualmente
            for (String dom : new String[]{"tecnologia","ciencias","humanidades","salud","arte"}) {
                AID a = buscarPorTipo(AgenteKB.SERVICE_PREFIX + dom);
                if (a != null) agentes.add(a);
            }
        }
        if (agentes.isEmpty()) {
            // Segundo fallback por si el wildcard no está soportado en esta versión de JADE
            for (String dom : new String[]{"tecnologia","ciencias","humanidades","salud","arte"}) {
                AID a = buscarPorTipo(AgenteKB.SERVICE_PREFIX + dom);
                if (a != null) agentes.add(a);
            }
        }
        return agentes;
    }

    private AID buscarPorTipo(String tipo) {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(tipo);
            template.addServices(sd);
            SearchConstraints sc = new SearchConstraints();
            sc.setMaxResults(1L);
            DFAgentDescription[] r = DFService.search(this, template, sc);
            if (r != null && r.length > 0) return r[0].getName();
        } catch (FIPAException e) {
            System.err.println("[Coordinador] Error buscando '" + tipo + "': " + e.getMessage());
        }
        return null;
    }

    // ── Búsqueda del agente de visualización ─────────────────────────────
    private AID buscarVisualizacion() {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(AgenteVisualizacion.SERVICE_TYPE);
            template.addServices(sd);
            SearchConstraints sc = new SearchConstraints();
            sc.setMaxResults(1L);
            DFAgentDescription[] r = DFService.search(this, template, sc);
            if (r != null && r.length > 0) return r[0].getName();
        } catch (FIPAException e) {
            System.err.println("[Coordinador] Error buscando visualización: " + e.getMessage());
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════════════
    //  BEHAVIOUR 1 — Esperar REQUEST de AgentePercepcion
    // ════════════════════════════════════════════════════════════════════
    private class EsperarRequestBehaviour extends Behaviour {

        private final MessageTemplate MT = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
            MessageTemplate.MatchOntology("career-advisor")
        );
        private boolean done = false;

        @Override
        public void action() {
            ACLMessage req = myAgent.receive(MT);
            if (req != null) {
                System.out.println("[Coordinador] REQUEST recibido de: "
                    + req.getSender().getLocalName());
                done = true;
                // Lanzar el behaviour de Contract-Net con el contenido recibido
                myAgent.addBehaviour(new ContractNetIniciadorBehaviour(req.getContent()));
            } else {
                block();
            }
        }

        @Override public boolean done() { return done; }
    }

    // ════════════════════════════════════════════════════════════════════
    //  BEHAVIOUR 2 — Contract-Net Iniciador
    //  Estados: SEND_CFP → WAIT_PROPOSALS → SEND_ACCEPT → WAIT_INFORM → DONE
    // ════════════════════════════════════════════════════════════════════
    private class ContractNetIniciadorBehaviour extends Behaviour {

        private static final int S_SEND_CFP       = 0;
        private static final int S_WAIT_PROPOSALS = 1;
        private static final int S_SEND_ACCEPT    = 2;
        private static final int S_WAIT_INFORM    = 3;
        private static final int S_DONE           = 4;

        private static final long TIMEOUT_PROPOSALS = 5000L; // ms
        private static final long TIMEOUT_INFORM    = 5000L;

        private final String contenidoOriginal;
        private int estado = S_SEND_CFP;

        // CFP tracking
        private String conversationId;
        private List<AID> kbAgents;
        private int expectedReplies;

        // Propuestas recibidas: AID → contenidoJSON
        private final Map<AID, String> propuestas = new LinkedHashMap<>();
        private final Set<AID> refusados = new HashSet<>();

        // Aceptada
        private AID kbGanador;
        private long cfpDeadline;
        private long informDeadline;

        ContractNetIniciadorBehaviour(String contenido) {
            this.contenidoOriginal = contenido;
        }

        @Override
        public void action() {
            switch (estado) {
                case S_SEND_CFP:       doSendCFP();       break;
                case S_WAIT_PROPOSALS: doWaitProposals(); break;
                case S_SEND_ACCEPT:    doSendAccept();    break;
                case S_WAIT_INFORM:    doWaitInform();    break;
                default: break;
            }
        }

        @Override public boolean done() { return estado == S_DONE; }

        // ── Estado 0: enviar CFP a todos los KB ──────────────────────
        private void doSendCFP() {
            kbAgents = buscarAgentesKB();
            if (kbAgents.isEmpty()) {
                System.err.println("[Coordinador] No se encontraron agentes KB. Abortando.");
                estado = S_DONE;
                return;
            }
            System.out.println("[Coordinador] Enviando CFP a " + kbAgents.size() + " agentes KB.");

            conversationId = "cfp-" + System.currentTimeMillis();
            expectedReplies = kbAgents.size();

            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            cfp.setOntology("career-advisor");
            cfp.setLanguage("plain-text");
            cfp.setConversationId(conversationId);
            cfp.setContent(contenidoOriginal);
            for (AID kb : kbAgents) cfp.addReceiver(kb);
            myAgent.send(cfp);

            cfpDeadline = System.currentTimeMillis() + TIMEOUT_PROPOSALS;
            estado = S_WAIT_PROPOSALS;
            System.out.println("[Coordinador] CFP enviado. Esperando propuestas (timeout "
                + TIMEOUT_PROPOSALS + "ms)...");
        }

        // ── Estado 1: recoger PROPOSE / REFUSE con timeout ────────────
        private void doWaitProposals() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchConversationId(conversationId),
                MessageTemplate.or(
                    MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                    MessageTemplate.MatchPerformative(ACLMessage.REFUSE)
                )
            );

            ACLMessage reply = myAgent.receive(mt);
            if (reply != null) {
                if (reply.getPerformative() == ACLMessage.PROPOSE) {
                    propuestas.put(reply.getSender(), reply.getContent());
                    System.out.println("[Coordinador] PROPOSE de "
                        + reply.getSender().getLocalName());
                } else {
                    refusados.add(reply.getSender());
                    System.out.println("[Coordinador] REFUSE de "
                        + reply.getSender().getLocalName());
                }
                expectedReplies--;
            }

            // Avanzar si ya tenemos todas las respuestas O ha pasado el timeout
            if (expectedReplies <= 0 || System.currentTimeMillis() > cfpDeadline) {
                System.out.println("[Coordinador] Ronda CFP cerrada. Propuestas: "
                    + propuestas.size() + ", Rechazos: " + refusados.size());
                estado = S_SEND_ACCEPT;
            } else {
                block(200); // polling ligero
            }
        }

        // ── Estado 2: elegir ganador y enviar ACCEPT / REJECT ─────────
        private void doSendAccept() {
            if (propuestas.isEmpty()) {
                System.err.println("[Coordinador] Ningún KB hizo propuesta. Abortando.");
                estado = S_DONE;
                return;
            }

            // Encontrar la carrera con mayor score entre todas las propuestas
            double mejorScore = Double.NEGATIVE_INFINITY;
            kbGanador = null;
            String contenidoGanador = null;

            for (Map.Entry<AID, String> e : propuestas.entrySet()) {
                double topScore = extraerTopScore(e.getValue());
                System.out.printf("[Coordinador]   KB %-25s top-score=%.4f%n",
                    e.getKey().getLocalName(), topScore);
                if (topScore > mejorScore) {
                    mejorScore   = topScore;
                    kbGanador    = e.getKey();
                    contenidoGanador = e.getValue();
                }
            }

            System.out.println("[Coordinador] Ganador: " + kbGanador.getLocalName()
                + " (score=" + String.format("%.4f", mejorScore) + ")");

            // ACCEPT al ganador
            ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            accept.setOntology("career-advisor");
            accept.setConversationId(conversationId);
            accept.setContent(contenidoGanador); // eco del JSON para que el KB lo confirme
            accept.addReceiver(kbGanador);
            myAgent.send(accept);

            // REJECT al resto
            for (AID kb : propuestas.keySet()) {
                if (!kb.equals(kbGanador)) {
                    ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                    reject.setOntology("career-advisor");
                    reject.setConversationId(conversationId);
                    reject.setContent("not-selected");
                    reject.addReceiver(kb);
                    myAgent.send(reject);
                }
            }

            informDeadline = System.currentTimeMillis() + TIMEOUT_INFORM;
            estado = S_WAIT_INFORM;
        }

        // ── Estado 3: esperar INFORM de confirmación del ganador ──────
        private void doWaitInform() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchOntology("career-advisor")
                ),
                MessageTemplate.MatchSender(kbGanador)
            );

            ACLMessage inform = myAgent.receive(mt);
            if (inform != null) {
                System.out.println("[Coordinador] INFORM de confirmación recibido de "
                    + inform.getSender().getLocalName());
                enviarRankingGlobal();
                estado = S_DONE;
            } else if (System.currentTimeMillis() > informDeadline) {
                System.err.println("[Coordinador] Timeout esperando INFORM. Enviando ranking igualmente.");
                enviarRankingGlobal();
                estado = S_DONE;
            } else {
                block(200);
            }
        }

        // ── Agrega todas las propuestas y envía a Visualizacion ───────
        private void enviarRankingGlobal() {
            // Separar intereses/desintereses para incluirlos en el resultado
            String[] partes = contenidoOriginal.split(
                java.util.regex.Pattern.quote(AgentePercepcion.SEPARADOR), 2);
            String intereses    = partes[0].trim();
            String desintereses = partes.length > 1 ? partes[1].trim() : "";

            String jsonGlobal = construirRankingGlobal(intereses, desintereses, propuestas, kbGanador);

            AID viz = buscarVisualizacion();
            if (viz == null) {
                System.err.println("[Coordinador] AgenteVisualizacion no encontrado.");
                return;
            }

            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setOntology("career-advisor");
            msg.setLanguage("json");
            msg.addReceiver(viz);
            msg.setContent(jsonGlobal);
            myAgent.send(msg);
            System.out.println("[Coordinador] Ranking global INFORM enviado a "
                + viz.getLocalName());
        }

        // ── Extrae el top score de un JSON de propuesta ───────────────
        private double extraerTopScore(String json) {
            // Primera aparición de "score": <número>
            Matcher m = Pattern.compile("\"score\"\\s*:\\s*([\\-\\d.]+)").matcher(json);
            if (m.find()) {
                try { return Double.parseDouble(m.group(1)); }
                catch (NumberFormatException ignored) {}
            }
            return Double.NEGATIVE_INFINITY;
        }

        // ── Construye JSON global con ranking completo ────────────────
        private String construirRankingGlobal(String intereses, String desintereses,
                                               Map<AID, String> propuestas, AID ganador) {
            // Recolectar todas las entradas de carrera de todos los dominios,
            // marcar el dominio ganador y ordenar globalmente por score
            List<String[]> todas = new ArrayList<>();
            // String[]: nombre, emoji, descripcion, score, positive, negative, domain, winner

            for (Map.Entry<AID, String> e : propuestas.entrySet()) {
                String dominio   = e.getKey().getLocalName();
                boolean isWinner = e.getKey().equals(ganador);
                // Parsear el array "careers" del JSON del KB
                Matcher mc = Pattern.compile(
                    "\\{\"nombre\":\"((?:[^\"\\\\]|\\\\.)*)\","
                    + "\"emoji\":\"((?:[^\"\\\\]|\\\\.)*)\","
                    + "\"descripcion\":\"((?:[^\"\\\\]|\\\\.)*)\","
                    + "\"score\":([\\-\\d.]+),"
                    + "\"positive\":(\\d+),"
                    + "\"negative\":(\\d+)\\}"
                ).matcher(e.getValue());
                while (mc.find()) {
                    todas.add(new String[]{
                        mc.group(1), mc.group(2), mc.group(3),
                        mc.group(4), mc.group(5), mc.group(6),
                        dominio, isWinner ? "true" : "false"
                    });
                }
            }

            // Ordenar por score descendente
            todas.sort((a, b) -> Double.compare(
                Double.parseDouble(b[3]), Double.parseDouble(a[3])));

            // Serializar
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"intereses\":\"")   .append(esc(intereses))   .append("\",");
            sb.append("\"desintereses\":\"").append(esc(desintereses)).append("\",");
            sb.append("\"winner_domain\":\"").append(ganador.getLocalName()).append("\",");
            sb.append("\"careers\":[");

            int limit = Math.min(10, todas.size());
            for (int i = 0; i < limit; i++) {
                String[] c = todas.get(i);
                sb.append("{");
                sb.append("\"nombre\":\"")      .append(esc(c[0])).append("\",");
                sb.append("\"emoji\":\"")       .append(esc(c[1])).append("\",");
                sb.append("\"descripcion\":\"") .append(esc(c[2])).append("\",");
                sb.append("\"score\":")         .append(c[3]).append(",");
                sb.append("\"positive\":")      .append(c[4]).append(",");
                sb.append("\"negative\":")      .append(c[5]).append(",");
                sb.append("\"domain\":\"")      .append(esc(c[6])).append("\",");
                sb.append("\"winner\":")        .append(c[7]);
                sb.append("}");
                if (i < limit - 1) sb.append(",");
            }
            sb.append("]}");
            return sb.toString();
        }

        private String esc(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", "\\n").replace("\r", "\\r");
        }
    }
}
