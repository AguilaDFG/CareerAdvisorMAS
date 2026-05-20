package es.upm.careeradvisor.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import es.upm.careeradvisor.gui.ResultadoGUI;
import es.upm.careeradvisor.gui.ResultadoGUI.CarreraResultado;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

/**
 * Agente de Visualización (AgenteVisualizacion).
 *
 * Responsabilidad: Recibe el resultado del ranking (mensaje INFORM con JSON),
 * lo parsea y lanza la interfaz gráfica Swing (ResultadoGUI) con los
 * resultados. También imprime un resumen por consola.
 *
 * Registra en el DF el servicio "visualizacion-resultados".
 * Implementa filtro de mensajes bloqueante (receive + block).
 */
public class AgenteVisualizacion extends Agent {

    public static final String SERVICE_TYPE = "visualizacion-resultados";

    @Override
    protected void setup() {
        System.out.println("[AgenteVisualizacion] Iniciando agente: " + getLocalName());
        registrarEnDF();
        addBehaviour(new MostrarResultadosBehaviour());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            System.err.println("[AgenteVisualizacion] Error al desregistrarse del DF: " + e.getMessage());
        }
        System.out.println("[AgenteVisualizacion] Agente finalizado.");
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
            dfd.addServices(sd);
            DFService.register(this, dfd);
            System.out.println("[AgenteVisualizacion] Registrado en DF con servicio: " + SERVICE_TYPE);
        } catch (FIPAException e) {
            System.err.println("[AgenteVisualizacion] Error al registrarse en el DF: " + e.getMessage());
            doDelete();
        }
    }

    // -------------------------------------------------------------------------
    // Behaviour cíclico con filtro bloqueante: espera mensajes INFORM
    // -------------------------------------------------------------------------
    private class MostrarResultadosBehaviour extends CyclicBehaviour {

        // Filtro: solo mensajes ACL de tipo INFORM con la ontología correcta
        private final MessageTemplate filtro = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.MatchOntology("career-advisor-ontology")
        );

        @Override
        public void action() {
            // Recepción NO bloqueante + block() — filtro en modo bloqueante
            ACLMessage msg = myAgent.receive(filtro);

            if (msg != null) {
                System.out.println("[AgenteVisualizacion] Mensaje INFORM recibido de: "
                    + msg.getSender().getLocalName());

                String jsonContent = msg.getContent();
                ResultadoParsed resultado = parsearJSON(jsonContent);

                if (resultado == null) {
                    System.err.println("[AgenteVisualizacion] Error parseando el resultado JSON.");
                    return;
                }

                // Mostrar por consola
                mostrarConsola(resultado);

                // Lanzar GUI en el hilo de Swing (EDT)
                final ResultadoParsed r = resultado;
                SwingUtilities.invokeLater(() -> {
                    new ResultadoGUI(r.intereses, r.ranking);
                });

            } else {
                // Sin mensaje — bloquear este behaviour hasta que llegue uno nuevo
                block();
            }
        }

        // -----------------------------------------------------------------------
        // Parser JSON ligero (sin dependencias externas)
        // -----------------------------------------------------------------------
        private ResultadoParsed parsearJSON(String json) {
            try {
                ResultadoParsed r = new ResultadoParsed();

                // Extraer campo "intereses"
                Pattern pIntereses = Pattern.compile("\"intereses\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
                Matcher mIntereses = pIntereses.matcher(json);
                r.intereses = mIntereses.find() ? unescapeJson(mIntereses.group(1)) : "(desconocido)";

                Pattern pDesint = Pattern.compile("\"desintereses\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
                Matcher mDesint = pDesint.matcher(json);
                r.desintereses = mDesint.find() ? unescapeJson(mDesint.group(1)) : "";

                // Extraer array "ranking"
                Pattern pItem = Pattern.compile(
                    "\\{\\s*\"nombre\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
                    + "\\s*,\\s*\"emoji\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
                    + "\\s*,\\s*\"descripcion\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
                    + "\\s*,\\s*\"score\"\\s*:\\s*([\\d.]+)"
                    + "\\s*,\\s*\"keywords_matched\"\\s*:\\s*(\\d+)"
                    + "\\s*,\\s*\"keywords_penalty\"\\s*:\\s*(\\d+)"
                    + "\\s*\\}"
                );
                Matcher mItem = pItem.matcher(json);
                r.ranking = new ArrayList<>();
                while (mItem.find()) {
                    long penalty = mItem.groupCount() >= 6
                        ? Long.parseLong(mItem.group(6)) : 0L;
                    r.ranking.add(new CarreraResultado(
                        unescapeJson(mItem.group(1)),
                        unescapeJson(mItem.group(2)),
                        unescapeJson(mItem.group(3)),
                        Double.parseDouble(mItem.group(4)),
                        Long.parseLong(mItem.group(5)),
                        penalty
                    ));
                }
                return r;
            } catch (Exception e) {
                System.err.println("[AgenteVisualizacion] Excepción al parsear JSON: " + e.getMessage());
                return null;
            }
        }

        private String unescapeJson(String s) {
            return s.replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r");
        }

        // -----------------------------------------------------------------------
        // Salida por consola
        // -----------------------------------------------------------------------
        private void mostrarConsola(ResultadoParsed r) {
            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║          CARREERADVISOR MAS — RESULTADOS                 ║");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║ Intereses:    " + truncar(r.intereses,    45));
            if (r.desintereses != null && !r.desintereses.isEmpty()) {
                System.out.println("║ Desintereses: " + truncar(r.desintereses, 45));
            }
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            String[] medals = {"🥇", "🥈", "🥉", " 4.", " 5."};
            for (int i = 0; i < r.ranking.size(); i++) {
                CarreraResultado c = r.ranking.get(i);
                String medal = i < 3 ? medals[i] : medals[i];
                System.out.printf("║ %s %-30s %5.1f%%  (%2d kw)%n",
                    medal, truncar(c.nombre, 30), c.score * 100, c.keywordsMatched);
            }
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println();
        }

        private String truncar(String s, int max) {
            if (s == null) return "";
            return s.length() <= max ? s : s.substring(0, max - 3) + "...";
        }
    }

    // -------------------------------------------------------------------------
    // Estructura auxiliar para el resultado parseado
    // -------------------------------------------------------------------------
    static class ResultadoParsed {
        String intereses;
        String desintereses;
        List<CarreraResultado> ranking;
    }
}
