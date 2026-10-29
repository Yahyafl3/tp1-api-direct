
package ma.emsi.yahya.tp1apidirect.llm;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.Serializable;

@Dependent
public class LlmClientPourGemini implements Serializable {

    private final String key;
    private Client clientRest;
    private final WebTarget target;

    public LlmClientPourGemini() {
        // Récupération de la clé API depuis les variables d'environnement
        this.key = System.getenv("GEMINI_KEY");

        this.clientRest = ClientBuilder.newClient();

        // URL de l'API Gemini à confirmer avec le support du professeur
        this.target = clientRest.target(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"
        );
    }

    public Response envoyerRequete(Entity requestEntity) {
        Invocation.Builder request = target
                .queryParam("key", key)
                .request(MediaType.APPLICATION_JSON_TYPE);

        return request.post(requestEntity);
    }

    public void closeClient() {
        this.clientRest.close();
    }
}
