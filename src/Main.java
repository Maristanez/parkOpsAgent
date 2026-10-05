import java.net.URI;
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
        StudentApplication app=new StudentApplication();
        try {
            if(mode.equals("scripted")) {
                System.out.println("REPLAY D1 input: not a live model run; approval/execution are not invoked.");
                // D1.3: the eligible B12, then the occupied D09 and an unknown target
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
            } else if(mode.equals("agent-scripted")) {
                System.out.println("REPLAY D2 workflow; not a live model run.");
                System.out.println(app.run(Fixture.replay(),6));
            } else if(mode.equals("live")) {
                String host=System.getenv("OLLAMA_CHAT_URL"),model=System.getenv("OLLAMA_MODEL");
                if(host==null || model==null || host.isBlank() || model.isBlank()) {
                    System.out.println("LIVE_RUN_PENDING_COURSE_ACCESS: configure the course-provided OLLAMA_CHAT_URL and OLLAMA_MODEL.");
                    System.exit(2);
                }
                System.out.println("LIVE provider selected; outcome must be recorded from actual execution.");
                System.out.println(app.run(new OllamaProvider(URI.create(host),model,Duration.ofSeconds(30)),6));
            } else {
                System.err.println("Modes: fixture | scripted (D1) | agent-scripted (D2) | live (D2)");System.exit(2);
            }
        } catch(UnsupportedOperationException ex) {
            System.err.println("STARTER_NOT_IMPLEMENTED: "+ex.getMessage());System.exit(1);
        }
    }
}
