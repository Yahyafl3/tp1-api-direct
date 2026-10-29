package ma.emsi.yahya.tp1apidirect.jsf;


import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
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

    @Inject
    private GenerateurReponse generateurReponse;

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

    // Verifie la question, appelle le service bonus et affiche le resultat.
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

        if (generateurReponse == null) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Service indisponible",
                    "Le service de generation de reponse n'est pas disponible."
            );

            facesContext.addMessage(null, message);
            return null;
        }

        // Le role reste fixe pendant toute la conversation.
        if (conversation.isEmpty()) {
            roleSystemeChangeable = false;
        }

        // Le service recoit le role choisi et le texte de la question.
        reponse = generateurReponse.genererReponse(
                roleSysteme,
                question
        );

        // On ajoute le nouvel echange a l'historique.
        afficherConversation();

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

    // Prepare les trois roles disponibles dans la liste.
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
        }

        return listeRolesSysteme;
    }
}
