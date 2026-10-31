// Add focused @org.junit.jupiter.api.Test methods here.
// Create fresh objects for each test and assert results AND unchanged state on rejection.
// The supplied wrapper is not a substitute for your own test design.
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StudentTests {

    // D1-A: valid durations are kept
    @Test
    void bookingKeepsDurationOne() {
        Booking b = new Booking("B1", "A17", 1, 0, true);
        assertEquals(1, b.getDurationHours());
    }

    @Test
    void bookingKeepsDurationTwentyFour() {
        Booking b = new Booking("B1", "A17", 24, 0, true);
        assertEquals(24, b.getDurationHours());
    }

    // D1-B: invalid durations are rejected
    @Test
    void bookingRejectsDurationZero() {
        assertThrows(IllegalArgumentException.class, () -> new Booking("B1", "A17", 0, 0, true));
    }

    @Test
    void bookingRejectsDurationTwentyFive() {
        assertThrows(IllegalArgumentException.class, () -> new Booking("B1", "A17", 25, 0, true));
    }

    // Invalid identifiers are rejected
    @Test
    void bookingRejectsNullId() {
        assertThrows(IllegalArgumentException.class, () -> new Booking(null, "A17", 3, 0, true));
    }

    @Test
    void bookingRejectsLowercaseId() {
        assertThrows(IllegalArgumentException.class, () -> new Booking("b1", "A17", 3, 0, true));
    }

    @Test
    void bookingRejectsNullSpaceId() {
        assertThrows(IllegalArgumentException.class, () -> new Booking("B1", null, 3, 0, true));
    }

    // Version cannot be negative
    @Test
    void bookingRejectsNegativeVersion() {
        assertThrows(IllegalArgumentException.class, () -> new Booking("B1", "A17", 3, -1, true));
    }

    // Getters return what the constructor received
    @Test
    void bookingKeepsAllFields() {
        Booking b = new Booking("B1", "A17", 3, 0, true);
        assertEquals("B1", b.getId());
        assertEquals("A17", b.getSpaceId());
        assertEquals(3, b.getDurationHours());
        assertEquals(0, b.getVersion());
        assertTrue(b.requiresAccessible());
    }

    // Space availability: open and not occupied
    @Test
    void spaceB12IsAvailable() {
        Space s = new Space("B12", true, false, false);
        assertTrue(s.isAvailable());
    }

    @Test
    void spaceD09IsNotAvailableBecauseOccupied() {
        Space s = new Space("D09", true, true, true);
        assertFalse(s.isAvailable());
    }

    @Test
    void spaceA17IsNotAvailableBecauseClosedAndOccupied() {
        Space s = new Space("A17", false, true, false);
        assertFalse(s.isAvailable());
    }

    @Test
    void spaceClosedButEmptyIsNotAvailable() {
        Space s = new Space("X1", false, false, false);
        assertFalse(s.isAvailable());
    }

    // Invalid space identifiers are rejected
    @Test
    void spaceRejectsNullId() {
        assertThrows(IllegalArgumentException.class, () -> new Space(null, true, false, false));
    }

    @Test
    void spaceRejectsLowercaseId() {
        assertThrows(IllegalArgumentException.class, () -> new Space("b12", true, false, false));
    }

    // Getters return what the constructor received
    @Test
    void spaceKeepsAllFields() {
        Space s = new Space("C03", true, false, true);
        assertEquals("C03", s.getId());
        assertTrue(s.isOpen());
        assertFalse(s.isOccupied());
        assertTrue(s.isAccessible());
    }

    // A new proposal is always pending
    @Test
    void newProposalIsPending() {
        Proposal p = new Proposal("P1", "B1", "B12", 0, 1);
        assertEquals(ProposalStatus.PENDING, p.getStatus());
    }

    // Getters return what the constructor received
    @Test
    void proposalKeepsAllFields() {
        Proposal p = new Proposal("P1", "B1", "B12", 0, 1);
        assertEquals("P1", p.getProposalId());
        assertEquals("B1", p.getBookingId());
        assertEquals("B12", p.getTargetId());
        assertEquals(0, p.getObservedBookingVersion());
        assertEquals(1, p.getObservedPolicyVersion());
    }

    // Invalid proposal input is rejected
    @Test
    void proposalRejectsNullId() {
        assertThrows(IllegalArgumentException.class, () -> new Proposal(null, "B1", "B12", 0, 1));
    }

    @Test
    void proposalRejectsLowercaseTarget() {
        assertThrows(IllegalArgumentException.class, () -> new Proposal("P1", "B1", "b12", 0, 1));
    }

    @Test
    void proposalRejectsNegativeBookingVersion() {
        assertThrows(IllegalArgumentException.class, () -> new Proposal("P1", "B1", "B12", -1, 1));
    }

    @Test
    void proposalRejectsPolicyVersionZero() {
        assertThrows(IllegalArgumentException.class, () -> new Proposal("P1", "B1", "B12", 0, 0));
    }

    // The view carries the same values, with the status as a String
    @Test
    void proposalViewMatchesProposal() {
        ProposalView v = new Proposal("P1", "B1", "B12", 0, 1).toView();
        assertEquals("P1", v.proposalId());
        assertEquals("B1", v.bookingId());
        assertEquals("B12", v.targetId());
        assertEquals(0, v.bookingVersion());
        assertEquals(1, v.policyVersion());
        assertEquals("PENDING", v.status());
    }

    // Policy v1: the target must differ from the booking's current space and be available
    private static Booking b1() {
        return new Booking("B1", "A17", 3, 0, true);
    }

    @Test
    void policyAcceptsB12EvenThoughB1NeedsAccessible() {
        assertTrue(new EligibilityPolicy().isEligible(b1(), new Space("B12", true, false, false)));
    }

    @Test
    void policyAcceptsC03() {
        assertTrue(new EligibilityPolicy().isEligible(b1(), new Space("C03", true, false, true)));
    }

    @Test
    void policyRejectsOccupiedD09() {
        assertFalse(new EligibilityPolicy().isEligible(b1(), new Space("D09", true, true, true)));
    }

    @Test
    void policyRejectsCurrentSpaceA17() {
        assertFalse(new EligibilityPolicy().isEligible(b1(), new Space("A17", false, true, false)));
    }

    @Test
    void policyRejectsCurrentSpaceEvenWhenAvailable() {
        Booking atB12 = new Booking("B1", "B12", 3, 0, true);
        assertFalse(new EligibilityPolicy().isEligible(atB12, new Space("B12", true, false, false)));
    }

    @Test
    void policyVersionIsOne() {
        assertEquals(1, new EligibilityPolicy().getVersion());
    }

    // StudentApplication starts from the supplied fixture
    @Test
    void applicationStartsFromFixture() {
        StudentApplication app = new StudentApplication();
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
        assertEquals(0, app.bookingSnapshot().get("version"));
        assertEquals(true, app.spaceSnapshot("D09").get("occupied"));
        assertEquals(false, app.spaceSnapshot("B12").get("occupied"));
        assertTrue(app.proposals().isEmpty());
    }

    // D1-C: B12 gives a pending proposal; the booking does not move
    @Test
    void proposeB12CreatesPendingProposalAndBookingStaysAtA17() {
        StudentApplication app = new StudentApplication();
        Map<String, Object> bookingBefore = app.bookingSnapshot();

        ProposalView p = app.propose("B12");

        assertEquals("PENDING", p.status());
        assertEquals("B1", p.bookingId());
        assertEquals("B12", p.targetId());
        assertEquals(0, p.bookingVersion());
        assertEquals(1, p.policyVersion());
        assertTrue(p.proposalId().matches("[A-Z][A-Z0-9_-]{0,15}"));
        assertEquals(List.of(p), app.proposals());
        assertEquals(bookingBefore, app.bookingSnapshot());
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
        assertEquals(0, app.bookingSnapshot().get("version"));
        assertEquals(false, app.spaceSnapshot("B12").get("occupied"));
        assertEquals(true, app.spaceSnapshot("A17").get("occupied"));
    }

    // D1-D: occupied D09 is rejected; booking, occupancy and proposals unchanged
    @Test
    void proposeD09IsRejectedAndNothingChanges() {
        StudentApplication app = new StudentApplication();
        Map<String, Object> bookingBefore = app.bookingSnapshot();
        Map<String, Object> d09Before = app.spaceSnapshot("D09");

        assertThrows(IllegalArgumentException.class, () -> app.propose("D09"));

        assertEquals(bookingBefore, app.bookingSnapshot());
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
        assertEquals(0, app.bookingSnapshot().get("version"));
        assertEquals(d09Before, app.spaceSnapshot("D09"));
        assertEquals(true, app.spaceSnapshot("D09").get("occupied"));
        assertTrue(app.proposals().isEmpty());
    }

    // D1-E: an unknown target is a controlled error; nothing is created or changed
    @Test
    void proposeUnknownTargetIsRejectedAndNothingChanges() {
        StudentApplication app = new StudentApplication();
        Map<String, Object> bookingBefore = app.bookingSnapshot();

        assertThrows(IllegalArgumentException.class, () -> app.propose("Z99"));

        assertEquals(bookingBefore, app.bookingSnapshot());
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
        assertEquals(0, app.bookingSnapshot().get("version"));
        assertTrue(app.proposals().isEmpty());
    }

    @Test
    void proposeCurrentSpaceA17IsRejectedAndNothingChanges() {
        StudentApplication app = new StudentApplication();
        Map<String, Object> bookingBefore = app.bookingSnapshot();

        assertThrows(IllegalArgumentException.class, () -> app.propose("A17"));

        assertEquals(bookingBefore, app.bookingSnapshot());
        assertTrue(app.proposals().isEmpty());
    }

    @Test
    void proposeMalformedTargetIsRejectedAndNothingChanges() {
        StudentApplication app = new StudentApplication();
        Map<String, Object> bookingBefore = app.bookingSnapshot();

        assertThrows(IllegalArgumentException.class, () -> app.propose(null));
        assertThrows(IllegalArgumentException.class, () -> app.propose("b12"));

        assertEquals(bookingBefore, app.bookingSnapshot());
        assertTrue(app.proposals().isEmpty());
    }

    @Test
    void eachProposalGetsItsOwnApplicationGeneratedId() {
        StudentApplication app = new StudentApplication();
        ProposalView first = app.propose("B12");
        ProposalView second = app.propose("C03");
        assertNotEquals(first.proposalId(), second.proposalId());
        assertEquals(List.of(first, second), app.proposals());
    }

    // D1-F: snapshots and lists handed out cannot change protected state
    @Test
    void bookingSnapshotCannotChangeTheBooking() {
        StudentApplication app = new StudentApplication();
        Map<String, Object> snapshot = app.bookingSnapshot();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put("spaceId", "D09"));
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
    }

    @Test
    void proposalListCannotBeModifiedByCallers() {
        StudentApplication app = new StudentApplication();
        app.propose("B12");
        List<ProposalView> list = app.proposals();
        assertThrows(UnsupportedOperationException.class, () -> list.clear());
        assertEquals(1, app.proposals().size());
    }

    @Test
    void spaceSnapshotRejectsUnknownSpace() {
        StudentApplication app = new StudentApplication();
        assertThrows(IllegalArgumentException.class, () -> app.spaceSnapshot("Z99"));
    }

    // Separate StudentApplication objects do not share state
    @Test
    void applicationsAreIndependent() {
        StudentApplication first = new StudentApplication();
        StudentApplication second = new StudentApplication();
        first.propose("B12");
        assertEquals(1, first.proposals().size());
        assertTrue(second.proposals().isEmpty());
    }

    @Test
    void approvePendingStatusRequest(){
        Proposal request = new Proposal("D12","B5", "A10", 1, 1);
        request.approve();
        assertEquals(ProposalStatus.APPROVED, request.getStatus());
    }

    @Test 
    void cannotApproveNonPendingRequest(){
        Proposal request = new Proposal("D12","B5", "A10", 1, 1);
        request.approve();
        assertThrows(IllegalStateException.class, () -> request.approve());

        // ensure the status does not change, state should not change after rejection
        assertEquals(ProposalStatus.APPROVED, request.getStatus());
    }

    @Test
    void rejectPendingStatusRequest(){
        Proposal request = new Proposal("D12","B5", "A10", 1, 1);
        request.reject();
        assertEquals(ProposalStatus.REJECTED, request.getStatus());
    }

    @Test 
    void cannotRejectNonPendingRequest(){
        Proposal request = new Proposal("D12","B5", "A10", 1, 1);
        request.reject();
        assertThrows(IllegalStateException.class, () -> request.reject());

        // ensure the status does not change, state should not change after rejection
        assertEquals(ProposalStatus.REJECTED, request.getStatus());
    }

    @Test
    void executeApprovedStatusRequest(){
        Proposal request = new Proposal("D12","B5", "A10", 1, 1);
        request.approve();
        request.markExecuted();
        assertEquals(ProposalStatus.EXECUTED, request.getStatus());
    }

    @Test
    void cannotExecutePendingRequest(){
        Proposal request = new Proposal("D12","B5", "A10", 1, 1);
        assertThrows(IllegalStateException.class, () -> request.markExecuted());
        assertEquals(ProposalStatus.PENDING, request.getStatus());
    }

    @Test
    void cannotExecuteRejectedRequest(){
        Proposal request = new Proposal("D12","B5", "A10", 1, 1);
        request.reject();
        assertThrows(IllegalStateException.class, () -> request.markExecuted());
        assertEquals(ProposalStatus.REJECTED, request.getStatus());
    }

    @Test 
    void cannotRejectApprovedRequest(){
        Proposal request = new Proposal("D12","B5", "A10", 1, 1);
        request.approve();
        assertThrows(IllegalStateException.class, () -> request.reject());
        assertEquals(ProposalStatus.APPROVED, request.getStatus());
    }

    @Test 
    void cannotApproveRejectedRequest(){
        Proposal request = new Proposal("D12","B5", "A10", 1, 1);
        request.reject();
        assertThrows(IllegalStateException.class, () -> request.approve());
        assertEquals(ProposalStatus.REJECTED, request.getStatus());
    }

    @Test
    void cannotApproveExecutedRequest(){
        Proposal request = new Proposal("D12","B5", "A10", 1, 1);
        request.approve();
        request.markExecuted();
        assertThrows(IllegalStateException.class, () -> request.approve());
        assertEquals(ProposalStatus.EXECUTED, request.getStatus());
    }

    @Test 
    void cannotRejectExecutedRequest(){
        Proposal request = new Proposal("D12","B5", "A10", 1, 1);
        request.approve();
        request.markExecuted();
        assertThrows(IllegalStateException.class, () -> request.reject());
        assertEquals(ProposalStatus.EXECUTED, request.getStatus());
    }
    @Test 
    void cannotMarkExecutedAnExecutedRequest(){
        Proposal request = new Proposal("D12","B5", "A10", 1, 1);
        request.approve();
        request.markExecuted();
        assertThrows(IllegalStateException.class, () -> request.markExecuted());
        assertEquals(ProposalStatus.EXECUTED, request.getStatus());
    }

    @Test 
    void validMovedBooking(){
        Booking booking = new Booking("D12", "C5", 5, 1, true);
        Booking newBooking = booking.movedTo("C14"); 
        assertEquals("C14", newBooking.getSpaceId());
        assertEquals(booking.getVersion() + 1, newBooking.getVersion());
    }

    @Test 
    void invalidMovedBookingSpaceId(){
        Booking booking = new Booking("D12", "C5", 5, 1, true);

        assertThrows(IllegalArgumentException.class, () -> booking.movedTo("d18")); 
    }

    @Test 
    void occupyTakesUpFreeSpace(){
        Space b12 = new Space("B12", true, false, false);
        b12.occupy();
        assertTrue(b12.isOccupied());
        assertFalse(b12.isAvailable());

    }

    @Test
    void cannotOccupyATakenSpace(){
        Space b12 = new Space("B12", true, true, false);
        assertThrows(IllegalStateException.class, () -> b12.occupy());
        assertTrue(b12.isOccupied());
        assertTrue(b12.isOpen());
    }

    @Test 
    void releaseFreesSpace(){
        Space b12 = new Space("B12", false, true, false);
        b12.release();
        assertFalse(b12.isOccupied());
        assertFalse(b12.isAvailable());
    }

    @Test 
    void closedSpaceNotAvailable(){
        Space b12 = new Space("B12", true, true, false);
        b12.close();
        assertFalse(b12.isOpen());
        assertFalse(b12.isAvailable());
        assertTrue(b12.isOccupied()); // closing does not change any other attributes
    }

    // D2.1: strict tool-request parsing. The model's reply is untrusted text.
    private static final Set<String> SPACES = Set.of("A17", "B12", "C03", "D09");

    @Test
    void parserAcceptsReadBooking(){
        ToolRequestParser.Result result = ToolRequestParser.parse(
                "{\"tool\":\"READ_BOOKING\",\"bookingId\":\"B1\",\"targetId\":\"\"}", "B1", SPACES);
        assertTrue(result.accepted());
        assertEquals(new ToolRequest(Tool.READ_BOOKING, "B1", ""), result.request());
    }

    private static ToolRequestParser.Result parse(String json){
        return ToolRequestParser.parse(json, "B1", SPACES);
    }

    // D2-H: malformed JSON is rejected, not thrown
    @Test
    void parserRejectsMalformedJson(){
        ToolRequestParser.Result result = parse("{\"tool\":\"READ_BOOKING\",");
        assertFalse(result.accepted());
        assertNull(result.request());
        assertTrue(result.rejection().contains("JSON"));
    }

    @Test
    void parserRejectsJsonThatIsNotAnObject(){
        assertFalse(parse("[\"READ_BOOKING\"]").accepted());
        assertFalse(parse("\"READ_BOOKING\"").accepted());
    }

    // D2-H: an extra "approved" field grants no authority; the whole request is rejected
    @Test
    void parserRejectsExtraApprovedField(){
        ToolRequestParser.Result result = parse(
                "{\"tool\":\"PROPOSE_REASSIGNMENT\",\"bookingId\":\"B1\",\"targetId\":\"B12\",\"approved\":true}");
        assertFalse(result.accepted());
        assertTrue(result.rejection().contains("approved"));
    }

    @Test
    void parserRejectsMissingProperty(){
        ToolRequestParser.Result result = parse("{\"tool\":\"READ_BOOKING\",\"bookingId\":\"B1\"}");
        assertFalse(result.accepted());
        assertTrue(result.rejection().contains("targetId"));
    }

    @Test
    void parserRejectsNonStringValue(){
        ToolRequestParser.Result result = parse("{\"tool\":\"READ_BOOKING\",\"bookingId\":1,\"targetId\":\"\"}");
        assertFalse(result.accepted());
        assertTrue(result.rejection().contains("bookingId"));
    }

    // D2-I: an unknown tool name maps to nothing that can run
    @Test
    void parserRejectsUnknownTool(){
        assertFalse(parse("{\"tool\":\"DELETE_BOOKING\",\"bookingId\":\"B1\",\"targetId\":\"\"}").accepted());
        assertFalse(parse("{\"tool\":\"read_booking\",\"bookingId\":\"B1\",\"targetId\":\"\"}").accepted());
    }

    // D2-I: a booking other than the goal booking is rejected
    @Test
    void parserRejectsWrongGoalBooking(){
        ToolRequestParser.Result result = parse("{\"tool\":\"READ_BOOKING\",\"bookingId\":\"B2\",\"targetId\":\"\"}");
        assertFalse(result.accepted());
        assertTrue(result.rejection().contains("B1"));
    }

    @Test
    void parserRejectsTargetOnNonProposalTool(){
        assertFalse(parse("{\"tool\":\"READ_BOOKING\",\"bookingId\":\"B1\",\"targetId\":\"B12\"}").accepted());
        assertFalse(parse("{\"tool\":\"FINISH\",\"bookingId\":\"B1\",\"targetId\":\"C03\"}").accepted());
    }

    @Test
    void parserRejectsProposalWithUnknownOrEmptyTarget(){
        assertFalse(parse("{\"tool\":\"PROPOSE_REASSIGNMENT\",\"bookingId\":\"B1\",\"targetId\":\"Z99\"}").accepted());
        assertFalse(parse("{\"tool\":\"PROPOSE_REASSIGNMENT\",\"bookingId\":\"B1\",\"targetId\":\"\"}").accepted());
        assertFalse(parse("{\"tool\":\"PROPOSE_REASSIGNMENT\",\"bookingId\":\"B1\",\"targetId\":\"b12\"}").accepted());
    }

    // Valid shapes of the other three tools
    @Test
    void parserAcceptsValidListProposeAndFinish(){
        assertEquals(new ToolRequest(Tool.LIST_ALTERNATIVES, "B1", ""),
                parse("{\"tool\":\"LIST_ALTERNATIVES\",\"bookingId\":\"B1\",\"targetId\":\"\"}").request());
        assertEquals(new ToolRequest(Tool.PROPOSE_REASSIGNMENT, "B1", "C03"),
                parse("{\"tool\":\"PROPOSE_REASSIGNMENT\",\"bookingId\":\"B1\",\"targetId\":\"C03\"}").request());
        assertEquals(new ToolRequest(Tool.FINISH, "B1", ""),
                parse("{\"tool\":\"FINISH\",\"bookingId\":\"B1\",\"targetId\":\"\"}").request());
    }

    // D2.1: the bounded model/tool loop, tested through StudentApplication.run

    // D2-A: replay read, list, propose B12 -> one pending proposal, booking not moved
    @Test
    void d2A_replayStopsAtPendingProposalWithoutMovingBooking(){
        StudentApplication app = new StudentApplication();
        AgentResult result = app.run(Fixture.replay(), 6);

        assertEquals(AgentOutcome.AWAITING_APPROVAL, result.outcome());
        assertEquals(3, result.calls());
        assertEquals(0, result.invalidResponses());

        assertEquals(1, app.proposals().size());
        ProposalView proposal = app.proposals().get(0);
        assertEquals(result.proposalId(), proposal.proposalId());
        assertEquals("PENDING", proposal.status());
        assertEquals("B1", proposal.bookingId());
        assertEquals("B12", proposal.targetId());
        assertEquals(0, proposal.bookingVersion());
        assertEquals(1, proposal.policyVersion());

        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
        assertEquals(0, app.bookingSnapshot().get("version"));
        assertEquals(false, app.spaceSnapshot("B12").get("occupied"));
        assertEquals(true, app.spaceSnapshot("A17").get("occupied"));
    }

    private static String request(String tool, String bookingId, String targetId){
        return "{\"tool\":\"" + tool + "\",\"bookingId\":\"" + bookingId + "\",\"targetId\":\"" + targetId + "\"}";
    }

    // Each model call sees the observations from the calls before it
    @Test
    void laterModelCallsSeeEarlierToolResults(){
        StudentApplication app = new StudentApplication();
        ReplayProvider replay = Fixture.replay();
        app.run(replay, 6);

        List<List<ChatMessage>> histories = replay.histories();
        assertEquals(3, histories.size());
        String afterRead = histories.get(1).get(histories.get(1).size() - 1).content();
        assertTrue(afterRead.contains("READ_BOOKING") && afterRead.contains("A17"));
        String afterList = histories.get(2).get(histories.get(2).size() - 1).content();
        assertTrue(afterList.contains("[\"B12\",\"C03\"]"));
    }

    // C03 is also eligible under policy 1; the model need not pick B12
    @Test
    void proposingC03IsAlsoAccepted(){
        StudentApplication app = new StudentApplication();
        AgentResult result = app.run(new ReplayProvider(List.of(
                request("READ_BOOKING", "B1", ""), request("LIST_ALTERNATIVES", "B1", ""),
                request("PROPOSE_REASSIGNMENT", "B1", "C03"))), 6);
        assertEquals(AgentOutcome.AWAITING_APPROVAL, result.outcome());
        assertEquals("C03", app.proposals().get(0).targetId());
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
    }

    // D2-H: malformed JSON and an extra approved field are rejected, counted, and grant nothing
    @Test
    void d2H_malformedAndApprovedFieldAreCountedAndGrantNothing(){
        StudentApplication app = new StudentApplication();
        AgentResult result = app.run(new ReplayProvider(List.of(
                "{\"tool\":\"READ_BOOKING\",",
                "{\"tool\":\"PROPOSE_REASSIGNMENT\",\"bookingId\":\"B1\",\"targetId\":\"B12\",\"approved\":true}",
                request("READ_BOOKING", "B1", ""), request("LIST_ALTERNATIVES", "B1", ""),
                request("PROPOSE_REASSIGNMENT", "B1", "B12"))), 6);

        assertEquals(AgentOutcome.AWAITING_APPROVAL, result.outcome());
        assertEquals(5, result.calls());
        assertEquals(2, result.invalidResponses());
        // only the valid proposal exists, and it is still waiting for the operator
        assertEquals(1, app.proposals().size());
        assertEquals("PENDING", app.proposals().get(0).status());
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
        assertEquals(0, app.bookingSnapshot().get("version"));
        // the model was told why each reply was rejected
        assertTrue(result.transcript().get(3).content().contains("REJECTED"));
        assertTrue(result.transcript().get(5).content().contains("approved"));
    }

    // D2-I: unknown tool and wrong goal booking are rejected; nothing runs. FINISH changes nothing.
    @Test
    void d2I_unknownToolAndWrongBookingRunNothing(){
        StudentApplication app = new StudentApplication();
        AgentResult result = app.run(new ReplayProvider(List.of(
                request("DELETE_BOOKING", "B1", ""),
                request("READ_BOOKING", "B2", ""),
                request("FINISH", "B1", ""))), 6);

        assertEquals(AgentOutcome.NO_ACTION, result.outcome());
        assertEquals(3, result.calls());
        assertEquals(2, result.invalidResponses());
        assertNull(result.proposalId());
        assertTrue(app.proposals().isEmpty());
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
        assertEquals(0, app.bookingSnapshot().get("version"));
    }

    // D2-J: six invalid responses -> STEP_LIMIT after exactly six calls, no seventh call
    @Test
    void d2J_sixInvalidResponsesStopAtStepLimit(){
        StudentApplication app = new StudentApplication();
        List<String> replies = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
            replies.add("not json " + i);
        }
        replies.add(request("READ_BOOKING", "B1", ""));   // must never be requested
        ReplayProvider replay = new ReplayProvider(replies);

        AgentResult result = app.run(replay, 6);

        assertEquals(AgentOutcome.STEP_LIMIT, result.outcome());
        assertEquals(6, result.calls());
        assertEquals(6, replay.calls());
        assertEquals(6, result.invalidResponses());
        assertTrue(app.proposals().isEmpty());
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
    }

    // The call limit must be 1..6
    @Test
    void runRejectsLimitOutsideOneToSix(){
        StudentApplication app = new StudentApplication();
        assertThrows(IllegalArgumentException.class, () -> app.run(Fixture.replay(), 0));
        assertThrows(IllegalArgumentException.class, () -> app.run(Fixture.replay(), 7));
        assertTrue(app.proposals().isEmpty());
    }

    // D2-K: a provider that throws or times out -> PROVIDER_ERROR, nothing committed or proposed
    @Test
    void d2K_providerFailureEndsWithProviderError(){
        StudentApplication app = new StudentApplication();
        AgentResult thrown = app.run(history -> { throw new java.io.IOException("connection refused"); }, 6);
        AgentResult timedOut = app.run(history -> { throw new java.net.http.HttpTimeoutException("timed out"); }, 6);

        assertEquals(AgentOutcome.PROVIDER_ERROR, thrown.outcome());
        assertEquals(AgentOutcome.PROVIDER_ERROR, timedOut.outcome());
        assertEquals(1, timedOut.calls());
        assertTrue(app.proposals().isEmpty());
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
        assertEquals(0, app.bookingSnapshot().get("version"));
    }

    // A failure after some successful calls still proposes nothing
    @Test
    void providerFailureMidRunProposesNothing(){
        StudentApplication app = new StudentApplication();
        // the replay runs out after two replies and then throws
        AgentResult result = app.run(new ReplayProvider(List.of(
                request("READ_BOOKING", "B1", ""), request("LIST_ALTERNATIVES", "B1", ""))), 6);
        assertEquals(AgentOutcome.PROVIDER_ERROR, result.outcome());
        assertEquals(3, result.calls());
        assertTrue(app.proposals().isEmpty());
    }

    // An interrupted provider ends the run and keeps the thread's interrupt flag set
    @Test
    void interruptedProviderKeepsInterruptFlag(){
        StudentApplication app = new StudentApplication();
        AgentResult result = app.run(history -> { throw new InterruptedException(); }, 6);
        assertEquals(AgentOutcome.PROVIDER_ERROR, result.outcome());
        assertTrue(Thread.interrupted());   // also clears the flag for later tests
    }

    // A null reply is an invalid response, not a crash
    @Test
    void nullReplyCountsAsInvalid(){
        StudentApplication app = new StudentApplication();
        AgentResult result = app.run(history -> null, 2);
        assertEquals(AgentOutcome.STEP_LIMIT, result.outcome());
        assertEquals(2, result.invalidResponses());
        assertTrue(app.proposals().isEmpty());
    }

    // Out-of-order tools get an error observation; they use a call but are not invalid replies
    @Test
    void outOfOrderToolsAreToolErrorsNotInvalidReplies(){
        StudentApplication app = new StudentApplication();
        AgentResult result = app.run(new ReplayProvider(List.of(
                request("LIST_ALTERNATIVES", "B1", ""),           // before any read
                request("READ_BOOKING", "B1", ""),
                request("PROPOSE_REASSIGNMENT", "B1", "B12"),     // before list
                request("LIST_ALTERNATIVES", "B1", ""),
                request("PROPOSE_REASSIGNMENT", "B1", "B12"))), 6);

        assertEquals(AgentOutcome.AWAITING_APPROVAL, result.outcome());
        assertEquals(5, result.calls());
        assertEquals(0, result.invalidResponses());
        assertEquals(1, app.proposals().size());
        assertTrue(result.transcript().get(3).content().contains("TOOL_ERROR"));
        assertTrue(result.transcript().get(7).content().contains("TOOL_ERROR"));
    }

    // Proposing an ineligible target (occupied D09) is a tool error; nothing is stored or changed
    @Test
    void proposingIneligibleTargetIsToolErrorAndChangesNothing(){
        StudentApplication app = new StudentApplication();
        AgentResult result = app.run(new ReplayProvider(List.of(
                request("READ_BOOKING", "B1", ""), request("LIST_ALTERNATIVES", "B1", ""),
                request("PROPOSE_REASSIGNMENT", "B1", "D09"),
                request("FINISH", "B1", ""))), 6);

        assertEquals(AgentOutcome.NO_ACTION, result.outcome());
        assertEquals(0, result.invalidResponses());
        assertTrue(result.transcript().get(7).content().contains("TOOL_ERROR"));
        assertTrue(app.proposals().isEmpty());
        assertEquals(true, app.spaceSnapshot("D09").get("occupied"));
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
        assertEquals(0, app.bookingSnapshot().get("version"));
    }

    // D2.2 (2.2.7): withDuration returns a changed copy with the version raised by one
    @Test
    void withDurationReturnsCopyWithNewDurationAndVersion(){
        Booking booking = new Booking("B1", "A17", 3, 0, true);
        Booking changed = booking.withDuration(4);
        assertEquals(4, changed.getDurationHours());
        assertEquals(1, changed.getVersion());
        assertEquals("A17", changed.getSpaceId());
        assertEquals("B1", changed.getId());
        assertTrue(changed.requiresAccessible());
        // the original is unchanged
        assertEquals(3, booking.getDurationHours());
        assertEquals(0, booking.getVersion());
    }

    @Test
    void withDurationKeepsTheOneToTwentyFourRule(){
        Booking booking = new Booking("B1", "A17", 3, 0, true);
        assertEquals(1, booking.withDuration(1).getDurationHours());
        assertEquals(24, booking.withDuration(24).getDurationHours());
        assertThrows(IllegalArgumentException.class, () -> booking.withDuration(0));
        assertThrows(IllegalArgumentException.class, () -> booking.withDuration(25));
    }

    // D2.2 (2.2.11): everything that must stay the same after a refused action, as one value
    private static Map<String, Object> protectedState(StudentApplication app){
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("booking", app.bookingSnapshot());
        for (String id : List.of("A17", "B12", "C03", "D09")) {
            state.put(id, app.spaceSnapshot(id));
        }
        state.put("statuses", app.proposals().stream().map(ProposalView::status).toList());
        state.put("commits", app.commitCount());
        state.put("notifications", app.notifications().size());
        return state;
    }

    @Test
    void newApplicationHasNoCommitsOrNotifications(){
        StudentApplication app = new StudentApplication();
        assertEquals(0, app.commitCount());
        assertTrue(app.notifications().isEmpty());
    }

    // D2.2 (2.2.12): operator approve and reject
    @Test
    void operatorApprovesAndRejectsPendingProposals(){
        StudentApplication app = new StudentApplication();
        String first = app.propose("B12").proposalId();
        String second = app.propose("C03").proposalId();
        assertEquals("APPROVED", app.approve(first).status());
        assertEquals("REJECTED", app.reject(second).status());
        assertEquals(List.of("APPROVED", "REJECTED"), app.proposals().stream().map(ProposalView::status).toList());
    }

    @Test
    void operatorActionOnUnknownProposalIsRefusedAndChangesNothing(){
        StudentApplication app = new StudentApplication();
        app.propose("B12");
        Map<String, Object> before = protectedState(app);
        assertThrows(IllegalArgumentException.class, () -> app.approve("P99"));
        assertThrows(IllegalArgumentException.class, () -> app.reject("P99"));
        assertEquals(before, protectedState(app));
    }

    @Test
    void operatorCannotApproveTwiceOrRejectAnApprovedProposal(){
        StudentApplication app = new StudentApplication();
        String id = app.propose("B12").proposalId();
        app.approve(id);
        Map<String, Object> before = protectedState(app);
        assertThrows(IllegalStateException.class, () -> app.approve(id));
        assertThrows(IllegalStateException.class, () -> app.reject(id));
        assertEquals(before, protectedState(app));
    }

    // D2.2 (2.2.13): setup actions used to change state after a proposal
    @Test
    void changeBookingDurationRaisesVersionWithoutMoving(){
        StudentApplication app = new StudentApplication();
        app.changeBookingDuration(4);
        assertEquals(4, app.bookingSnapshot().get("durationHours"));
        assertEquals(1, app.bookingSnapshot().get("version"));
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
    }

    @Test
    void changeBookingDurationRejectsInvalidHoursAndChangesNothing(){
        StudentApplication app = new StudentApplication();
        Map<String, Object> before = protectedState(app);
        assertThrows(IllegalArgumentException.class, () -> app.changeBookingDuration(25));
        assertEquals(before, protectedState(app));
    }

    @Test
    void closeAndOccupySpaceChangeOnlyThatSpace(){
        StudentApplication app = new StudentApplication();
        app.closeSpace("B12");
        app.occupySpace("C03");
        assertEquals(false, app.spaceSnapshot("B12").get("open"));
        assertEquals(true, app.spaceSnapshot("C03").get("occupied"));
        assertEquals(List.of(), app.alternatives());
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
    }

    @Test
    void setupActionsRefuseUnknownOrTakenSpaces(){
        StudentApplication app = new StudentApplication();
        Map<String, Object> before = protectedState(app);
        assertThrows(IllegalArgumentException.class, () -> app.closeSpace("Z99"));
        assertThrows(IllegalArgumentException.class, () -> app.occupySpace("Z99"));
        assertThrows(IllegalStateException.class, () -> app.occupySpace("D09"));
        assertEquals(before, protectedState(app));
    }

    // D2-B: executing a pending proposal -> NOT_APPROVED, all protected state unchanged
    @Test
    void d2B_executingPendingProposalIsNotApprovedAndChangesNothing(){
        StudentApplication app = new StudentApplication();
        String id = app.propose("B12").proposalId();
        Map<String, Object> before = protectedState(app);

        assertEquals(ExecutionOutcome.NOT_APPROVED, app.execute(id));
        assertEquals(before, protectedState(app));
        assertEquals("PENDING", app.proposals().get(0).status());
    }

    // D2-C: approve then execute -> one COMMITTED, B1 at B12 / version 1, one notification
    @Test
    void d2C_approvedProposalCommitsOnce(){
        StudentApplication app = new StudentApplication();
        String id = app.propose("B12").proposalId();
        app.approve(id);

        assertEquals(ExecutionOutcome.COMMITTED, app.execute(id));
        assertEquals("B12", app.bookingSnapshot().get("spaceId"));
        assertEquals(1, app.bookingSnapshot().get("version"));
        assertEquals(false, app.spaceSnapshot("A17").get("occupied"));
        assertEquals(true, app.spaceSnapshot("B12").get("occupied"));
        assertEquals(false, app.spaceSnapshot("C03").get("occupied"));
        assertEquals("EXECUTED", app.proposals().get(0).status());
        assertEquals(1, app.commitCount());
        assertEquals(1, app.notifications().size());
        assertEquals("B1", app.notifications().get(0).bookingId());
    }

    @Test
    void executingUnknownProposalIsRefusedAndChangesNothing(){
        StudentApplication app = new StudentApplication();
        Map<String, Object> before = protectedState(app);
        assertThrows(IllegalArgumentException.class, () -> app.execute("P99"));
        assertEquals(before, protectedState(app));
    }

    // D2-D: executing the same committed proposal again -> DUPLICATE, counts unchanged
    @Test
    void d2D_repeatedExecutionIsDuplicateWithNoSecondCommit(){
        StudentApplication app = new StudentApplication();
        String id = app.propose("B12").proposalId();
        app.approve(id);
        app.execute(id);
        Map<String, Object> afterCommit = protectedState(app);

        assertEquals(ExecutionOutcome.DUPLICATE, app.execute(id));
        assertEquals(afterCommit, protectedState(app));
        assertEquals(1, app.commitCount());
        assertEquals(1, app.notifications().size());
        assertEquals(1, app.bookingSnapshot().get("version"));
    }

    // D2-E: reject a pending proposal, then execute -> no commit, booking unchanged
    @Test
    void d2E_rejectedProposalNeverExecutes(){
        StudentApplication app = new StudentApplication();
        String id = app.propose("B12").proposalId();
        app.reject(id);
        Map<String, Object> before = protectedState(app);

        assertEquals(ExecutionOutcome.NOT_APPROVED, app.execute(id));
        assertEquals(before, protectedState(app));
        assertEquals(0, app.commitCount());
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
        assertEquals("REJECTED", app.proposals().get(0).status());
    }

    // D2-F: booking version changes after the proposal -> STALE, no move
    @Test
    void d2F_changedBookingVersionMakesProposalStale(){
        StudentApplication app = new StudentApplication();
        String id = app.propose("B12").proposalId();
        app.approve(id);
        app.changeBookingDuration(4);   // B1 is now version 1; the proposal recorded version 0
        Map<String, Object> before = protectedState(app);

        assertEquals(ExecutionOutcome.STALE, app.execute(id));
        assertEquals(before, protectedState(app));
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
        assertEquals(false, app.spaceSnapshot("B12").get("occupied"));
        assertEquals("APPROVED", app.proposals().get(0).status());
    }

    // D2-G: target occupied after the proposal -> UNAVAILABLE_OR_INELIGIBLE, no move
    @Test
    void d2G_occupiedTargetIsUnavailable(){
        StudentApplication app = new StudentApplication();
        String id = app.propose("B12").proposalId();
        app.approve(id);
        app.occupySpace("B12");
        Map<String, Object> before = protectedState(app);

        assertEquals(ExecutionOutcome.UNAVAILABLE_OR_INELIGIBLE, app.execute(id));
        assertEquals(before, protectedState(app));
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
        assertEquals(0, app.bookingSnapshot().get("version"));
    }

    // D2-G: target closed after the proposal -> UNAVAILABLE_OR_INELIGIBLE, no move
    @Test
    void d2G_closedTargetIsUnavailable(){
        StudentApplication app = new StudentApplication();
        String id = app.propose("B12").proposalId();
        app.approve(id);
        app.closeSpace("B12");
        Map<String, Object> before = protectedState(app);

        assertEquals(ExecutionOutcome.UNAVAILABLE_OR_INELIGIBLE, app.execute(id));
        assertEquals(before, protectedState(app));
        assertEquals(0, app.commitCount());
        assertEquals(false, app.spaceSnapshot("B12").get("occupied"));
    }

    // Two approved proposals for the same booking version: only the first can commit
    @Test
    void secondApprovedProposalIsStaleAfterFirstCommits(){
        StudentApplication app = new StudentApplication();
        String first = app.propose("B12").proposalId();
        String second = app.propose("C03").proposalId();
        app.approve(first);
        app.approve(second);

        assertEquals(ExecutionOutcome.COMMITTED, app.execute(first));
        Map<String, Object> afterCommit = protectedState(app);
        assertEquals(ExecutionOutcome.STALE, app.execute(second));
        assertEquals(afterCommit, protectedState(app));
        assertEquals(1, app.commitCount());
        assertEquals(false, app.spaceSnapshot("C03").get("occupied"));
    }

    // D2.1 (2.1.10): versions that change after the read make list/propose stale; the model must read again
    @Test
    void staleVersionsAfterReadForceANewRead(){
        StudentApplication app = new StudentApplication();
        List<String> replies = List.of(
                request("READ_BOOKING", "B1", ""),
                request("LIST_ALTERNATIVES", "B1", ""),      // B1 changes just before this call
                request("READ_BOOKING", "B1", ""),
                request("LIST_ALTERNATIVES", "B1", ""),
                request("PROPOSE_REASSIGNMENT", "B1", "B12"));
        int[] call = {0};
        AgentResult result = app.run(history -> {
            int n = call[0]++;
            if (n == 1) {
                app.changeBookingDuration(4);   // B1 version 0 -> 1 between read and list
            }
            return replies.get(n);
        }, 6);

        assertEquals(AgentOutcome.AWAITING_APPROVAL, result.outcome());
        assertEquals(0, result.invalidResponses());
        assertTrue(result.transcript().get(5).content().contains("TOOL_ERROR"));
        assertTrue(result.transcript().get(5).content().contains("READ_BOOKING"));
        assertEquals(1, app.proposals().size());
        assertEquals(1, app.proposals().get(0).bookingVersion());
    }

    // D2.3: periodic detection with controllable time, no real waiting

    // D2-L: ticks 0, 59, 60 with unchanged versions -> at most one work item
    @Test
    void d2L_boundaryTicksEmitAtMostOneWorkItem(){
        StudentApplication app = new StudentApplication();
        List<WorkItem> atZero = app.tick(0);
        List<WorkItem> atFiftyNine = app.tick(59);
        List<WorkItem> atSixty = app.tick(60);

        assertEquals(List.of(new WorkItem("B1", 0, 1, 0)), atZero);
        assertTrue(atFiftyNine.isEmpty());   // before the 60-second boundary: no scan
        assertTrue(atSixty.isEmpty());       // boundary scan, but this booking/policy pair was already emitted
        assertTrue(app.proposals().isEmpty());   // ticks never propose or move anything
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
    }

    // D2-M: a tick below the previous time is a clear clock rejection
    @Test
    void d2M_backwardsClockIsRejected(){
        StudentApplication app = new StudentApplication();
        app.tick(60);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> app.tick(30));
        assertTrue(ex.getMessage().contains("backwards"));
        // the rejected tick changed nothing: the next boundary is still 60 + 60
        assertTrue(app.tick(119).isEmpty());
        assertTrue(app.tick(120).isEmpty());
    }

    // An active (pending) proposal for the same booking/policy versions suppresses work
    @Test
    void pendingProposalSuppressesWork(){
        StudentApplication app = new StudentApplication();
        app.propose("B12");
        assertTrue(app.tick(0).isEmpty());
        assertTrue(app.tick(60).isEmpty());
        assertEquals(1, app.proposals().size());
    }

    // After a commit, B1 is on open B12 and no longer affected
    @Test
    void committedBookingIsNoLongerAffected(){
        StudentApplication app = new StudentApplication();
        String id = app.propose("B12").proposalId();
        app.approve(id);
        app.execute(id);
        assertTrue(app.tick(0).isEmpty());
    }

    // Conservative retry: failed work is not re-emitted until the operator retries or a version changes
    @Test
    void retryOrVersionChangeAllowsWorkAgain(){
        StudentApplication app = new StudentApplication();
        assertEquals(1, app.tick(0).size());
        assertTrue(app.tick(60).isEmpty());          // e.g. the agent run failed: nothing new on its own

        app.retry("B1");
        assertEquals(List.of(new WorkItem("B1", 0, 1, 120)), app.tick(120));

        app.changeBookingDuration(4);                 // version 0 -> 1 is a new pair
        assertEquals(List.of(new WorkItem("B1", 1, 1, 180)), app.tick(180));
    }

    // A rejected proposal is not active, so it does not suppress work for a new scan after retry
    @Test
    void rejectedProposalDoesNotSuppressRetriedWork(){
        StudentApplication app = new StudentApplication();
        assertEquals(1, app.tick(0).size());
        app.reject(app.propose("B12").proposalId());
        app.retry("B1");
        assertEquals(1, app.tick(60).size());
    }

    @Test
    void tickRejectsNegativeTimeAndRetryRejectsUnknownBooking(){
        StudentApplication app = new StudentApplication();
        assertThrows(IllegalArgumentException.class, () -> app.tick(-1));
        assertThrows(IllegalArgumentException.class, () -> app.retry("B9"));
        assertEquals(1, app.tick(0).size());   // the refused calls changed nothing
    }

    // D2.3: the agent does not issue another active proposal for the same booking/policy versions
    @Test
    void agentRefusesSecondActiveProposalForSameState(){
        StudentApplication app = new StudentApplication();
        app.run(Fixture.replay(), 6);
        AgentResult second = app.run(new ReplayProvider(List.of(
                request("READ_BOOKING", "B1", ""), request("LIST_ALTERNATIVES", "B1", ""),
                request("PROPOSE_REASSIGNMENT", "B1", "C03"), request("FINISH", "B1", ""))), 6);

        assertEquals(AgentOutcome.NO_ACTION, second.outcome());
        assertTrue(second.transcript().get(7).content().contains("TOOL_ERROR"));
        assertTrue(second.transcript().get(7).content().contains("P1"));
        assertEquals(1, app.proposals().size());
        assertEquals("B12", app.proposals().get(0).targetId());
    }

    // Once the earlier proposal is rejected it is no longer active, so a new one may be issued
    @Test
    void agentMayProposeAgainAfterRejection(){
        StudentApplication app = new StudentApplication();
        app.reject(app.run(Fixture.replay(), 6).proposalId());
        AgentResult again = app.run(Fixture.replay(), 6);
        assertEquals(AgentOutcome.AWAITING_APPROVAL, again.outcome());
        assertEquals(List.of("REJECTED", "PENDING"), app.proposals().stream().map(ProposalView::status).toList());
    }

    // D2.1: a version change between LIST and PROPOSE makes the proposal step stale
    @Test
    void staleVersionsBeforeProposeForceANewRead(){
        StudentApplication app = new StudentApplication();
        List<String> replies = List.of(
                request("READ_BOOKING", "B1", ""), request("LIST_ALTERNATIVES", "B1", ""),
                request("PROPOSE_REASSIGNMENT", "B1", "B12"),   // B1 changes just before this call
                request("READ_BOOKING", "B1", ""), request("LIST_ALTERNATIVES", "B1", ""),
                request("PROPOSE_REASSIGNMENT", "B1", "B12"));
        int[] call = {0};
        AgentResult result = app.run(history -> {
            int n = call[0]++;
            if (n == 2) {
                app.changeBookingDuration(4);
            }
            return replies.get(n);
        }, 6);

        assertEquals(AgentOutcome.AWAITING_APPROVAL, result.outcome());
        assertTrue(result.transcript().get(7).content().contains("TOOL_ERROR"));
        assertEquals(1, app.proposals().size());
        assertEquals(1, app.proposals().get(0).bookingVersion());   // recorded from the fresh read
    }

    // D2-L boundary: a tick before the boundary does not scan; the boundary tick does
    @Test
    void tickBeforeBoundaryDoesNotScanButBoundaryTickDoes(){
        StudentApplication app = new StudentApplication();
        assertEquals(1, app.tick(0).size());
        app.retry("B1");                       // a scan would now emit again
        assertTrue(app.tick(59).isEmpty());    // before the boundary: no scan
        assertEquals(List.of(new WorkItem("B1", 0, 1, 60)), app.tick(60));   // boundary: scans
    }

    // Boundaries stay on the 60-second grid from the first tick: 0, 60, 120, ...
    @Test
    void scanBoundariesFollowTheSixtySecondGrid(){
        StudentApplication app = new StudentApplication();
        assertEquals(1, app.tick(0).size());
        app.retry("B1");
        assertEquals(1, app.tick(61).size());   // late tick past the 60 boundary scans
        app.retry("B1");
        assertEquals(1, app.tick(120).size());  // the next boundary is 120, not 121
    }
}
