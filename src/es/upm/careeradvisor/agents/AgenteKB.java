package es.upm.careeradvisor.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import es.upm.careeradvisor.knowledge.KnowledgeDomain;
import es.upm.careeradvisor.knowledge.KnowledgeDomain.Domain;
import es.upm.careeradvisor.knowledge.KnowledgeDomain.ScoredCareer;

import java.util.List;
import java.util.Locale;

/**
 * AgenteKB  (Agente Base de Conocimiento especializado)
 * ───────────────────────────────────────────────────────
 * Hay una instancia de este agente por cada dominio de conocimiento:
 *   - agenteKB_tecnologia   → Domain.TECNOLOGIA
 *   - agenteKB_ciencias     → Domain.CIENCIAS
 *   - agenteKB_humanidades  → Domain.HUMANIDADES
 *   - agenteKB_salud        → Domain.SALUD
 *   - agenteKB_arte         → Domain.ARTE
 *
 * Protocolo Contract-Net (rol: Participante / Responder)
 * ────────────────────────────────────────────────────────
 *  1. Recibe CFP del AgenteCoordinador con intereses+desintereses.
 *  2. Evalúa sus carreras con KnowledgeDomain.rankDomain().
 *  3. Si hay al menos una carrera con score > 0  → envía PROPOSE con JSON.
 *     Si no hay candidatos positivos             → envía REFUSE.
 *  4. Espera ACCEPT_PROPOSAL o REJECT_PROPOSAL.
 *     · Con ACCEPT → envía INFORM con la propuesta confirmada.
 *     · Con REJECT → registra en log y vuelve a escuchar.
 *
 * Servicio DF registrado: "kb-domain-<nombre_dominio>"
 * Ontología ACL          : "career-advisor"
 */
public class AgenteKB extends Agent {

    public static final String SERVICE_PREFIX = "kb-domain-";

    private Domain domain;
    private String domainName;

