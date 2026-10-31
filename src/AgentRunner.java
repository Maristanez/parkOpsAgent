import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Runs one bounded conversation: ask the provider, check the reply, run the tool through the
// application's public methods, send the observation back, repeat. Stops at a pending proposal.
// It never approves or executes; those are separate operator actions.
final class AgentRunner {
    static final String GOAL = "Resolve the booking affected by the closure of A17.";
    static final int MIN_CALLS = 1;
    static final int MAX_CALLS = 6;

    private final StudentApplication app;
    private final ModelProvider provider;
    private final int limit;
    private final List<ChatMessage> history = new ArrayList<>();
    private int calls;
    private int invalidResponses;
    // the read -> list -> propose order, tracked for this run only
    private boolean hasRead;
    private boolean hasListed;
    // the versions the model saw at its last READ_BOOKING
    private int readBookingVersion;
    private int readPolicyVersion;

    AgentRunner(StudentApplication app, ModelProvider provider, int limit) {
        if (limit < MIN_CALLS || limit > MAX_CALLS) {
            throw new IllegalArgumentException("call limit must be " + MIN_CALLS + ".." + MAX_CALLS + ", got: " + limit);
        }
        this.app = app;
        this.provider = provider;
        this.limit = limit;
    }

    AgentResult run() {
        String goalBookingId = (String) app.bookingSnapshot().get("id");
        history.add(new ChatMessage("system", systemPrompt(goalBookingId)));
        history.add(new ChatMessage("user", "Goal: " + GOAL + " Goal booking: " + goalBookingId + "."));
        while (calls < limit) {
            String reply;
            try {
                calls++;
                reply = provider.next(List.copyOf(history));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return finish(AgentOutcome.PROVIDER_ERROR, null);
            } catch (Exception ex) {
                // includes timeouts; a provider failure never commits anything
                return finish(AgentOutcome.PROVIDER_ERROR, null);
            }
            history.add(new ChatMessage("assistant", reply == null ? "" : reply));
            ToolRequestParser.Result parsed = ToolRequestParser.parse(reply, goalBookingId, app.spaceIds());
            if (!parsed.accepted()) {
                // an invalid reply still used a call; the model gets the reason and may try again
                invalidResponses++;
                observe(rejected(parsed.rejection()));
                continue;
            }
            ToolRequest request = parsed.request();
            if (request.tool() == Tool.READ_BOOKING) {
                Map<String, Object> observation = ok(request.tool());
                observation.put("booking", app.bookingSnapshot());
                observation.put("policyVersion", app.policyVersion());
                observe(observation);
                hasRead = true;
                hasListed = false;   // a new read restarts the investigation
                readBookingVersion = currentBookingVersion();
                readPolicyVersion = app.policyVersion();
            } else if (request.tool() == Tool.LIST_ALTERNATIVES) {
                if (!hasRead) {
                    observe(toolError("LIST_ALTERNATIVES requires READ_BOOKING first"));
                    continue;
                }
                if (isStale()) {
                    observe(staleError());
                    continue;
                }
                Map<String, Object> observation = ok(request.tool());
                observation.put("alternatives", app.alternatives());
                observe(observation);
                hasListed = true;
            } else if (request.tool() == Tool.PROPOSE_REASSIGNMENT) {
                if (!hasRead || !hasListed) {
                    observe(toolError("PROPOSE_REASSIGNMENT requires READ_BOOKING then LIST_ALTERNATIVES first"));
                    continue;
                }
                if (isStale()) {
                    observe(staleError());
                    continue;
                }
                String active = activeProposalId();
                if (active != null) {
                    observe(toolError("proposal " + active + " is already waiting for the operator for this "
                                      + "booking and policy version; no new proposal is issued"));
                    continue;
                }
                ProposalView proposal;
                try {
                    proposal = app.propose(request.targetId());
                } catch (IllegalArgumentException ex) {
                    // the application refused the target (e.g. occupied); the model may try again
                    observe(toolError(ex.getMessage()));
                    continue;
                }
                Map<String, Object> observation = new LinkedHashMap<>();
                observation.put("status", "AWAITING_APPROVAL");
                observation.put("proposalId", proposal.proposalId());
                observe(observation);
                return finish(AgentOutcome.AWAITING_APPROVAL, proposal.proposalId());
            } else {
                Map<String, Object> observation = new LinkedHashMap<>();
                observation.put("status", "NO_ACTION");
                observe(observation);
                return finish(AgentOutcome.NO_ACTION, null);
            }
        }
        return finish(AgentOutcome.STEP_LIMIT, null);
    }

    private int currentBookingVersion() {
        return (Integer) app.bookingSnapshot().get("version");
    }

    // A PENDING or APPROVED proposal for the booking's current version and policy version, or null
    private String activeProposalId() {
        for (ProposalView proposal : app.proposals()) {
            boolean active = proposal.status().equals("PENDING") || proposal.status().equals("APPROVED");
            if (active && proposal.bookingVersion() == currentBookingVersion()
                    && proposal.policyVersion() == app.policyVersion()) {
                return proposal.proposalId();
            }
        }
        return null;
    }

    // True when the booking or policy changed since the model's last read
    private boolean isStale() {
        return currentBookingVersion() != readBookingVersion || app.policyVersion() != readPolicyVersion;
    }

    // A stale investigation restarts: the model has to read again before listing or proposing
    private Map<String, Object> staleError() {
        hasRead = false;
        hasListed = false;
        return toolError("booking or policy version changed since READ_BOOKING; call READ_BOOKING again");
    }

    private static Map<String, Object> ok(Tool tool) {
        Map<String, Object> observation = new LinkedHashMap<>();
        observation.put("status", "OK");
        observation.put("tool", tool.name());
        return observation;
    }

    private static Map<String, Object> rejected(String reason) {
        Map<String, Object> observation = new LinkedHashMap<>();
        observation.put("status", "REJECTED");
        observation.put("reason", reason);
        return observation;
    }

    // A valid request that broke a tool rule; it used a call but is not an invalid reply
    private static Map<String, Object> toolError(String reason) {
        Map<String, Object> observation = new LinkedHashMap<>();
        observation.put("status", "TOOL_ERROR");
        observation.put("reason", reason);
        return observation;
    }

    // Tool results go back to the model as JSON in a user message
    private void observe(Map<String, Object> observation) {
        history.add(new ChatMessage("user", MiniJson.stringify(observation)));
    }

    private AgentResult finish(AgentOutcome outcome, String proposalId) {
        return new AgentResult(outcome, calls, invalidResponses, proposalId, history);
    }

    private static String systemPrompt(String goalBookingId) {
        return "You resolve parking bookings by calling tools. Reply with exactly one JSON object and nothing else: "
             + "{\"tool\":\"<TOOL>\",\"bookingId\":\"" + goalBookingId + "\",\"targetId\":\"<SPACE or empty>\"}. "
             + "Tools: READ_BOOKING (targetId empty) returns the booking and policy version; "
             + "LIST_ALTERNATIVES (targetId empty, after READ_BOOKING) returns eligible spaces; "
             + "PROPOSE_REASSIGNMENT (targetId = one listed space, after LIST_ALTERNATIVES) creates a proposal "
             + "that a human operator must approve; FINISH (targetId empty) stops without changes. "
             + "You cannot approve or execute anything.";
    }
}
