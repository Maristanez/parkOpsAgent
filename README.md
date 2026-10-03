# ParkOps — cumulative individual project starter

Release: 2026-09-15. Read the matching Student Brief before editing.
This is an exercise starter. A clean compile does not mean the exercise is solved.
The public checks are expected to fail initially. Preserve them and add your own tests.
All data is synthetic. No personal paid AI account or model service is needed for the labs.

## First run

Extract the whole ZIP. Open its starter folder, containing this README and src/Main.java.
Use JDK 21. Check java -version and javac -version.

macOS/Linux (or Git Bash on Windows):

    bash run.sh compile
    bash run.sh run
    bash run.sh check
    bash setup-junit.sh
    bash run.sh test

Windows PowerShell:

    ./run.ps1 compile
    ./run.ps1 run
    ./run.ps1 check
    ./setup-junit.ps1
    ./run.ps1 test

If local policy blocks PowerShell scripts, use Git Bash or the lab-supported setup;
do not weaken institution-managed security policies. COURSE_JAVA_HOME may point to
a supported JDK folder. JAVA_HOME is also respected. Scripts always target Java 21.

setup-junit downloads the pinned JUnit 6.0.3 console jar from Maven Central and checks its
SHA-256. The TA can cache it once and provide JUNIT_JAR as an absolute path instead.
Compilation and plain Java checks work offline without JUnit. A missing dependency
is an environment issue; report it separately from failed behavioural checks.

## Your work

Implement your domain model and application in D1, then extend the same repository for D2 and D3. The supplied adapter, JSON codec, raw fixture and build scripts are infrastructure. DomainRules and StudentApplication contain explicit TODOs. Their public methods are a small demonstration/testing boundary, not a required internal architecture; you may adapt this boundary with a documented mapping. The existing PDF briefs define acceptance and marks. The initial check/test commands intentionally fail on unimplemented D1 requirements.

## Commands and evidence

- compile: compile source with Java 21 and compiler warnings enabled.
- run: small starter orientation or deliberately faulty demonstration, not a test suite.
- check: named plain Java checks, exit 1 when a required behaviour is missing.
- test: the same supplied cases through JUnit Jupiter plus your added test classes.
- lint: compile with -Xlint:all. Lab07 deliberately begins with raw/unchecked warnings.

Each check creates its own objects. Read the case name and assertion message; do not
delete failing cases or edit the captured output. Add focused Jupiter tests in
test/junit/StudentTests.java. An all-green public suite is not proof of every requirement.

For a lab submission include src/, test/, README.md, AI_USE.md and results.txt, plus
the diagram where the brief requests it. Do not submit build/, lib/, secrets or IDE caches.
Use the exact ZIP naming and end-of-session submission route in the brief.
The initial trace/design phase prohibits generative AI; permitted implementation assistance
must be disclosed. The supplied infrastructure is acknowledged in STARTER_PROVENANCE.md.

## References

- Java 21: https://docs.oracle.com/en/java/javase/21/
- JUnit 6.0.3 console: https://docs.junit.org/6.0.3/running-tests/console-launcher.html
