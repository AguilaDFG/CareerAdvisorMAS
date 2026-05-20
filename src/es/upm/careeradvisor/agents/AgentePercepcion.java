package es.upm.careeradvisor.agents;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;

/**
 * Agente de Percepción (AgentePercepcion).
 *
 * Responsabilidad: Adquirir los intereses Y desintereses del usuario.
 * Fuentes posibles:
 *   1. Ficheros resources/intereses.txt y resources/desintereses.txt si existen.
 *   2. Interfaz gráfica Swing para introducción manual de ambos textos.
 *
 * El contenido ACL enviado al AgenteConocimiento usa el separador
 * SEPARADOR para dividir intereses y desintereses en un único campo.
 *
 * Registra en el DF el servicio "percepcion-intereses".
 */
public class AgentePercepcion extends Agent {

    public static final String SERVICE_TYPE    = "percepcion-intereses";
    public static final String SEPARADOR        = "||DESINTERESES||";
    private static final String FILE_INTERESES    = "resources/intereses.txt";
    private static final String FILE_DESINTERESES = "resources/desintereses.txt";

    @Override
    protected void setup() {
        System.out.println("[AgentePercepcion] Iniciando agente: " + getLocalName());
        registrarEnDF();
        addBehaviour(new AdquirirInteresesBehaviour());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            System.err.println("[AgentePercepcion] Error al desregistrarse del DF: " + e.getMessage());
        }
        System.out.println("[AgentePercepcion] Agente finalizado.");
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
            dfd.addServices(sd);
            DFService.register(this, dfd);
            System.out.println("[AgentePercepcion] Registrado en DF con servicio: " + SERVICE_TYPE);
        } catch (FIPAException e) {
            System.err.println("[AgentePercepcion] Error al registrarse en el DF: " + e.getMessage());
            doDelete();
        }
    }

    // -------------------------------------------------------------------------
    // Busca un agente que ofrezca el tipo de servicio indicado
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
                System.out.println("[AgentePercepcion] Agente encontrado para servicio '"
                    + tipoServicio + "': " + agente.getLocalName());
                return agente;
            }
        } catch (FIPAException e) {
            System.err.println("[AgentePercepcion] Error buscando servicio '" + tipoServicio + "': " + e.getMessage());
        }
        System.err.println("[AgentePercepcion] No se encontró agente para el servicio: " + tipoServicio);
        return null;
    }

    // -------------------------------------------------------------------------
    // Behaviour principal: adquiere intereses + desintereses y los envía
    // -------------------------------------------------------------------------
    private class AdquirirInteresesBehaviour extends OneShotBehaviour {

        @Override
        public void action() {
            // Leer intereses (obligatorio) y desintereses (opcional, puede quedar "")
            String intereses    = leerFicheroODialogo(
                FILE_INTERESES,
                "intereses",
                "Describe tus intereses, aficiones y gustos:",
                "Me encanta programar, resolver problemas lógicos y crear aplicaciones web. "
                    + "También me interesan los algoritmos y la inteligencia artificial."
            );
            if (intereses == null || intereses.trim().isEmpty()) {
                System.err.println("[AgentePercepcion] No se obtuvieron intereses. Abortando.");
                return;
            }

            String desintereses = leerFicheroODialogo(
                FILE_DESINTERESES,
                "desintereses",
                "Describe lo que NO te gusta o quieres evitar (opcional):",
                "No me gusta la biología, la medicina ni trabajar con pacientes."
            );
            if (desintereses == null) desintereses = "";

            System.out.println("[AgentePercepcion] Intereses:    \"" + intereses    + "\"");
            System.out.println("[AgentePercepcion] Desintereses: \"" + desintereses + "\"");

            // Espera breve para que el AgenteConocimiento se haya registrado en el DF
            doWait(1500);

            AID receptor = buscarAgente(AgenteConocimiento.SERVICE_TYPE);
            if (receptor == null) {
                JOptionPane.showMessageDialog(null,
                    "No se encontró el AgenteConocimiento en la plataforma.\n"
                    + "Asegúrate de que está en ejecución.",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Contenido = intereses + SEPARADOR + desintereses
            String contenido = intereses + SEPARADOR + desintereses;

            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(receptor);
            msg.setOntology("career-advisor-ontology");
            msg.setLanguage("plain-text");
            msg.setContent(contenido);
            send(msg);

            System.out.println("[AgentePercepcion] Mensaje REQUEST enviado a: " + receptor.getLocalName());
        }

        /**
         * Intenta leer el fichero indicado; si no existe o está vacío,
         * muestra un diálogo Swing con la etiqueta e instrucción dadas.
         *
         * @param fichero     ruta relativa al fichero de texto
         * @param tipo        "intereses" o "desintereses" (para logs)
         * @param instruccion texto que muestra el diálogo al usuario
         * @param placeholder texto de ejemplo prerellenado en el diálogo
         * @return el texto obtenido, o null si el usuario canceló
         */
        private String leerFicheroODialogo(String fichero, String tipo,
                                           String instruccion, String placeholder) {
            File f = new File(fichero);
            if (f.exists() && f.isFile()) {
                try {
                    String contenido = new String(Files.readAllBytes(f.toPath()), "UTF-8").trim();
                    if (!contenido.isEmpty()) {
                        System.out.println("[AgentePercepcion] " + tipo + " leídos desde: " + fichero);
                        return contenido;
                    }
                } catch (IOException e) {
                    System.err.println("[AgentePercepcion] Error leyendo " + fichero + ": " + e.getMessage());
                }
            }
            return mostrarDialogo(tipo, instruccion, placeholder);
        }

        private String mostrarDialogo(String tipo, String instruccion, String placeholder) {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            JLabel titulo = new JLabel(
                "<html><b>CareerAdvisor MAS</b><br>"
                + "<span style=\'font-size:11px;color:#555;\'>Sistema de Recomendación de Carreras</span></html>");
            titulo.setFont(new Font("SansSerif", Font.PLAIN, 14));
            panel.add(titulo, BorderLayout.NORTH);

            JLabel lblInstruccion = new JLabel(
                "<html><br>" + instruccion + "</html>");
            lblInstruccion.setFont(new Font("SansSerif", Font.PLAIN, 12));
            panel.add(lblInstruccion, BorderLayout.CENTER);

            JTextArea area = new JTextArea(6, 40);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setFont(new Font("SansSerif", Font.PLAIN, 13));
            area.setText(placeholder);
            JScrollPane scroll = new JScrollPane(area);
            panel.add(scroll, BorderLayout.SOUTH);

            int result = JOptionPane.showConfirmDialog(null, panel,
                "CareerAdvisor — " + tipo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                return area.getText().trim();
            }
            return null;
        }
    }
}
