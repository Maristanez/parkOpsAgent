import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
public final class Main {
    public static void main(String[] args) throws Exception {
        String mode=args.length==0?"fixture":args[0];
        if(mode.equals("fixture")) {
            System.out.println("SYNTHETIC FIXTURE; no model invocation or booking mutation.");
            System.out.println(MiniJson.stringify(Fixture.booking()));
            System.out.println(MiniJson.stringify(Fixture.spaces()));
            return;
        }
        try {
            if(mode.equals("scripted") || mode.equals("agent-scripted")) {
                scriptedD2();
            } else if(mode.equals("scripted-d1")) {
                scriptedD1();
            } else if(mode.equals("live")) {
                live();
            } else {
                System.err.println("Modes: fixture | scripted (D2; alias agent-scripted) | scripted-d1 (D1 regression) | live (D2)");System.exit(2);
            }
        } catch(UnsupportedOperationException ex) {
            System.err.println("STARTER_NOT_IMPLEMENTED: "+ex.getMessage());System.exit(1);
        }
    }

    // D2 walkthrough: ticks, the replayed agent run, then scripted operator actions and the rejected paths
    private static void scriptedD2() {
        System.out.println("SCRIPTED D2 DEMO: replayed model responses (ReplayProvider), not a live model run.");
        System.out.println("Operator actions below are explicit scripted commands; the agent run never approves or executes.");
        StudentApplication app=new StudentApplication();

        heading("1. Periodic ticks (time is supplied; nothing waits)");
        System.out.println("tick(0) -> "+app.tick(0)+" (first tick scans: B1 is on closed A17)");
        System.out.println("tick(59) -> "+app.tick(59)+" (before the 60-second boundary: no scan)");
        System.out.println("tick(60) -> "+app.tick(60)+" (boundary scan; this booking/policy version pair was already emitted)");
        app.retry("B1");
        System.out.println("OPERATOR retry(B1): the pair may be emitted again at the next scan");
        System.out.println("tick(119) -> "+app.tick(119)+" (before the 120 boundary: no scan)");
        System.out.println("tick(120) -> "+app.tick(120)+" (boundary scan emits again after the retry)");

        heading("2. Agent run: replay read -> list -> propose B12 (limit 6)");
        AgentResult result=app.run(Fixture.replay(),6);
        printRun(result);
        state(app);
        String id=result.proposalId();

        heading("3. Execute before approval");
        System.out.println("execute("+id+") -> "+app.execute(id));
        state(app);

        heading("4. OPERATOR approves, then execute");
        System.out.println("approve("+id+") -> "+app.approve(id).status());
        System.out.println("execute("+id+") -> "+app.execute(id));
        state(app);
        System.out.println("notification: "+app.notifications().get(0).message());

        heading("5. Execute the same proposal again");
        System.out.println("execute("+id+") -> "+app.execute(id));
        state(app);

        heading("6. Ticks after the commit, and a backwards clock");
        System.out.println("tick(180) -> "+app.tick(180)+" (B1 is on open B12, so nothing is affected)");
        try { app.tick(30); }
        catch(IllegalArgumentException ex) { System.out.println("tick(30) -> REJECTED: "+ex.getMessage()); }

        heading("7. Rejected paths, each on a fresh application");
        StudentApplication rejected=new StudentApplication();
        String r=rejected.propose("B12").proposalId();
        rejected.reject(r);
        System.out.println("reject("+r+"), execute -> "+rejected.execute(r)+"; booking "+booking(rejected)+", commits "+rejected.commitCount());
        StudentApplication stale=new StudentApplication();
        String s=stale.propose("B12").proposalId();
        stale.approve(s);
        stale.changeBookingDuration(4);
        System.out.println("approve("+s+"), booking version changed, execute -> "+stale.execute(s)+"; booking "+booking(stale)+", commits "+stale.commitCount());
        StudentApplication taken=new StudentApplication();
        String u=taken.propose("B12").proposalId();
        taken.approve(u);
        taken.occupySpace("B12");
        System.out.println("approve("+u+"), B12 occupied, execute -> "+taken.execute(u)+"; booking "+booking(taken)+", commits "+taken.commitCount());
        StudentApplication bad=new StudentApplication();
        AgentResult invalid=bad.run(new ReplayProvider(List.of(
            "{\"tool\":\"PROPOSE_REASSIGNMENT\",\"bookingId\":\"B1\",\"targetId\":\"B12\",\"approved\":true}",
            "{\"tool\":\"DELETE_BOOKING\",\"bookingId\":\"B1\",\"targetId\":\"\"}",
            "not json","not json","not json","not json")),6);
        System.out.println("six invalid replies -> "+invalid.outcome()+" after "+invalid.calls()+" calls, "+invalid.invalidResponses()+" invalid; proposals "+bad.proposals().size());
        System.out.println("  first rejection sent back to the model: "+invalid.transcript().get(3).content());
        StudentApplication twice=new StudentApplication();
        twice.run(Fixture.replay(),6);
        AgentResult second=twice.run(new ReplayProvider(List.of(
            "{\"tool\":\"READ_BOOKING\",\"bookingId\":\"B1\",\"targetId\":\"\"}",
            "{\"tool\":\"LIST_ALTERNATIVES\",\"bookingId\":\"B1\",\"targetId\":\"\"}",
            "{\"tool\":\"PROPOSE_REASSIGNMENT\",\"bookingId\":\"B1\",\"targetId\":\"C03\"}",
            "{\"tool\":\"FINISH\",\"bookingId\":\"B1\",\"targetId\":\"\"}")),6);
        System.out.println("second agent run while P1 is PENDING -> "+second.outcome()+"; proposals "+twice.proposals().size());
        System.out.println("  propose answer: "+second.transcript().get(7).content());
    }

