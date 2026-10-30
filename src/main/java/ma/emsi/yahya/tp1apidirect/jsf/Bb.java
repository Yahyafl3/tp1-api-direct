
package ma.emsi.yahya.tp1apidirect.jsf;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ma.emsi.yahya.tp1apidirect.llm.JsonAdapterPourGemini;
import ma.emsi.yahya.tp1apidirect.llm.LlmInteraction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Named
@ViewScoped
public class Bb implements Serializable {

    private static final long serialVersionUID = 1L;

    // Role choisi dans la liste deroulante.
    private String roleSysteme;

    // On bloque le choix du role apres le premier message.
    private boolean roleSystemeChangeable = true;

    private List<SelectItem> listeRolesSysteme;

    // Question saisie et reponse generee.
    private String question;
    private String reponse;

    // Garde les messages echanges pendant le chat.
    private StringBuilder conversation = new StringBuilder();

    // Mode de debogage et affichage des JSON.
    private boolean debug;
    private String texteRequeteJson;
    private String texteReponseJson;

    @Inject
    private JsonAdapterPourGemini jsonAdapter;

    @Inject
    private FacesContext facesContext;

    public Bb() {
    }

    public String getRoleSysteme() {
        return roleSysteme;
    }

    public void setRoleSysteme(String roleSysteme) {
        this.roleSysteme = roleSysteme;
    }

    public boolean isRoleSystemeChangeable() {
        return roleSystemeChangeable;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getReponse() {
        return reponse;
    }

    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public String getConversation() {
        return conversation.toString();
    }

    public void setConversation(String conversation) {
        this.conversation = new StringBuilder(
                conversation == null ? "" : conversation
        );
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getTexteRequeteJson() {
        return texteRequeteJson;
    }

    public void setTexteRequeteJson(String texteRequeteJson) {
        this.texteRequeteJson = texteRequeteJson;
    }

    public String getTexteReponseJson() {
        return texteReponseJson;
    }

    public void setTexteReponseJson(String texteReponseJson) {
        this.texteReponseJson = texteReponseJson;
    }

    // Verifie la question et appelle l'API Gemini.
    public String envoyer() {

        if (question == null || question.isBlank()) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Question vide",
                    "Veuillez saisir une question."
            );

            facesContext.addMessage(null, message);
            return null;
        }

        if (roleSysteme == null || roleSysteme.isBlank()) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Role non selectionne",
                    "Veuillez choisir un role dans la liste."
            );

            facesContext.addMessage(null, message);
            return null;
        }

        try {
            // Transmet le role systeme au debut de la conversation.
            if (conversation.isEmpty()) {
                jsonAdapter.setSystemRole(roleSysteme);
            }

            LlmInteraction interaction =
                    jsonAdapter.envoyerRequete(question);

            this.reponse = interaction.reponseExtraite();
            this.texteRequeteJson = interaction.questionJson();
            this.texteReponseJson = interaction.reponseJson();

            // Le role reste fixe apres le premier echange reussi.
            roleSystemeChangeable = false;

            // Ajoute le nouvel echange a l'historique.
            afficherConversation();

        } catch (Exception e) {
            this.reponse = "Erreur : " + e.getMessage();

            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Problème de connexion avec l'API du LLM",
                    "Problème de connexion avec l'API du LLM : "
                            + e.getMessage() + "\n"
                            + Arrays.toString(e.getStackTrace())
            );

            facesContext.addMessage(null, message);
        }

        return null;
    }

    // Demarre une nouvelle conversation.
    public String nouveauChat() {
        return "index?faces-redirect=true";
    }

    // Conserve les questions et les reponses dans l'ordre.
    private void afficherConversation() {
        conversation.append("== User:\n")
                .append(question)
                .append("\n== Serveur:\n")
                .append(reponse)
                .append("\n\n");
    }

    // Prepare les quatre roles disponibles dans la liste.
    public List<SelectItem> getRolesSysteme() {

        if (listeRolesSysteme == null) {
            listeRolesSysteme = new ArrayList<>();

            String role = """
                    You are a helpful assistant.
                    You help the user to find the information they need.
                    If the user asks a question, answer it.
                    """;

            listeRolesSysteme.add(
                    new SelectItem(role, "Assistant")
            );

            role = """
                    You are an interpreter.
                    Translate from English to French and from French to English.
                    If the user writes in French, translate into English.
                    If the user writes in English, translate into French.
                    """;

            listeRolesSysteme.add(
                    new SelectItem(role, "Traducteur Anglais-Français")
            );

            role = """
                    You are a travel guide.
                    If the user gives the name of a country or a town,
                    suggest the main places to visit.
                    """;

            listeRolesSysteme.add(
                    new SelectItem(role, "Guide touristique")
            );

            role = """
                    Tu es le Maître des Énigmes, un expert dans l'art de
                    transmettre les connaissances à travers des mystères
                    et des défis intellectuels.

                    Ton objectif est de transformer chaque question de
                    l'utilisateur en une petite aventure de réflexion.

                    Pour chaque demande :
                    1. Présente le sujet sous la forme d'un mystère à résoudre.
                    2. Donne un premier indice pour orienter la réflexion.
                    3. Explique progressivement les concepts nécessaires.
                    4. Propose une énigme ou un défi en rapport avec le sujet.
                    5. Après la réponse de l'utilisateur, explique la solution
                       et les erreurs éventuelles avec bienveillance.

                    Adapte la difficulté au niveau de l'utilisateur.
                    Ne révèle pas immédiatement la solution, sauf si
                    l'utilisateur la demande explicitement.
                    Sois captivant, pédagogique et créatif.

                    Termine chaque réponse par une section intitulée
                    « Le prochain mystère » contenant un nouveau défi
                    lié au sujet.
                    """;

            listeRolesSysteme.add(
                    new SelectItem(role, "Le Maître des Énigmes")
            );
        }

        return listeRolesSysteme;
    }
}
