package es.upm.careeradvisor.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import es.upm.careeradvisor.gui.ResultadoGUI;

import javax.swing.SwingUtilities;
import java.util.*;
import java.util.regex.*;

/**
 * AgenteVisualizacion
 * ────────────────────
 * Recibe el INFORM con el ranking global del AgenteCoordinador,
 * lo parsea y lanza la GUI Swing. También imprime el ranking por consola.
 *
 * Servicio DF : "visualizacion-resultados"
 * Ontología   : "career-advisor"
 */
public class AgenteVisualizacion extends Agent {

    public static final String SERVICE_TYPE = "visualizacion-resultados";

    @Override
    protected void setup() {
        System.out.println("[Visualizacion] Iniciando: " + getLocalName());
        registrarDF();
        addBehaviour(new MostrarBehaviour());
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (FIPAException ignored) {}
        System.out.println("[Visualizacion] Finalizado.");
    }

    private void registrarDF() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType(SERVICE_TYPE);
            sd.setName(SERVICE_TYPE);
            dfd.addServices(sd);
            DFService.register(this, dfd);
            System.out.println("[Visualizacion] Registrado en DF: " + SERVICE_TYPE);
        } catch (FIPAException e) {
            System.err.println("[Visualizacion] Error DF: " + e.getMessage());
            doDelete();
        }
    }

    // ── Behaviour cíclico con filtro bloqueante ──────────────────────────
    private class MostrarBehaviour extends CyclicBehaviour {

        private final MessageTemplate MT = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.MatchOntology("career-advisor")
        );

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(MT);
            if (msg != null) {
                System.out.println("[Visualizacion] INFORM recibido de: "
                    + msg.getSender().getLocalName());
                ResultadoParsed r = parsear(msg.getContent());
                if (r != null) {
                    mostrarConsola(r);
                    SwingUtilities.invokeLater(() -> new ResultadoGUI(r));
                }
            } else {
                block();
            }
        }

        // ── Parser JSON ligero ────────────────────────────────────────
        private ResultadoParsed parsear(String json) {
            try {
                ResultadoParsed r = new ResultadoParsed();

                r.intereses    = extraerCampo(json, "intereses");
                r.desintereses = extraerCampo(json, "desintereses");
                r.winnerDomain = extraerCampo(json, "winner_domain");
                r.careers      = new ArrayList<>();

                Pattern p = Pattern.compile(
                    "\\{\"nombre\":\"((?:[^\"\\\\]|\\\\.)*)\","
                    + "\"emoji\":\"((?:[^\"\\\\]|\\\\.)*)\","
                    + "\"descripcion\":\"((?:[^\"\\\\]|\\\\.)*)\","
                    + "\"score\":([\\-\\d.]+),"
                    + "\"positive\":(\\d+),"
                    + "\"negative\":(\\d+),"
                    + "\"domain\":\"((?:[^\"\\\\]|\\\\.)*)\","
                    + "\"winner\":(true|false)\\}"
                );
                Matcher m = p.matcher(json);
                while (m.find()) {
                    ResultadoGUI.CarreraResultado c = new ResultadoGUI.CarreraResultado(
                        unesc(m.group(1)), unesc(m.group(2)), unesc(m.group(3)),
                        Double.parseDouble(m.group(4)),
                        Long.parseLong(m.group(5)), Long.parseLong(m.group(6)),
                        unesc(m.group(7)), Boolean.parseBoolean(m.group(8))
                    );
                    r.careers.add(c);
                }
                return r;
            } catch (Exception e) {
                System.err.println("[Visualizacion] Error parseando JSON: " + e.getMessage());
                return null;
            }
        }

        private String extraerCampo(String json, String campo) {
            Matcher m = Pattern.compile("\"" + campo + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
                               .matcher(json);
            return m.find() ? unesc(m.group(1)) : "";
        }

        private String unesc(String s) {
            return s.replace("\\\"", "\"").replace("\\\\", "\\")
                    .replace("\\n", "\n").replace("\\r", "\r");
        }

        // ── Consola ───────────────────────────────────────────────────
        private void mostrarConsola(ResultadoParsed r) {
            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║        CARREERADVISOR MAS — RANKING GLOBAL               ║");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║ Intereses:    " + trunc(r.intereses, 45));
            if (!r.desintereses.isEmpty())
                System.out.println("║ Desintereses: " + trunc(r.desintereses, 45));
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            String[] med = {"🥇","🥈","🥉"," 4."," 5."," 6."," 7."," 8."," 9.","10."};
            for (int i = 0; i < r.careers.size(); i++) {
                ResultadoGUI.CarreraResultado c = r.careers.get(i);
                System.out.printf("║ %s %-28s %5.1f%%  +%d/-%d  [%s]%n",
                    i < med.length ? med[i] : "  ",
                    trunc(c.nombre, 28), c.score * 100,
                    c.positive, c.negative,
                    trunc(c.domain, 12));
            }
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println();
        }

        private String trunc(String s, int max) {
            if (s == null) return "";
            return s.length() <= max ? s : s.substring(0, max - 3) + "...";
        }
    }

    // ── Estructura de datos del resultado parseado ───────────────────────
    public static class ResultadoParsed {
        public String intereses;
        public String desintereses;
        public String winnerDomain;
        public List<ResultadoGUI.CarreraResultado> careers;
    }
}
