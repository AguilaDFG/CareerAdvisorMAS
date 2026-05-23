package es.upm.careeradvisor;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

/**
 * Punto de entrada del sistema CareerAdvisor-MAS (rediseño Contract-Net).
 *
 * Agentes creados (en orden):
 *  1. AgenteVisualizacion           — receptor final del ranking
 *  2. AgenteKB_tecnologia           — KB dominio Tecnología
 *  3. AgenteKB_ciencias             — KB dominio Ciencias
 *  4. AgenteKB_humanidades          — KB dominio Humanidades
 *  5. AgenteKB_salud                — KB dominio Salud
 *  6. AgenteKB_arte                 — KB dominio Arte
 *  7. AgenteCoordinador             — inicia Contract-Net
 *  8. AgentePercepcion              — dispara la cadena (último)
 *
 * Todos los agentes deben estar registrados en el DF antes de que
 * AgentePercepcion envíe el primer REQUEST; las pausas garantizan ese orden.
 */
public class MainLauncher {

    public static void main(String[] args) throws Exception {
        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.println("║   CareerAdvisor-MAS  —  Arquitectura Contract-Net     ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");

        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);

        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.MAIN_PORT, "1099");
        profile.setParameter(Profile.GUI, "true");

        ContainerController cc = rt.createMainContainer(profile);

        try {
            start(cc, "agenteVisualizacion",
                "es.upm.careeradvisor.agents.AgenteVisualizacion", null);
            pause(400);

            // Cinco agentes KB especializados, cada uno con su dominio como argumento
            for (String dom : new String[]{"tecnologia","ciencias","humanidades","salud","arte"}) {
                start(cc, "agenteKB_" + dom,
                    "es.upm.careeradvisor.agents.AgenteKB",
                    new Object[]{dom});
                pause(300);
            }

            start(cc, "agenteCoordinador",
                "es.upm.careeradvisor.agents.AgenteCoordinador", null);
            pause(400);

            // AgentePercepcion al final: dispara la cadena de mensajes
            start(cc, "agentePercepcion",
                "es.upm.careeradvisor.agents.AgentePercepcion", null);

        } catch (StaleProxyException e) {
            System.err.println("[Launcher] Error creando agentes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void start(ContainerController cc, String name,
                               String clazz, Object[] args) throws StaleProxyException {
        AgentController ac = cc.createNewAgent(name, clazz, args);
        ac.start();
        System.out.println("[Launcher] ✓ " + name);
    }

    private static void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
