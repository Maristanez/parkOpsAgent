import java.util.List;
import java.util.Map;
import java.util.Set;

// Turns a model reply (untrusted text) into a checked ToolRequest, or a reason it was rejected.
// It never runs anything and never touches stored state.
public final class ToolRequestParser {
    // Exactly these three properties, all strings
    private static final List<String> PROPERTIES = List.of("tool", "bookingId", "targetId");

    private ToolRequestParser() {}

    // Either a checked request or a rejection reason, never both
    public record Result(ToolRequest request, String rejection) {
        static Result accept(ToolRequest request) { return new Result(request, null); }
        static Result reject(String reason) { return new Result(null, reason); }
        public boolean accepted() { return request != null; }
    }

    public static Result parse(String text, String goalBookingId, Set<String> knownSpaceIds) {
        Map<String, Object> fields;
        try {
            fields = MiniJson.object(MiniJson.parse(text));
        } catch (IllegalArgumentException ex) {
            return Result.reject("malformed JSON: " + ex.getMessage());
        }
        for (String key : fields.keySet()) {
            if (!PROPERTIES.contains(key)) {
                return Result.reject("unexpected property: " + key);
            }
        }
        for (String key : PROPERTIES) {
            if (!fields.containsKey(key)) {
                return Result.reject("missing property: " + key);
            }
        }
        for (String key : PROPERTIES) {
            if (!(fields.get(key) instanceof String)) {
                return Result.reject("property " + key + " must be a string");
            }
        }
        Tool tool = toolNamed((String) fields.get("tool"));
        if (tool == null) {
            return Result.reject("unknown tool: " + fields.get("tool"));
        }
        String bookingId = (String) fields.get("bookingId");
        if (!bookingId.equals(goalBookingId)) {
            return Result.reject("bookingId must be the goal booking " + goalBookingId + ", got: " + bookingId);
        }
        String targetId = (String) fields.get("targetId");
        if (tool == Tool.PROPOSE_REASSIGNMENT) {
            if (!knownSpaceIds.contains(targetId)) {
                return Result.reject("targetId must be a known space, got: \"" + targetId + "\"");
            }
        } else if (!targetId.isEmpty()) {
            return Result.reject("targetId must be empty for " + tool);
        }
        return Result.accept(new ToolRequest(tool, bookingId, targetId));
    }

    // Exact, case-sensitive match against the four tools; null when there is none
    private static Tool toolNamed(String name) {
        for (Tool tool : Tool.values()) {
            if (tool.name().equals(name)) {
                return tool;
            }
        }
        return null;
    }
}
