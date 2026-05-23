package es.upm.careeradvisor.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;

/**
 * AgentePercepcion
 * ─────────────────
 * Adquiere los intereses y desintereses del usuario (fichero o GUI Swing)
 * y envía un único mensaje ACL REQUEST al AgenteCoordinador.
 *
 * Servicio DF registrado : "percepcion-intereses"
 * Mensaje enviado         : ACL REQUEST, ontología "career-advisor"
 * Contenido               : <intereses> + SEPARADOR + <desintereses>
 */
public class AgentePercepcion extends Agent {

    public static final String SERVICE_TYPE   = "percepcion-intereses";
    public static final String SEPARADOR      = "||DESINTERESES||";

    private static final String FILE_INTERESES    = "resources/intereses.txt";
    private static final String FILE_DESINTERESES = "resources/desintereses.txt";

    @Override
    protected void setup() {
        System.out.println("[Percepcion] Iniciando: " + getLocalName());
        registrarDF();
        addBehaviour(new AdquirirBehaviour());
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (FIPAException ignored) {}
        System.out.println("[Percepcion] Finalizado.");
    }

    // ── Registro DF ─────────────────────────────────────────────────────
    private void registrarDF() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType(SERVICE_TYPE);
            sd.setName(SERVICE_TYPE);
            dfd.addServices(sd);
            DFService.register(this, dfd);
            System.out.println("[Percepcion] Registrado en DF: " + SERVICE_TYPE);
        } catch (FIPAException e) {
            System.err.println("[Percepcion] Error DF: " + e.getMessage());
            doDelete();
        }
    }

    // ── Búsqueda en DF ──────────────────────────────────────────────────
    private AID buscarAgente(String tipo) {
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
            System.err.println("[Percepcion] Error buscando '" + tipo + "': " + e.getMessage());
        }
        return null;
    }

    // ── Behaviour principal ──────────────────────────────────────────────
    private class AdquirirBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            String intereses    = leerODialogo(FILE_INTERESES,    "intereses",
                "Describe tus <b>intereses, aficiones y gustos</b>:",
                "Me encanta programar, resolver problemas lógicos y crear aplicaciones web. " +
                "También me interesan los algoritmos y la inteligencia artificial.");
            if (intereses == null || intereses.trim().isEmpty()) {
                System.err.println("[Percepcion] Sin intereses. Abortando.");
                return;
            }
            String desintereses = leerODialogo(FILE_DESINTERESES, "desintereses",
                "Describe lo que <b>NO te gusta</b> o quieres evitar (opcional):",
                "No me gusta la biología ni trabajar con pacientes.");
            if (desintereses == null) desintereses = "";

            System.out.println("[Percepcion] Intereses:    \"" + intereses    + "\"");
            System.out.println("[Percepcion] Desintereses: \"" + desintereses + "\"");

            doWait(1500); // margen para que los demás agentes se registren

            AID coordinador = buscarAgente(AgenteCoordinador.SERVICE_TYPE);
            if (coordinador == null) {
                JOptionPane.showMessageDialog(null,
                    "No se encontró el AgenteCoordinador.\nAsegúrate de que la plataforma está activa.",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(coordinador);
            msg.setOntology("career-advisor");
            msg.setLanguage("plain-text");
            msg.setContent(intereses + SEPARADOR + desintereses);
            send(msg);
            System.out.println("[Percepcion] REQUEST enviado a " + coordinador.getLocalName());
        }

        private String leerODialogo(String path, String tipo, String instruccion, String placeholder) {
            File f = new File(path);
            if (f.exists() && f.isFile()) {
                try {
                    String txt = new String(Files.readAllBytes(f.toPath()), "UTF-8").trim();
                    if (!txt.isEmpty()) {
                        System.out.println("[Percepcion] " + tipo + " leídos de: " + path);
                        return txt;
                    }
                } catch (IOException e) {
                    System.err.println("[Percepcion] Error leyendo " + path + ": " + e.getMessage());
                }
            }
            return mostrarDialogo(tipo, instruccion, placeholder);
        }

        private String mostrarDialogo(String tipo, String instruccion, String placeholder) {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            JLabel lTitulo = new JLabel("<html><b>CareerAdvisor MAS</b> — " + tipo + "</html>");
            lTitulo.setFont(new Font("SansSerif", Font.PLAIN, 14));
            panel.add(lTitulo, BorderLayout.NORTH);

            JLabel lInst = new JLabel("<html><br>" + instruccion + "</html>");
            lInst.setFont(new Font("SansSerif", Font.PLAIN, 12));
            panel.add(lInst, BorderLayout.CENTER);

            JTextArea area = new JTextArea(6, 42);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setFont(new Font("SansSerif", Font.PLAIN, 13));
            area.setText(placeholder);
            panel.add(new JScrollPane(area), BorderLayout.SOUTH);

            int r = JOptionPane.showConfirmDialog(null, panel,
                "CareerAdvisor — " + tipo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            return r == JOptionPane.OK_OPTION ? area.getText().trim() : null;
        }
    }
}