    // ── Setup ────────────────────────────────────────────────────────────
    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args == null || args.length == 0) {
            System.err.println("[KB] Sin argumentos de dominio. Abortando.");
            doDelete();
            return;
        }
        domainName = args[0].toString().toUpperCase(Locale.ROOT);
        try {
            domain = Domain.valueOf(domainName);
        } catch (IllegalArgumentException e) {
            System.err.println("[KB] Dominio desconocido: " + domainName);
            doDelete();
            return;
        }

        System.out.println("[KB:" + domainName + "] Iniciando. Carreras: "
            + KnowledgeDomain.getCareers(domain).size());
        registrarDF();
        addBehaviour(new ContractNetResponderBehaviour());
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (FIPAException ignored) {}
        System.out.println("[KB:" + domainName + "] Finalizado.");
    }

    // ── Registro DF ──────────────────────────────────────────────────────
    private void registrarDF() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType(SERVICE_PREFIX + domainName.toLowerCase());
            sd.setName(SERVICE_PREFIX + domainName.toLowerCase());
            dfd.addServices(sd);
            DFService.register(this, dfd);
            System.out.println("[KB:" + domainName + "] Registrado en DF: "
                + SERVICE_PREFIX + domainName.toLowerCase());
        } catch (FIPAException e) {
            System.err.println("[KB:" + domainName + "] Error DF: " + e.getMessage());
            doDelete();
        }
    }

    // ── Behaviour Contract-Net Responder ──────────────────────────────────
    /**
     * Behaviour cíclico que implementa el rol de Participante del protocolo
     * FIPA Contract-Net:
     *
     *  FASE 1 — Espera CFP
     *    filtro: performative=CFP, ontología="career-advisor"
     *    → evalúa carreras → PROPOSE o REFUSE
     *
     *  FASE 2 — Espera ACCEPT_PROPOSAL o REJECT_PROPOSAL
     *    (correlacionado por conversation-id)
     *    → si ACCEPT: envía INFORM de confirmación
     *    → si REJECT: vuelve a fase 1
     */
    private class ContractNetResponderBehaviour extends CyclicBehaviour {

        // Filtros de mensaje
        private final MessageTemplate MT_CFP = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.CFP),
            MessageTemplate.MatchOntology("career-advisor")
        );
        private final MessageTemplate MT_ACCEPT = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
            MessageTemplate.MatchOntology("career-advisor")
        );
        private final MessageTemplate MT_REJECT = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL),
            MessageTemplate.MatchOntology("career-advisor")
        );

        @Override
        public void action() {
            // ── FASE 1: CFP ────────────────────────────────────────────
            ACLMessage cfp = myAgent.receive(MT_CFP);
            if (cfp != null) {
                handleCFP(cfp);
                return;
            }

            // ── FASE 2: ACCEPT ─────────────────────────────────────────
            ACLMessage accept = myAgent.receive(MT_ACCEPT);
            if (accept != null) {
                handleAccept(accept);
                return;
            }

            // ── FASE 2: REJECT ─────────────────────────────────────────
            ACLMessage reject = myAgent.receive(MT_REJECT);
            if (reject != null) {
                System.out.println("[KB:" + domainName + "] Propuesta rechazada por "
                    + reject.getSender().getLocalName());
                return;
            }

            block(); // sin mensajes → dormir hasta que llegue uno
        }

        // ── Manejo CFP ────────────────────────────────────────────────
        private void handleCFP(ACLMessage cfp) {
            String content = cfp.getContent();
            String[] partes = content.split(
                java.util.regex.Pattern.quote(AgentePercepcion.SEPARADOR), 2);
            String intereses    = partes[0].trim();
            String desintereses = partes.length > 1 ? partes[1].trim() : "";

            System.out.println("[KB:" + domainName + "] CFP recibido de "
                + cfp.getSender().getLocalName());

            // Evaluar carreras del dominio
            List<ScoredCareer> ranking = KnowledgeDomain.rankDomain(domain, intereses, desintereses);

            // Mejor carrera del dominio
            ScoredCareer best = ranking.isEmpty() ? null : ranking.get(0);

            if (best == null || best.score <= 0.0) {
                // Sin candidatos positivos → REFUSE
                ACLMessage refuse = cfp.createReply();
                refuse.setPerformative(ACLMessage.REFUSE);
                refuse.setContent("no-suitable-careers");
                myAgent.send(refuse);
                System.out.println("[KB:" + domainName + "] REFUSE enviado (sin candidatos positivos).");
            } else {
                // Hay candidatos → PROPOSE con JSON del ranking completo del dominio
                ACLMessage propose = cfp.createReply();
                propose.setPerformative(ACLMessage.PROPOSE);
                propose.setContent(buildProposalJSON(ranking, intereses));
                myAgent.send(propose);
                System.out.printf("[KB:%s] PROPOSE enviado. Mejor: %s (%.3f)%n",
                    domainName, best.career.name, best.score);
            }
        }

        // ── Manejo ACCEPT ─────────────────────────────────────────────
        private void handleAccept(ACLMessage accept) {
            System.out.println("[KB:" + domainName + "] ACCEPT_PROPOSAL recibido de "
                + accept.getSender().getLocalName() + ". Enviando INFORM de confirmación.");

            ACLMessage inform = accept.createReply();
            inform.setPerformative(ACLMessage.INFORM);
            // Reenvía el contenido de la propuesta confirmada (ya lo tiene en accept)
            inform.setContent(accept.getContent());
            myAgent.send(inform);
        }

        // ── Construye JSON de la propuesta ───────────────────────────
        private String buildProposalJSON(List<ScoredCareer> ranking, String intereses) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"domain\":\"").append(domainName).append("\",");
            sb.append("\"careers\":[");
            for (int i = 0; i < ranking.size(); i++) {
                ScoredCareer sc = ranking.get(i);
                sb.append("{");
                sb.append("\"nombre\":\"")     .append(esc(sc.career.name))       .append("\",");
                sb.append("\"emoji\":\"")      .append(esc(sc.career.emoji))      .append("\",");
                sb.append("\"descripcion\":\"").append(esc(sc.career.description)).append("\",");
                sb.append("\"score\":")        .append(String.format(Locale.US, "%.4f", sc.score)).append(",");
                sb.append("\"positive\":")     .append(sc.positive).append(",");
                sb.append("\"negative\":")     .append(sc.negative);
                sb.append("}");
                if (i < ranking.size() - 1) sb.append(",");
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