    // D1 regression demonstration: propose B12 (pending), D09 and Z99 (rejected)
    private static void scriptedD1() throws IOException {
        System.out.println("REPLAY D1 input: not a live model run; approval/execution are not invoked.");
        StudentApplication app=new StudentApplication();
        List<String> targets=List.of("B12","D09","Z99");
        ReplayProvider replay=new ReplayProvider(targets.stream().map(Fixture::proposalRequest).toList());
        for(int i=0;i<targets.size();i++) {
            var input=MiniJson.object(MiniJson.parse(replay.next(List.of(new ChatMessage("user","Propose a replacement for B1")))));
            String target=(String)input.get("targetId");
            System.out.println();
            System.out.println("Scripted target: "+target);
            try {
                ProposalView p=app.propose(target);
                System.out.println("PROPOSED "+p);
                System.out.println("AWAITING_APPROVAL: operator decision required; nothing approved or executed.");
            } catch(IllegalArgumentException ex) {
                System.out.println("REJECTED: "+ex.getMessage());
            }
            System.out.println("Booking: "+MiniJson.stringify(app.bookingSnapshot()));
        }
        System.out.println();
        System.out.println("Stored proposals: "+app.proposals().size());
    }

    // Live model run through the same loop; a commit needs the operator to type "y"
    private static void live() throws IOException {
        String host=System.getenv("OLLAMA_CHAT_URL"),model=System.getenv("OLLAMA_MODEL");
        if(host==null || model==null || host.isBlank() || model.isBlank()) {
            System.out.println("LIVE_RUN_PENDING_COURSE_ACCESS: configure the course-provided OLLAMA_CHAT_URL and OLLAMA_MODEL.");
            System.exit(2);
        }
        System.out.println("LIVE provider selected; model "+model+". Outcome must be recorded from actual execution.");
        StudentApplication app=new StudentApplication();
        AgentResult result=app.run(new OllamaProvider(URI.create(host),model,Duration.ofSeconds(30)),6);
        printRun(result);
        state(app);
        if(result.outcome()!=AgentOutcome.AWAITING_APPROVAL) return;
        String id=result.proposalId();
        System.out.print("OPERATOR: approve "+id+" and execute it? [y = approve, r = reject, anything else = leave pending]: ");
        System.out.flush();
        String answer=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8)).readLine();
        answer=answer==null?"":answer.trim();
        if(answer.equalsIgnoreCase("y")) {
            System.out.println("approve("+id+") -> "+app.approve(id).status());
            System.out.println("execute("+id+") -> "+app.execute(id));
        } else if(answer.equalsIgnoreCase("r")) {
            System.out.println("reject("+id+") -> "+app.reject(id).status());
        } else {
            System.out.println(id+" left PENDING; nothing approved or executed.");
        }
        state(app);
    }

    private static void heading(String title) {
        System.out.println();
        System.out.println("== "+title);
    }

    private static void printRun(AgentResult result) {
        for(ChatMessage m:result.transcript()) System.out.println("  ["+m.role()+"] "+m.content());
        System.out.println("outcome "+result.outcome()+"; calls "+result.calls()+"; invalid "+result.invalidResponses()
                           +"; proposal "+(result.proposalId()==null?"none":result.proposalId()));
    }

    private static String booking(StudentApplication app) {
        return app.bookingSnapshot().get("spaceId")+"/v"+app.bookingSnapshot().get("version");
    }

    // One line of protected state: booking, occupancy, proposals, commit and notification counts
    private static void state(StudentApplication app) {
        StringBuilder occupied=new StringBuilder();
        for(String id:List.of("A17","B12","C03","D09"))
            occupied.append(id).append(Boolean.TRUE.equals(app.spaceSnapshot(id).get("occupied"))?"=occupied ":"=free ");
        StringBuilder proposals=new StringBuilder();
        for(ProposalView p:app.proposals()) proposals.append(p.proposalId()).append("->").append(p.targetId()).append(":").append(p.status()).append(' ');
        System.out.println("STATE booking B1 "+booking(app)+" | "+occupied.toString().trim()+" | proposals "
                           +(proposals.length()==0?"none":proposals.toString().trim())
                           +" | commits "+app.commitCount()+" | notifications "+app.notifications().size());
    }
}
