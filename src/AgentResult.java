import java.util.List;

// Summary of one run: how it ended, model calls used, rejected replies, the proposal it created
// (null when none) and the full conversation.
public record AgentResult(AgentOutcome outcome, int calls, int invalidResponses,
                          String proposalId, List<ChatMessage> transcript) {
    public AgentResult {
        transcript = List.copyOf(transcript);
    }
}
