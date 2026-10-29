package ma.emsi.yahya.tp1apidirect.llm;

public record LlmInteraction(
        String questionJson,
        String reponseJson,
        String reponseExtraite
) {
}