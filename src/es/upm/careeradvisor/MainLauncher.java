package es.upm.careeradvisor;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

/**
 * Punto de entrada principal del sistema CareerAdvisor-MAS.
 *
 * Lanza la plataforma JADE con GUI e instancia los tres agentes:
 *   1. AgenteVisualizacion  — se registra primero (receptor del INFORM)
 *   2. AgenteConocimiento   — procesador (receptor del REQUEST, emisor del INFORM)
 *   3. AgentePercepcion     — adquisición (emisor del REQUEST)
 *
 * Uso desde Eclipse:
 *   Run As > Java Application
 *   Main class: es.upm.careeradvisor.MainLauncher
 *
 * Uso desde línea de comandos (tras compilar):
 *   java -cp bin:jade.jar es.upm.careeradvisor.MainLauncher
 */
public class MainLauncher {

    public static void main(String[] args) throws Exception {
        System.out.println("╔═══════════════════════════════════════════════╗");
        System.out.println("║     CareerAdvisor-MAS  —  Iniciando...        ║");
        System.out.println("╚═══════════════════════════════════════════════╝");

        // 1. Obtener el runtime de JADE
        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);

        // 2. Configurar el contenedor principal con GUI
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.MAIN_PORT, "1099");
        profile.setParameter(Profile.GUI, "true");

        ContainerController container = rt.createMainContainer(profile);

        // 3. Crear agentes en orden: primero los receptores, luego el emisor
        try {
            // AgenteVisualizacion — debe estar listo antes de que llegue el INFORM
            AgentController agViz = container.createNewAgent(
                "agenteVisualizacion",
                "es.upm.careeradvisor.agents.AgenteVisualizacion",
                null
            );
            agViz.start();
            System.out.println("[Launcher] AgenteVisualizacion iniciado.");

            // Pausa para que el agente se registre en el DF
            Thread.sleep(500);

            // AgenteConocimiento — debe estar listo antes de que llegue el REQUEST
            AgentController agCon = container.createNewAgent(
                "agenteConocimiento",
                "es.upm.careeradvisor.agents.AgenteConocimiento",
                null
            );
            agCon.start();
            System.out.println("[Launcher] AgenteConocimiento iniciado.");

            // Pausa para que el agente se registre en el DF
            Thread.sleep(500);

            // AgentePercepcion — inicia la cadena de mensajes
            AgentController agPer = container.createNewAgent(
                "agentePercepcion",
                "es.upm.careeradvisor.agents.AgentePercepcion",
                null
            );
            agPer.start();
            System.out.println("[Launcher] AgentePercepcion iniciado.");

        } catch (StaleProxyException e) {
            System.err.println("[Launcher] Error al crear agentes: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
