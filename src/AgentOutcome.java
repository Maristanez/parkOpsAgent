// How a model/tool run ended. None of these approves or executes anything.
public enum AgentOutcome {
    AWAITING_APPROVAL, NO_ACTION, STEP_LIMIT, PROVIDER_ERROR
}
