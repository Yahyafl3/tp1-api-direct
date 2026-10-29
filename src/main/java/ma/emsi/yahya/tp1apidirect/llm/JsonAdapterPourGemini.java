
package ma.emsi.yahya.tp1apidirect.llm;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.json.*;
import jakarta.json.stream.JsonGenerator;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe pour gérer le JSON des requêtes à l'API de Gemini.
 * Contient l'état JSON de la conversation et des méthodes pour manipuler le JSON.
 */
@Dependent
public class JsonAdapterPourGemini implements Serializable {

    private String systemRole;

    /**
     * Pour ajouter une nouvelle valeur à la fin du tableau JSON "contents".
     */
    private final JsonPointer pointer = Json.createPointer("/contents/-");

    /**
     * Requête JSON, à partir du prompt de l'utilisateur.
     */
    private JsonObject requeteJson;
    private String texteRequeteJson;

    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;
    }

    /**
     * Pour envoyer une requête à l'API de Gemini.
     */
    @Inject
    private LlmClientPourGemini geminiClient;

    /**
     * Envoie une requête à l'API de Gemini.
     *
     * @param question question posée par l'utilisateur
     * @return la réponse de l'API sous forme de texte et de JSON
     * @throws RequeteException si la requête est rejetée par l'API
     */
    public LlmInteraction envoyerRequete(String question)
            throws RequeteException {

        String requestBody;

        if (this.requeteJson == null) {
            // Si c'est la première question, crée la requête JSON avec le rôle système.
            requestBody = creerRequeteJson(this.systemRole, question);
        } else {
            // Ajoute la nouvelle question à la conversation.
            requestBody = ajouteQuestionDansJsonRequete(question);
        }

        Entity<String> entity =
                Entity.entity(requestBody, MediaType.APPLICATION_JSON_TYPE);

        // Mise en forme de la requête JSON pour l'affichage.
        this.texteRequeteJson = prettyPrinting(requeteJson);

        // Envoi de la requête à Gemini.
        try (Response response = geminiClient.envoyerRequete(entity)) {

            String texteReponseJson = response.readEntity(String.class);

            if (response.getStatus() == 200) {

                String reponseExtraite = extractReponse(texteReponseJson);

                // Ordre adapté au record LlmInteraction.
                return new LlmInteraction(
                        this.texteRequeteJson,
                        texteReponseJson,
                        reponseExtraite
                );

            } else {
                JsonObject objet = Json.createReader(
                        new StringReader(requestBody)
                ).readObject();

                throw new RequeteException(
                        response.getStatus() + " : "
                                + response.getStatusInfo()
                                + " Réponse API : "
                                + texteReponseJson,
                        prettyPrinting(objet)
                );
            }
        }
    }

    /**
     * Crée une requête JSON avec le rôle système et la question.
     */
    private String creerRequeteJson(String systemRole, String question) {

        JsonArray systemInstructionParts = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("text", systemRole))
                .build();

        JsonObject systemInstruction = Json.createObjectBuilder()
                .add("parts", systemInstructionParts)
                .build();

        JsonArray userContentParts = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("text", question))
                .build();

        JsonObject userContent = Json.createObjectBuilder()
                .add("role", "user")
                .add("parts", userContentParts)
                .build();

        JsonArray contents = Json.createArrayBuilder()
                .add(userContent)
                .build();

        JsonObject rootJson = Json.createObjectBuilder()
                .add("system_instruction", systemInstruction)
                .add("contents", contents)
                .build();

        this.requeteJson = rootJson;

        return rootJson.toString();
    }

    /**
     * Ajoute une nouvelle question à la conversation existante.
     */
    private String ajouteQuestionDansJsonRequete(String nouvelleQuestion) {

        JsonObject nouveauMessageJson = Json.createObjectBuilder()
                .add("text", nouvelleQuestion)
                .build();

        JsonObjectBuilder newPartBuilder = Json.createObjectBuilder()
                .add("role", "user")
                .add("parts", Json.createArrayBuilder()
                        .add(nouveauMessageJson)
                        .build());

        this.requeteJson = this.pointer.add(
                this.requeteJson,
                newPartBuilder.build()
        );

        this.texteRequeteJson = prettyPrinting(requeteJson);

        return this.requeteJson.toString();
    }

    /**
     * Retourne le JSON formaté pour un affichage plus lisible.
     */
    private String prettyPrinting(JsonObject jsonObject) {

        Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);

        JsonWriterFactory writerFactory =
                Json.createWriterFactory(config);

        StringWriter stringWriter = new StringWriter();

        try (JsonWriter jsonWriter =
                     writerFactory.createWriter(stringWriter)) {
            jsonWriter.write(jsonObject);
        }

        return stringWriter.toString();
    }

    /**
     * Extrait la réponse de Gemini et l'ajoute à l'historique.
     */
    private String extractReponse(String json) {

        try (JsonReader jsonReader =
                     Json.createReader(new StringReader(json))) {

            JsonObject jsonObject = jsonReader.readObject();

            JsonObject messageReponse = jsonObject
                    .getJsonArray("candidates")
                    .getJsonObject(0)
                    .getJsonObject("content");

            // Ajoute la réponse de Gemini à l'historique.
            this.requeteJson = this.pointer.add(
                    this.requeteJson,
                    messageReponse
            );

            // Extrait uniquement le texte de la réponse.
            return messageReponse
                    .getJsonArray("parts")
                    .getJsonObject(0)
                    .getString("text");
        }
    }
}
