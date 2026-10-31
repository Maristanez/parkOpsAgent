#!/usr/bin/env python3
"""Builds DESIGN.pdf: the D2 design explanation plus two UML class diagrams, two sequence
diagrams and the proposal-state diagram.

Usage, from the project root:  python3 design/build.py
Writes design/DESIGN.html and DESIGN.pdf. The PDF step needs Google Chrome
(set CHROME to its executable if it is not in the default macOS location).
When the code changes, update the diagram data below so the figures still match it.
The D1 version of this script is in the git history (commit 07f55c4).
"""
import html
import os
import pathlib
import subprocess
import sys
import shutil
import tempfile
import time

ROOT = pathlib.Path(__file__).resolve().parent.parent
HTML_OUT = ROOT / "design" / "DESIGN.html"
PDF_OUT = ROOT / "DESIGN.pdf"
CHROME = os.environ.get("CHROME", "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome")

MONO = "Menlo, Consolas, 'DejaVu Sans Mono', monospace"
SANS = "Helvetica, Arial, sans-serif"
CHAR_W = 6.02  # width of one 10px monospace character
LINE_H = 13    # height of one member line


def esc(s):
    return html.escape(s, quote=True)


def text(x, y, s, size=10, anchor="start", family=SANS, bold=False, underline=False, italic=False):
    attrs = [f'x="{x:.1f}"', f'y="{y:.1f}"', f'font-size="{size}"', f'font-family="{family}"',
             f'text-anchor="{anchor}"', 'fill="#111"']
    if bold:
        attrs.append('font-weight="bold"')
    if underline:
        attrs.append('text-decoration="underline"')
    if italic:
        attrs.append('font-style="italic"')
    return f'<text {" ".join(attrs)}>{esc(s)}</text>'


def label(x, y, s, size=9.5, anchor="middle", italic=False, split=False):
    """Text with a white background so it stays readable over lines.
    With split=True, returns (background, text) so callers can layer them separately."""
    w = len(s) * size * 0.56 + 6
    rx = {"middle": x - w / 2, "start": x - 3, "end": x - w + 3}[anchor]
    bg = f'<rect x="{rx:.1f}" y="{y - size:.1f}" width="{w:.1f}" height="{size + 4:.1f}" fill="#fff"/>'
    fg = text(x, y, s, size=size, anchor=anchor, italic=italic)
    return (bg, fg) if split else bg + fg


def defs(p):
    return f"""<defs>
<marker id="{p}open" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="10" markerHeight="10" markerUnits="userSpaceOnUse" orient="auto"><path d="M0,0 L10,5 L0,10" fill="none" stroke="#222"/></marker>
<marker id="{p}tri" viewBox="0 0 12 12" refX="12" refY="6" markerWidth="12" markerHeight="12" markerUnits="userSpaceOnUse" orient="auto"><path d="M0,0 L12,6 L0,12 Z" fill="#fff" stroke="#222"/></marker>
<marker id="{p}diamond" viewBox="0 0 16 10" refX="0" refY="5" markerWidth="16" markerHeight="10" markerUnits="userSpaceOnUse" orient="auto"><path d="M0,5 L8,0 L16,5 L8,10 Z" fill="#222"/></marker>
<marker id="{p}solid" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="9" markerHeight="9" markerUnits="userSpaceOnUse" orient="auto"><path d="M0,0 L10,5 L0,10 Z" fill="#222"/></marker>
</defs>"""


# ---------------------------------------------------------------- class diagram

class Box:
    """One UML class box: header, attribute compartment, operation compartment."""

    def __init__(self, x, y, name, attrs=(), ops=(), stereo=None, starter=False, statics=(), literals=None):
        self.x, self.y, self.name, self.stereo, self.starter = x, y, name, stereo, starter
        self.attrs, self.ops, self.literals = list(attrs), list(ops), literals
        self.statics = set(self.attrs + self.ops) if statics == "all" else set(statics)
        members = self.attrs + self.ops + list(literals or [])
        longest = max([len(m) for m in members] + [len(name) + 2, len(stereo or "")])
        self.w = round(longest * CHAR_W + 16)
        self.head = 34 if stereo else 22
        if literals is not None:
            self.compartments = [list(literals)]
        elif stereo == "«interface»":
            self.compartments = [self.ops]
        else:
            self.compartments = [self.attrs, self.ops]
        self.h = self.head + sum(len(c) * LINE_H + 8 for c in self.compartments)

    def pt(self, side, t=0.5):
        return {"top": (self.x + self.w * t, self.y),
                "bottom": (self.x + self.w * t, self.y + self.h),
                "left": (self.x, self.y + self.h * t),
                "right": (self.x + self.w, self.y + self.h * t)}[side]

    def svg(self):
        x, y, w = self.x, self.y, self.w
        fill = "#e4e4e4" if self.starter else "#ffffff"
        out = [f'<rect x="{x}" y="{y}" width="{w}" height="{self.h}" fill="#fff" stroke="#222"/>',
               f'<rect x="{x}" y="{y}" width="{w}" height="{self.head}" fill="{fill}" stroke="#222"/>']
        ty = y + 15
        if self.stereo:
            out.append(text(x + w / 2, ty, self.stereo, size=9.5, anchor="middle"))
            ty += 13
        out.append(text(x + w / 2, ty, self.name, size=11, anchor="middle", bold=True))
        top = y + self.head
        for comp in self.compartments:
            my = top + 14
            for m in comp:
                out.append(text(x + 6, my, m, size=10, family=MONO, underline=m in self.statics))
                my += LINE_H
            top += len(comp) * LINE_H + 8
            out.append(f'<line x1="{x}" y1="{top}" x2="{x + w}" y2="{top}" stroke="#222"/>')
        return "\n".join(out)


def link(p, a, b, kind, name=None, at=0.5, dx=0, dy=-4, ends=()):
    """kind: comp (composition), assoc (navigable association), dep (dependency), real (realization)."""
    (x1, y1), (x2, y2) = a, b
    parts = [f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="#222"']
    if kind in ("dep", "real"):
        parts.append('stroke-dasharray="5,3"')
    if kind == "comp":
        parts.append(f'marker-start="url(#{p}diamond)"')
    parts.append(f'marker-end="url(#{p}{"tri" if kind == "real" else "open"})"')
    out = [" ".join(parts) + "/>"]
    if name:
        out.append(label(x1 + (x2 - x1) * at + dx, y1 + (y2 - y1) * at + dy, name))
    for s, ex, ey in ends:
        out.append(label(x2 + ex, y2 + ey, s, anchor="start"))
    return "\n".join(out)


def poly(p, pts, kind, ends=(), name=None, name_at=None):
    """A link drawn through waypoints; same kinds and end labels as link()."""
    d = "M" + " L".join(f"{x:.1f},{y:.1f}" for x, y in pts)
    parts = [f'<path d="{d}" fill="none" stroke="#222"']
    if kind in ("dep", "real"):
        parts.append('stroke-dasharray="5,3"')
    if kind == "comp":
        parts.append(f'marker-start="url(#{p}diamond)"')
    parts.append(f'marker-end="url(#{p}{"tri" if kind == "real" else "open"})"')
    out = [" ".join(parts) + "/>"]
    x2, y2 = pts[-1]
    for s, ex, ey in ends:
        out.append(label(x2 + ex, y2 + ey, s, anchor="start"))
    if name:
        out.append(label(name_at[0], name_at[1], name))
    return "\n".join(out)


def region(x, y, w, h, title):
    """A light dashed frame naming one of the four kinds of code D2.4 separates."""
    return (f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="8" fill="#fafafa" stroke="#999" stroke-dasharray="6,4"/>'
            + text(x + 8, y + 14, title, size=10.5, bold=True, italic=True))


# ---------------------------------------------------------------- class diagrams

def class_diagram_core():
    """Figure 1: coordination, storage and domain rules."""
    p = "c1-"
    app = Box(345, 30, "StudentApplication",
              attrs=["- nextProposalNumber: int"],
              ops=["+ propose(String): ProposalView",
                   "+ run(ModelProvider, int): AgentResult",
                   "+ approve(String): ProposalView",
                   "+ reject(String): ProposalView",
                   "+ execute(String): ExecutionOutcome",
                   "+ tick(long): List<WorkItem>",
                   "+ retry(String): void",
                   "+ changeBookingDuration(int): void",
                   "+ closeSpace(String): void",
                   "+ occupySpace(String): void",
                   "+ bookingSnapshot(): Map<String,Object>",
                   "+ spaceSnapshot(String): Map<String,Object>",
                   "+ proposals(): List<ProposalView>",
                   "+ commitCount(): int",
                   "+ notifications(): List<Notification>",
                   "+ policyVersion(): int",
                   "+ alternatives(): List<String>",
                   "+ spaceIds(): Set<String>",
                   "- commit(Proposal, Booking, Space): void"])
    checker = Box(20, 30, "PeriodicChecker",
                  attrs=["- PERIOD_SECONDS: long = 60", "- lastTick: Long", "- nextScan: Long", "- emitted: Set<String>"],
                  ops=["~ tick(long): List<WorkItem>", "~ retry(String): void", "- scan(long): List<WorkItem>",
                       "- hasActiveProposal(Booking): boolean"],
                  statics=["- PERIOD_SECONDS: long = 60"])
    work = Box(20, 210, "WorkItem", stereo="«record»",
               attrs=["bookingId: String", "bookingVersion: int", "policyVersion: int", "detectedAt: long"])
    policy = Box(20, 345, "EligibilityPolicy", attrs=["- VERSION: int = 1"],
                 ops=["+ getVersion(): int", "+ isEligible(Booking, Space): boolean"], statics=["- VERSION: int = 1"])
    outcome = Box(660, 30, "ExecutionOutcome", stereo="«enumeration»",
                  literals=["NOT_APPROVED", "STALE", "UNAVAILABLE_OR_INELIGIBLE", "COMMITTED", "DUPLICATE"])
    view = Box(848, 30, "ProposalView", stereo="«record»", starter=True,
               attrs=["proposalId: String", "bookingId: String", "targetId: String", "bookingVersion: int",
                      "policyVersion: int", "status: String"])
    commit = Box(660, 195, "CommitRecord", stereo="«record»",
                 attrs=["proposalId: String", "bookingId: String", "fromSpaceId: String", "toSpaceId: String",
                        "newBookingVersion: int"])
    note = Box(848, 208, "Notification", stereo="«record»", attrs=["bookingId: String", "message: String"])
    store = Box(700, 352, "ParkingStore",
                attrs=["- booking: Booking"],
                ops=["~ booking(): Booking", "~ replaceBooking(Booking): void", "~ space(String): Space",
                     "~ proposal(String): Proposal", "~ addProposal(Proposal): void",
                     "~ recordCommit(CommitRecord): void", "~ addNotification(Notification): void",
                     "~ commitCount(): int", "~ notifications(): List<Notification>"])
    rules = Box(345, 395, "DomainRules", stereo="«utility»",
                ops=["+ requireDuration(int): void", "+ requireIdentifier(String): void"], statics="all")
    booking = Box(20, 576, "Booking",
                  attrs=["- id: String", "- spaceId: String", "- durationHours: int", "- version: int",
                         "- requiresAccessible: boolean"],
                  ops=["+ Booking(String, String, int, int, boolean)", "+ movedTo(String): Booking",
                       "+ withDuration(int): Booking"])
    space = Box(318, 576, "Space",
                attrs=["- id: String", "- open: boolean", "- occupied: boolean", "- accessible: boolean"],
                ops=["+ isAvailable(): boolean", "+ occupy(): void", "+ release(): void", "+ close(): void"])
    proposal = Box(565, 576, "Proposal",
                   attrs=["- proposalId: String", "- bookingId: String", "- targetId: String",
                          "- observedBookingVersion: int", "- observedPolicyVersion: int"],
                   ops=["+ approve(): void", "+ reject(): void", "+ markExecuted(): void", "+ toView(): ProposalView"])
    status = Box(868, 576, "ProposalStatus", stereo="«enumeration»",
                 literals=["PENDING", "APPROVED", "REJECTED", "EXECUTED"])
    boxes = [app, checker, work, policy, outcome, view, commit, note, store, rules, booking, space, proposal, status]

    regions = [
        region(10, 6, 640, 340, "Coordination"),
        region(655, 6, 337, 162, "Results returned to callers"),
        region(655, 172, 337, 365, "Data storage"),
        region(10, 352, 640, 185, "Domain rules: policy and validation"),
        region(10, 552, 982, 198, "Domain rules: entities and statuses"),
    ]
    links = [
        link(p, app.pt("left", 0.12), checker.pt("right", 0.45), "comp", ends=[("-checker", 6, -6), ("1", 6, 14)]),
        link(p, app.pt("left", 0.9), policy.pt("right", 0.35), "comp", ends=[("-policy", 6, -6), ("1", 6, 14)]),
        link(p, app.pt("right", 0.93), store.pt("left", 0.18), "comp", ends=[("-store", -52, -8), ("1", -14, 16)]),
        poly(p, [checker.pt("right", 0.92), (318, checker.pt("right", 0.92)[1]), (318, 392), (store.x, 392)], "assoc",
             ends=[("-store", -46, -6)]),
        link(p, checker.pt("bottom", 0.88), policy.pt("top", 0.88), "assoc", ends=[("-policy", 4, -24)]),
        link(p, checker.pt("bottom", 0.3), work.pt("top", 0.45), "dep", "«create»", at=0.5, dx=26),
        link(p, store.pt("top", 0.15), commit.pt("bottom", 0.4), "comp", ends=[("0..*", 4, 14)]),
        link(p, store.pt("top", 0.85), note.pt("bottom", 0.5), "comp", ends=[("0..*", 4, 14)]),
        link(p, store.pt("bottom", 0.08), booking.pt("top", 0.85), "comp", ends=[("-booking 1", 6, -8)]),
        link(p, store.pt("bottom", 0.3), space.pt("top", 0.75), "comp", ends=[("-spaces 1..*", 6, -8)]),
        link(p, store.pt("bottom", 0.55), proposal.pt("top", 0.7), "comp", ends=[("-proposals 0..*", 6, -8)]),
        link(p, proposal.pt("right", 0.25), status.pt("left", 0.4), "assoc", ends=[("-status 1", -62, -6)]),
        link(p, app.pt("right", 0.2), outcome.pt("left", 0.6), "dep", "«create»", at=0.45),
        link(p, policy.pt("bottom", 0.3), booking.pt("top", 0.25), "dep", "«use»", at=0.5),
        link(p, policy.pt("bottom", 0.7), space.pt("top", 0.15), "dep", "«use»", at=0.6),
        link(p, booking.pt("top", 0.6), rules.pt("left", 0.6), "dep"),
        link(p, space.pt("top", 0.4), rules.pt("bottom", 0.4), "dep"),
        link(p, proposal.pt("top", 0.2), rules.pt("right", 0.7), "dep"),
    ]
    legend = [
        text(20, 768, "Grey header: supplied starter class.  Underlined: static.  + public, - private, ~ package-private.  "
                      "Simple getters are omitted.  Filled diamond: composition.", size=10),
        text(20, 784, "Solid line, open arrow: association.  Dashed line: dependency.  StudentApplication and "
                      "PeriodicChecker share one ParkingStore and one EligibilityPolicy.", size=10),
        text(20, 800, "Proposal.toView() creates the ProposalView records StudentApplication returns.  Domain classes "
                      "import nothing from coordination, storage or the model protocol.", size=10),
    ]
    body = "\n".join([defs(p)] + regions + links + [b.svg() for b in boxes] + legend)
    return f'<svg viewBox="0 0 1000 810" xmlns="http://www.w3.org/2000/svg">{body}</svg>'


def class_diagram_agent():
    """Figure 2: the model/tool loop, the protocol classes and the provider boundary."""
    p = "c2-"
    main = Box(20, 30, "Main", statics="all",
               ops=["+ main(String[]): void", "- scriptedD2(): void", "- scriptedD1(): void", "- live(): void"])
    app = Box(280, 30, "StudentApplication",
              ops=["+ run(ModelProvider, int): AgentResult", "+ bookingSnapshot(): Map<String,Object>",
                   "+ alternatives(): List<String>", "+ propose(String): ProposalView", "+ policyVersion(): int",
                   "+ spaceIds(): Set<String>", "(operator actions: Figure 1)"])
    runner = Box(280, 245, "AgentRunner",
                 attrs=["- MAX_CALLS: int = 6", "- limit: int", "- calls: int", "- invalidResponses: int",
                        "- hasRead: boolean", "- hasListed: boolean", "- readBookingVersion: int",
                        "- readPolicyVersion: int"],
                 ops=["~ AgentRunner(StudentApplication, ModelProvider, int)", "~ run(): AgentResult",
                      "- isStale(): boolean", "- activeProposalId(): String",
                      "- observe(Map<String,Object>): void"],
                 statics=["- MAX_CALLS: int = 6"])
    result = Box(20, 245, "AgentResult", stereo="«record»",
                 attrs=["outcome: AgentOutcome", "calls: int", "invalidResponses: int", "proposalId: String",
                        "transcript: List<ChatMessage>"])
    aoutcome = Box(20, 410, "AgentOutcome", stereo="«enumeration»",
                   literals=["AWAITING_APPROVAL", "NO_ACTION", "STEP_LIMIT", "PROVIDER_ERROR"])
    model = Box(745, 245, "ModelProvider", stereo="«interface»", starter=True,
                ops=["+ next(List<ChatMessage>): String"])
    replay = Box(745, 80, "ReplayProvider", starter=True,
                 attrs=["- responses: List<String>"], ops=["+ next(List<ChatMessage>): String"])
    ollama = Box(745, 345, "OllamaProvider", starter=True,
                 attrs=["- endpoint: URI", "- model: String", "- timeout: Duration"],
                 ops=["+ next(List<ChatMessage>): String"])
    chat = Box(800, 505, "ChatMessage", stereo="«record»", starter=True, attrs=["role: String", "content: String"])
    parser = Box(280, 520, "ToolRequestParser",
                 attrs=["- PROPERTIES: List<String>"],
                 ops=["+ parse(String, String, Set<String>): Result", "- toolNamed(String): Tool"], statics="all")
    res = Box(20, 560, "ToolRequestParser.Result", stereo="«record»",
              attrs=["request: ToolRequest", "rejection: String"], ops=["+ accepted(): boolean"])
    req = Box(20, 668, "ToolRequest", stereo="«record»", attrs=["tool: Tool", "bookingId: String", "targetId: String"])
    tool = Box(280, 640, "Tool", stereo="«enumeration»",
               literals=["READ_BOOKING", "LIST_ALTERNATIVES", "PROPOSE_REASSIGNMENT", "FINISH"])
    mini = Box(600, 620, "MiniJson", starter=True, statics="all",
               ops=["+ parse(String): Object", "+ object(Object): Map<String,Object>", "+ stringify(Object): String"])
    boxes = [main, app, runner, result, aoutcome, model, replay, ollama, chat, parser, res, req, tool, mini]

    regions = [
        region(10, 6, 718, 500, "Coordination"),
        region(735, 56, 257, 520, "Provider boundary (starter)"),
        region(10, 512, 718, 268, "Model protocol"),
    ]
    links = [
        link(p, main.pt("right", 0.4), app.pt("left", 0.2), "dep", "«create»", at=0.5),
        poly(p, [main.pt("top", 0.7), (main.pt("top", 0.7)[0], 18), (replay.pt("top", 0.9)[0], 18), replay.pt("top", 0.9)],
             "dep", name="«create»", name_at=(640, 22)),
        link(p, main.pt("bottom", 0.4), result.pt("top", 0.4), "dep", "«use»", at=0.5),
        link(p, app.pt("bottom", 0.3), runner.pt("top", 0.3), "dep", "«create»", at=0.5, dx=-30),
        link(p, runner.pt("top", 0.7), app.pt("bottom", 0.7), "assoc", ends=[("-app 1", 6, 16)]),
        link(p, runner.pt("right", 0.3), model.pt("left", 0.5), "assoc", ends=[("-provider 1", -78, -8)]),
        link(p, replay.pt("bottom", 0.5), model.pt("top", 0.5), "real"),
        link(p, ollama.pt("top", 0.5), model.pt("bottom", 0.5), "real"),
        link(p, runner.pt("right", 0.8), chat.pt("left", 0.35), "assoc", ends=[("-history 0..*", -98, -10)]),
        link(p, runner.pt("left", 0.25), result.pt("right", 0.4), "dep", "«create»", at=0.5),
        link(p, result.pt("bottom", 0.3), aoutcome.pt("top", 0.3), "assoc", ends=[("1", 6, -6)]),
        link(p, runner.pt("bottom", 0.4), parser.pt("top", 0.4), "dep", "«use»", at=0.5),
        link(p, parser.pt("left", 0.5), res.pt("right", 0.3), "dep", "«create»", at=0.5),
        link(p, res.pt("bottom", 0.5), req.pt("top", 0.5), "assoc", ends=[("0..1", 6, -6)]),
        link(p, req.pt("right", 0.4), tool.pt("left", 0.5), "assoc", ends=[("1", -14, -6)]),
        link(p, parser.pt("right", 0.6), mini.pt("left", 0.3), "dep", "«use»", at=0.5),
    ]
    legend = [
        text(20, 796, "AgentRunner reaches the domain only through StudentApplication's public methods, so the model "
                      "can never approve, execute or skip a check.", size=10),
        text(20, 828, "Main creates the ReplayProvider in scripted mode and the OllamaProvider in live mode "
                      "(only the first dependency is drawn).", size=10),
        text(20, 812, "Grey header: supplied starter class.  Dashed line, hollow triangle: realization.  Dashed line, "
                      "open arrow: dependency.  Solid line: association.", size=10),
    ]
    body = "\n".join([defs(p)] + regions + links + [b.svg() for b in boxes] + legend)
    return f'<svg viewBox="0 0 1000 836" xmlns="http://www.w3.org/2000/svg">{body}</svg>'


# ------------------------------------------------------------ sequence diagrams

def seq(p, lanes, msgs, step=18, boundary=None, frames=(), created=(), footer=""):
    """Generic sequence diagram.
    lanes: (key, [header lines], x); msgs: (kind, from, to, label) with kind call|ret|self|create.
    boundary: ("top" | "bottom", line1, line2). frames: (label, first msg, last msg, x1, x2).
    Activations are derived from matching call/return pairs."""
    X = {k: x for k, _, x in lanes}
    head_h = 34
    top_pad = 70 if boundary and boundary[0] == "top" else 0
    head_y = 14 + top_pad
    ys, y = [], head_y + head_h + 22
    frame_starts = {f[1] for f in frames}
    for i, (kind, *_rest) in enumerate(msgs):
        if i in frame_starts:
            y += 16
        if kind == "create":
            y += 10
        ys.append(y)
        y += step + (10 if kind == "self" else 0) + (10 if kind == "create" else 0)
    end_y = y + 6
    bottom = end_y + (70 if boundary and boundary[0] == "bottom" else 10)

    created_y = {}
    for (kind, a, b, s), my in zip(msgs, ys):
        if kind == "create" and b in created:
            created_y[b] = my

    # activations from call/return pairs
    bars, stack = [], [(msgs[0][1], 0)]
    for i, (kind, a, b, s) in enumerate(msgs):
        if kind == "call":
            stack.append((b, i))
        elif kind == "ret":
            while stack and stack[-1][0] != a:
                k, start = stack.pop()
                bars.append((k, start, start))
            if stack:
                k, start = stack.pop()
                bars.append((k, start, i))
    bars += [(k, start, len(msgs) - 1) for k, start in stack]

    out, heads, tags = [defs(p)], [], []
    if boundary and boundary[0] == "top":
        out.append(f'<line x1="12" y1="18" x2="988" y2="18" stroke="#222" stroke-width="2.2" stroke-dasharray="10,5"/>')
        out.append(label(500, 38, boundary[1], size=11))
        out.append(label(500, 55, boundary[2], size=10.5))
    for key, lines, x in lanes:
        top = created_y[key] - head_h / 2 if key in created_y else head_y
        w = max(len(s) for s in lines) * 6.0 + 16
        out.append(f'<line x1="{x}" y1="{top + head_h}" x2="{x}" y2="{end_y}" stroke="#666" stroke-dasharray="4,3"/>')
        heads.append(f'<rect x="{x - w / 2:.1f}" y="{top:.1f}" width="{w:.1f}" height="{head_h}" fill="#fff" stroke="#222"/>')
        if len(lines) == 1:
            heads.append(text(x, top + 21, lines[0], size=10.5, anchor="middle", bold=True))
        else:
            heads.append(text(x, top + 14, lines[0], size=9.5, anchor="middle"))
            heads.append(text(x, top + 27, lines[1], size=10.5, anchor="middle", bold=True))
    for label_text, a, b, x1, x2 in frames:
        y1, y2 = ys[a] - 24, ys[b] + 26
        out.append(f'<rect x="{x1}" y="{y1:.1f}" width="{x2 - x1}" height="{y2 - y1:.1f}" fill="none" stroke="#444"/>')
        tw = len(label_text) * 5.6 + 14
        tags.append(f'<path d="M{x1},{y1:.1f} h{tw:.1f} v10 l-6,6 h-{tw - 6:.1f} Z" fill="#fff" stroke="#444"/>')
        tags.append(text(x1 + 5, y1 + 12, label_text, size=9.5, bold=True))
    rects = []
    for key, a, b in bars:
        if key not in X:
            continue
        y1 = ys[a] - 6
        y2 = ys[b] + (22 if msgs[b][0] == "self" else 6)
        rects.append(f'<rect x="{X[key] - 5}" y="{y1:.1f}" width="10" height="{max(y2 - y1, 12):.1f}" fill="#f2f2f2" stroke="#222"/>')

    backgrounds, labels = [], []
    lane_w = {k: max(len(s) for s in lines) * 6.0 + 16 for k, lines, _ in lanes}
    for (kind, a, b, s), my in zip(msgs, ys):
        if kind == "self":
            x = X[a] + 5
            out.append(f'<path d="M{x},{my} h28 v12 h-28" fill="none" stroke="#222" marker-end="url(#{p}solid)"/>')
            bg, fg = label(x + 34, my + 10, s, anchor="start", split=True)
            backgrounds.append(bg)
            labels.append(fg)
            continue
        x1, x2 = X[a], X[b]
        sign = 1 if x2 > x1 else -1
        start = x1 + 5 * sign
        end = x2 - (lane_w[b] / 2) * sign if kind == "create" and b in created else x2 - 5 * sign
        dash = ' stroke-dasharray="5,3"' if kind in ("ret", "create") else ""
        marker = "solid" if kind == "call" else "open"
        out.append(f'<line x1="{start:.1f}" y1="{my}" x2="{end:.1f}" y2="{my}" stroke="#222"{dash} marker-end="url(#{p}{marker})"/>')
        if s:
            bg, fg = label((start + end) / 2, my - 4, s, split=True)
            backgrounds.append(bg)
            labels.append(fg)
    out += backgrounds + rects + labels + tags + heads

    if boundary and boundary[0] == "bottom":
        by = end_y + 14
        out.append(f'<line x1="12" y1="{by}" x2="988" y2="{by}" stroke="#222" stroke-width="2.2" stroke-dasharray="10,5"/>')
        out.append(label(500, by + 19, boundary[1], size=11))
        out.append(label(500, by + 36, boundary[2], size=10.5))
    out.append(text(12, bottom + 16, footer or "Solid line, filled arrow: call.  Dashed line, open arrow: return or «create».  "
                                              "Shaded bars: activations.", size=10))
    return f'<svg viewBox="0 0 1000 {bottom + 24}" xmlns="http://www.w3.org/2000/svg">{"".join(out)}</svg>'


def sequence_run():
    """Figure 3: the replayed agent run up to the pending proposal (bash run.sh run scripted, step 2)."""
    lanes = [("main", [":Main"], 45), ("app", ["app:StudentApplication"], 175), ("runner", [":AgentRunner"], 330),
             ("replay", [":ReplayProvider"], 460), ("parser", ["«static»", "ToolRequestParser"], 590),
             ("policy", [":EligibilityPolicy"], 715), ("store", ["store:ParkingStore"], 830),
             ("p1", ["p1:Proposal"], 935)]
    msgs = [
        ("call", "main", "app", "run(replay, 6)"),
        ("create", "app", "runner", "«create»"),
        ("call", "app", "runner", "run()"),
        ("call", "runner", "replay", "next(history)"),
        ("ret", "replay", "runner", "READ_BOOKING request"),
        ("call", "runner", "parser", 'parse(reply, "B1", spaceIds)'),
        ("ret", "parser", "runner", "accepted"),
        ("call", "runner", "app", "bookingSnapshot()"),
        ("ret", "app", "runner", "B1: A17, version 0"),
        ("self", "runner", None, "observe(OK, policyVersion 1)"),
        ("call", "runner", "replay", "next(history)"),
        ("ret", "replay", "runner", "LIST_ALTERNATIVES request"),
        ("call", "runner", "parser", "parse(…)"),
        ("ret", "parser", "runner", "accepted"),
        ("self", "runner", None, "isStale() → false"),
        ("call", "runner", "app", "alternatives()"),
        ("call", "app", "policy", "isEligible(b1, space) for each"),
        ("ret", "policy", "app", "B12, C03 eligible"),
        ("ret", "app", "runner", '["B12","C03"]'),
        ("self", "runner", None, "observe(OK, alternatives)"),
        ("call", "runner", "replay", "next(history)"),
        ("ret", "replay", "runner", "PROPOSE_REASSIGNMENT B12"),
        ("call", "runner", "parser", "parse(…)"),
        ("ret", "parser", "runner", "accepted"),
        ("self", "runner", None, "isStale() → false"),
        ("call", "runner", "app", 'propose("B12")'),
        ("call", "app", "policy", "isEligible(b1, b12)"),
        ("ret", "policy", "app", "true"),
        ("create", "app", "p1", '«create» ("P1", "B1", "B12", 0, 1): PENDING'),
        ("call", "app", "store", "addProposal(p1)"),
        ("ret", "store", "app", ""),
        ("ret", "app", "runner", "ProposalView(P1, PENDING)"),
        ("self", "runner", None, "observe(AWAITING_APPROVAL)"),
        ("ret", "runner", "app", "AgentResult(AWAITING_APPROVAL, 3 calls, P1)"),
        ("ret", "app", "main", "AgentResult"),
    ]
    return seq("s1-", lanes, msgs, step=17, created={"runner", "p1"},
               frames=[("loop [calls < limit]: one model call per turn", 3, 32, 105, 990)],
               boundary=("bottom", "APPROVAL BOUNDARY: the run stops at AWAITING_APPROVAL. P1 is PENDING; B1 is still at A17, version 0.",
                         "Nothing in the run can approve or execute. Only a separate operator command continues (Figure 4)."),
               footer="Solid line, filled arrow: call.  Dashed line, open arrow: return or «create».  Each turn also "
                      "appends the reply and the observation to the history.  An invalid reply is answered with REJECTED and counted.")


def sequence_execute():
    """Figure 4: operator approval, execution with re-checks, and the repeated execution."""
    lanes = [("main", ["operator command", ":Main"], 50), ("app", ["app:StudentApplication"], 205),
             ("store", ["store:ParkingStore"], 350), ("p1", ["p1:Proposal"], 460),
             ("policy", [":EligibilityPolicy"], 565), ("b1", ["b1:Booking"], 675),
             ("a17", ["a17:Space"], 775), ("b12", ["b12:Space"], 870), ("b1n", ["b1′:Booking"], 955)]
    msgs = [
        ("call", "main", "app", 'approve("P1")'),
        ("call", "app", "store", 'proposal("P1")'),
        ("ret", "store", "app", "p1"),
        ("call", "app", "p1", "approve()"),
        ("ret", "p1", "app", "PENDING → APPROVED"),
        ("ret", "app", "main", "P1: APPROVED"),
        ("call", "main", "app", 'execute("P1")'),
        ("call", "app", "store", 'proposal("P1")'),
        ("ret", "store", "app", "p1"),
        ("call", "app", "p1", "getStatus()"),
        ("ret", "p1", "app", "APPROVED (not EXECUTED)"),
        ("self", "app", None, "observed 0/1 = current 0/1 (not STALE)"),
        ("call", "app", "policy", "isEligible(b1, b12)"),
        ("call", "policy", "b12", "isAvailable()"),
        ("ret", "b12", "policy", "true"),
        ("ret", "policy", "app", "true"),
        ("self", "app", None, "commit(p1, b1, b12)"),
        ("call", "app", "b12", "occupy()"),
        ("ret", "b12", "app", ""),
        ("call", "app", "a17", "release()"),
        ("ret", "a17", "app", ""),
        ("call", "app", "b1", 'movedTo("B12")'),
        ("create", "b1", "b1n", "«create» (B12, version 1)"),
        ("ret", "b1", "app", "b1′"),
        ("call", "app", "store", "replaceBooking(b1′)"),
        ("ret", "store", "app", ""),
        ("call", "app", "p1", "markExecuted()"),
        ("ret", "p1", "app", "APPROVED → EXECUTED"),
        ("call", "app", "store", "recordCommit(c)"),
        ("ret", "store", "app", ""),
        ("call", "app", "store", "addNotification(n)"),
        ("ret", "store", "app", ""),
        ("ret", "app", "main", "COMMITTED"),
        ("call", "main", "app", 'execute("P1")  again'),
        ("call", "app", "p1", "getStatus()"),
        ("ret", "p1", "app", "EXECUTED"),
        ("ret", "app", "main", "DUPLICATE"),
    ]
    return seq("s2-", lanes, msgs, step=17, created={"b1n"},
               boundary=("top", "APPROVAL BOUNDARY (continued from Figure 3): each call below is a separate operator command.",
                         "The model is not involved. execute() runs every check and the commit with no model or external call in between."),
               footer="Solid line, filled arrow: call.  Dashed line, open arrow: return or «create».  A failed check "
                      "returns NOT_APPROVED, STALE or UNAVAILABLE_OR_INELIGIBLE before commit() and changes nothing.")


# --------------------------------------------------------------- state diagram

def state_diagram():
    """Figure 5: proposal states and transitions."""
    p = "st-"
    W, H = 150, 46
    S = {"PENDING": (300, 150), "APPROVED": (620, 150), "REJECTED": (300, 400), "EXECUTED": (620, 400)}

    def box(name):
        x, y = S[name]
        return (f'<rect x="{x - W / 2}" y="{y - H / 2}" width="{W}" height="{H}" rx="14" fill="#fff" stroke="#222" stroke-width="1.4"/>'
                + text(x, y + 5, name, size=12.5, anchor="middle", bold=True))

    def arrow(x1, y1, x2, y2, s, lx, ly, anchor="middle"):
        return (f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="#222" stroke-width="1.3" marker-end="url(#{p}open)"/>'
                + label(lx, ly, s, size=10.5, anchor=anchor))

    def loop(name, side, lines):
        x, y = S[name]
        if side == "top":
            d = f"M{x - 30},{y - H / 2} C{x - 40},{y - 95} {x + 40},{y - 95} {x + 30},{y - H / 2}"
            lx, ly, anchor = x, y - 92, "middle"
        elif side == "right":
            d = f"M{x + W / 2},{y - 12} C{x + W / 2 + 70},{y - 40} {x + W / 2 + 70},{y + 40} {x + W / 2},{y + 12}"
            lx, ly, anchor = x + W / 2 + 62, y - 6, "start"
        else:
            d = f"M{x - W / 2},{y - 12} C{x - W / 2 - 70},{y - 40} {x - W / 2 - 70},{y + 40} {x - W / 2},{y + 12}"
            lx, ly, anchor = x - W / 2 - 62, y - 6, "end"
        out = [f'<path d="{d}" fill="none" stroke="#222" stroke-width="1.3" marker-end="url(#{p}open)"/>']
        for i, s in enumerate(lines):
            out.append(label(lx, ly + i * 15, s, size=10.5, anchor=anchor))
        return "".join(out)

    out = [defs(p)]
    out.append('<circle cx="80" cy="150" r="9" fill="#222"/>')
    out.append(arrow(89, 150, 300 - W / 2, 150, "propose(target) [target eligible]", 140, 128))
    out.append(arrow(300 + W / 2, 150, 620 - W / 2, 150, "approve()", 460, 140))
    out.append(label(460, 172, "operator", size=9.5, italic=True))
    out.append(arrow(300, 150 + H / 2, 300, 400 - H / 2, "reject()  (operator)", 306, 280, anchor="start"))
    out.append(arrow(620, 150 + H / 2, 620, 400 - H / 2, "execute() [versions match and target eligible]", 626, 268, anchor="start"))
    out.append(label(626, 284, "/ commit: move booking, version + 1, record", size=10.5, anchor="start"))
    out.append(label(626, 299, "one commit and one notification → COMMITTED", size=10.5, anchor="start"))
    out.append(loop("PENDING", "top", ["execute() / NOT_APPROVED, no change"]))
    out.append(loop("APPROVED", "top", ["execute() [version changed] / STALE, no change",
                                        "execute() [target closed or taken] / UNAVAILABLE_OR_INELIGIBLE, no change"]))
    out.append(loop("REJECTED", "left", ["execute() / NOT_APPROVED,", "no change"]))
    out.append(loop("EXECUTED", "right", ["execute() / DUPLICATE,", "no second commit", "or notification"]))
    for name in S:
        out.append(box(name))
    for name in ("REJECTED", "EXECUTED"):
        x, y = S[name]
        out.append(f'<line x1="{x}" y1="{y + H / 2}" x2="{x}" y2="{y + H / 2 + 34}" stroke="#222" stroke-width="1.3" marker-end="url(#{p}open)"/>')
        out.append(f'<circle cx="{x}" cy="{y + H / 2 + 44}" r="10" fill="#fff" stroke="#222" stroke-width="1.3"/>'
                   f'<circle cx="{x}" cy="{y + H / 2 + 44}" r="6" fill="#222"/>')
    out.append(text(20, 520, "approve() or reject() on any state other than PENDING, and markExecuted() on any state other "
                             "than APPROVED, throw IllegalStateException and leave the status unchanged.", size=10.5))
    out.append(text(20, 537, "REJECTED and EXECUTED are final.  The model can only cause the first transition (propose); "
                             "every other transition is an operator command checked in Java.", size=10.5))
    return f'<svg viewBox="0 0 1000 550" xmlns="http://www.w3.org/2000/svg">{"".join(out)}</svg>'


# ------------------------------------------------------------------- document

def c(s):
    return f"<code>{esc(s)}</code>"


def table(head, rows):
    th = "".join(f"<th>{h}</th>" for h in head)
    trs = "".join("<tr>" + "".join(f"<td>{cell}</td>" for cell in row) + "</tr>" for row in rows)
    return f"<table><thead><tr>{th}</tr></thead><tbody>{trs}</tbody></table>"


CSS = """
@page { size: letter; margin: 0.65in 0.75in; }
@page landscape { size: letter landscape; margin: 0.35in 0.4in; }
body { font-variant-numeric: lining-nums; font-family: Charter, 'Bitstream Charter', 'Times New Roman', serif; font-size: 10.3pt; line-height: 1.36; color: #111; }
h1 { font-family: Helvetica, Arial, sans-serif; font-size: 17pt; margin: 0 0 2pt; }
.meta { font-family: Helvetica, Arial, sans-serif; font-size: 9.5pt; color: #444; margin-bottom: 10pt; }
h2 { font-family: Helvetica, Arial, sans-serif; font-size: 12pt; margin: 13pt 0 4pt; break-after: avoid; }
h3 { font-family: Helvetica, Arial, sans-serif; font-size: 10.5pt; margin: 9pt 0 3pt; break-after: avoid; }
p { margin: 0 0 6pt; }
code { font-family: Menlo, Consolas, monospace; font-size: 8.6pt; }
table { border-collapse: collapse; width: 100%; font-size: 9pt; line-height: 1.3; margin: 4pt 0 8pt; }
th, td { border: 0.6pt solid #999; padding: 3pt 5pt; vertical-align: top; text-align: left; }
th { background: #eee; font-family: Helvetica, Arial, sans-serif; }
tr { break-inside: avoid; }
ul { margin: 0 0 6pt; padding-left: 16pt; }
li { margin-bottom: 2pt; }
.fig { page: landscape; break-before: page; }
.fig svg { display: block; width: 100%; height: auto; max-height: 7.35in; }
.cap { font-family: Helvetica, Arial, sans-serif; font-size: 10pt; margin: 0 0 4pt; }
"""


def document():
    s = []
    s.append("<h1>ParkOps D2: Design</h1>")
    s.append('<div class="meta">EECS 3311 B, Fall 2026 · Deliverable 2: a working supervised agent · '
             "Route A (ParkOps) · Bryan Maristanez · November 2026</div>")

    s.append("<h2>1. Scope</h2>")
    s.append(f"<p>D2 turns D1's proposal path into a supervised agent for the supplied fixture. "
             f"{c('StudentApplication.run(ModelProvider, int)')} runs a bounded model/tool loop that can read booking B1, "
             f"list eligible spaces and create one {c('PENDING')} proposal, and then stops. Separate operator commands "
             f"({c('approve')}, {c('reject')}, {c('execute')}) decide what happens next; {c('execute')} re-checks the "
             f"current state and either commits once or returns a named refusal. {c('tick(long)')} detects bookings on "
             f"closed spaces with caller-supplied time. Policy version 1 stays in force. {c('bash run.sh run scripted')} "
             f"demonstrates every path with replayed responses (not a live model run); {c('bash run.sh run live')} uses the "
             f"same loop with the supplied Ollama adapter. Figures 1–5 follow the text.</p>")

    s.append("<h2>2. Components and dependencies</h2>")
    s.append("<p>The code is split into the four kinds D2.4 names. Dependencies point inward: the model protocol "
             "and the domain know nothing about coordination or storage, and the domain imports nothing outside itself.</p>")
    s.append(table(["Kind", "Classes", "Responsibility", "Depends on"], [
        ["Model protocol", c("Tool") + ", " + c("ToolRequest") + ", " + c("ToolRequestParser"),
         "Turn untrusted reply text into a checked request or a rejection reason: exactly three string properties, "
         "one of four tools, goal booking B1, an empty " + c("targetId") + " except for a proposal naming a known space. "
         "Never runs anything.",
         c("MiniJson") + " (starter); the goal id and space ids are passed in, so it never reads storage"],
        ["Coordination", c("StudentApplication") + ", " + c("AgentRunner") + ", " + c("PeriodicChecker"),
         c("StudentApplication") + " is the only entry point: queries, " + c("propose") + ", operator commands and "
         + c("tick") + ". " + c("AgentRunner") + " owns one conversation: call limit, history, invalid count, "
         "read → list → propose order, stale-version checks and the one-active-proposal rule. " + c("PeriodicChecker") + " owns tick time and "
         "duplicate suppression.",
         "Protocol, domain, storage; " + c("ModelProvider")],
        ["Domain rules", c("Booking") + ", " + c("Space") + ", " + c("Proposal") + ", " + c("ProposalStatus") + ", "
         + c("EligibilityPolicy") + ", " + c("ExecutionOutcome") + ", " + c("DomainRules"),
         "Validation, the policy-1 rule, legal status transitions ("+ c("approve") + "/" + c("reject") + " only from "
         + c("PENDING") + ", " + c("markExecuted") + " only from " + c("APPROVED") + "), and controlled changes: "
         + c("Booking.movedTo") + " and " + c("withDuration") + " return copies with version + 1; " + c("Space")
         + " changes only through " + c("occupy") + ", " + c("release") + ", " + c("close") + ". " + c("ExecutionOutcome")
         + " names the five execution results (Figure 1 draws it with the values returned to callers).",
         "Nothing outside the domain"],
        ["Data storage", c("ParkingStore") + ", " + c("CommitRecord") + ", " + c("Notification"),
         "Holds the booking, spaces, proposals and the commit and simulated-notification records for one application. "
         "Stores and finds; no rules.",
         "Domain"],
        ["Provider boundary (starter)", c("ModelProvider") + ", " + c("ReplayProvider") + ", " + c("OllamaProvider")
         + ", " + c("ChatMessage"),
         c("ReplayProvider") + " is the deterministic test double; " + c("OllamaProvider") + " is the real route. "
         "Both return untrusted text through the same interface, so replay and live share one loop.",
         c("MiniJson")],
    ]))
    s.append(f"<p>Callers only ever receive copies: snapshot maps, {c('ProposalView')}, {c('AgentResult')} (its "
             f"transcript is copied), and immutable records. {c('StudentApplication')} keeps its {c('ParkingStore')} "
             f"and {c('PeriodicChecker')} in private fields and never returns them or the domain objects they hold, so "
             f"every change goes through its checked methods.</p>")

    s.append("<h2>3. Proposal lifecycle</h2>")
    s.append(f"<p>Figure 5 draws these transitions. {c('execute')} checks, in order: the proposal exists and is for "
             f"B1; already {c('EXECUTED')} → {c('DUPLICATE')}; not {c('APPROVED')} → {c('NOT_APPROVED')}; booking or "
             f"policy version differs from the recorded one → {c('STALE')}; target not eligible now → "
             f"{c('UNAVAILABLE_OR_INELIGIBLE')}. Only when all pass does {c('commit')} occupy the target, release the old "
             f"space, swap in the moved booking (version + 1), mark the proposal executed and append one commit and one "
             f"notification. The checks and the commit run in one method with no model or external call in between; "
             f"this is a sequential guarantee only, not safety under concurrent processes.</p>")
    s.append(table(["Status", "approve()", "reject()", "execute()"], [
        [c("PENDING"), "→ " + c("APPROVED"), "→ " + c("REJECTED"), c("NOT_APPROVED") + "; no change"],
        [c("APPROVED"), "refused (" + c("IllegalStateException") + "); no change", "refused; no change",
         "→ " + c("EXECUTED") + " and " + c("COMMITTED") + " if versions match and the target is eligible; otherwise "
         + c("STALE") + " or " + c("UNAVAILABLE_OR_INELIGIBLE") + ", still " + c("APPROVED") + ", no change"],
        [c("REJECTED"), "refused; no change", "refused; no change", c("NOT_APPROVED") + "; no change"],
        [c("EXECUTED"), "refused; no change", "refused; no change", c("DUPLICATE") + "; no second commit or notification"],
    ]))

    s.append("<h2>4. Two design decisions</h2>")
    s.append("<h3>4.1 The loop reaches the domain only through the application's public methods</h3>")
    s.append(f"<p><b>Problem.</b> The model's choices must never bypass a check that an operator call would go "
             f"through, and must never be able to approve or execute. <b>Decision.</b> {c('AgentRunner')} holds a "
             f"{c('StudentApplication')} and calls only its public methods ({c('bookingSnapshot')}, "
             f"{c('alternatives')}, {c('propose')}, {c('policyVersion')}, {c('spaceIds')}). The propose tool goes "
             f"through the same {c('propose')} that D1 tests, and its {c('IllegalArgumentException')} becomes a tool "
             f"error the model can react to. No approve or execute tool exists, and the parser rejects any extra "
             f"property such as {c('\"approved\":true')}, so the only path from a reply to a commit is a separate "
             f"operator command. <b>Alternative rejected:</b> giving the runner the store and the domain objects "
             f"directly. It would save a few methods, but every tool would need its own copy of the eligibility and "
             f"version checks, and a bug there could change state the operator never saw. Enforcing the boundary in "
             f"Java rather than in the system prompt is what the brief requires: the prompt describes the protocol, "
             f"but nothing depends on the model obeying it.</p>")
    s.append("<h3>4.2 Execution returns an outcome enum from one method that both checks and commits</h3>")
    s.append(f"<p><b>Problem.</b> Five results must be distinguishable, tests must assert them with unchanged "
             f"state, and validation must not be separated from mutation. <b>Decision.</b> {c('execute')} returns "
             f"{c('ExecutionOutcome')} and performs every check and the commit itself; {c('commit')} is the only place "
             f"a booking moves. <b>Alternative rejected:</b> one exception type per refusal (as D1's "
             f"{c('propose')} does). Refusals here are normal operator outcomes, not errors, so exceptions would make "
             f"{c('DUPLICATE')} look like a failure and would force callers into control flow by catch blocks. A "
             f"separate {c('canExecute')} query followed by {c('commit')} was also rejected because it reopens the "
             f"check-then-act gap. Unknown proposal ids remain {c('IllegalArgumentException')}: that is a caller "
             f"mistake, not an outcome.</p>")
    s.append(table(["Other choice", "Alternative rejected", "Reason"], [
        [c("Booking") + " stays immutable; changes return copies with version + 1",
         "A mutating " + c("moveTo"), "Every change raises the version exactly once, so stale detection cannot be "
         "skipped; the store swaps the copy in."],
        [c("Space") + " flags change only through " + c("occupy") + "/" + c("release") + "/" + c("close"),
         "Immutable copies like " + c("Booking"), "A space has no version to bump, so copies would add code for nothing; "
         + c("occupy") + " refuses a taken space as a backstop."],
        ["The parser returns a result value", "Throwing on bad input",
         "Bad replies are expected input: the loop must count them and send the reason back."],
        [c("tick(long seconds)") + " takes time as an argument", "An injected " + c("Clock"),
         "The simplest controllable monotonic time; tests and the demo need no waiting."],
    ]))

    s.append("<h2>5. Periodic detection and the conservative retry policy</h2>")
    s.append(f"<p>{c('tick(t)')} rejects a time below the previous tick with a clear "
             f"{c('IllegalArgumentException')} and changes nothing. The first tick scans; after that a scan happens "
             f"on the first tick at or past each boundary of the 60-second grid that starts at the first tick "
             f"(first + 60, first + 120, …), so a late tick does not shift later boundaries. A scan finds bookings on closed spaces and emits a {c('WorkItem')} once per "
             f"(booking, booking version, policy version), and not at all while a {c('PENDING')} or {c('APPROVED')} "
             f"proposal exists for that pair; the agent likewise refuses to propose while such a proposal exists, so "
             f"repeated work never issues a second active proposal for the same state. Ticks 0, 59 and 60 therefore "
             f"give one item, then none (no scan), then none (scan, pair already emitted). "
             f"<b>Retry policy:</b> once a pair has been emitted it is not emitted again on its own, even if the agent "
             f"run failed ({c('STEP_LIMIT')}, {c('PROVIDER_ERROR')}) or the operator rejected the proposal. New work "
             f"needs an explicit operator {c('retry(bookingId)')} or a relevant version change (a changed booking or a "
             f"new policy makes a new pair). This avoids a failing model being called again every minute for the same "
             f"state, at the cost of a human having to notice and retry. {c('tick')} never calls the model or proposes "
             f"anything.</p>")

    s.append("<h2>6. A rejected model/tool interaction</h2>")
    s.append(f"<p>From {c('run scripted')} step 7 (replay, not a live model): the first reply tries to grant itself "
             f"authority. Java rejects it before any tool runs, counts it as an invalid response and tells the model why:</p>")
    s.append("<p><code>[assistant] {\"tool\":\"PROPOSE_REASSIGNMENT\",\"bookingId\":\"B1\",\"targetId\":\"B12\","
             "\"approved\":true}</code><br><code>[user] {\"status\":\"REJECTED\",\"reason\":\"unexpected property: "
             "approved\"}</code></p>")
    s.append(f"<p>Nothing is proposed or approved. After six invalid replies the run ends with {c('STEP_LIMIT')} "
             f"after exactly six calls and no proposal. A valid request used wrongly is a <i>tool error</i> instead: "
             f"proposing D09 returns {c('TOOL_ERROR')} (“Target D09 is not eligible under policy version 1”), and if B1's "
             f"version changes between the read and the list, the list returns {c('TOOL_ERROR')} asking for a new "
             f"{c('READ_BOOKING')}. Tool errors use a call but are not counted as invalid.</p>")

    s.append("<h2>7. Testing and evidence</h2>")
    s.append(f"<p>{c('bash run.sh check')} runs the 14 supplied public checks (plain Java); {c('bash run.sh test')} "
             f"runs 129 JUnit Jupiter tests, all passing: the 14 checks, the infrastructure test and 114 student tests, "
             f"including every D1 regression. Rejection tests compare a captured protected state (booking, all four "
             f"spaces, proposal statuses, commit and notification counts) before and after. {c('TEST_PLAN.md')} maps "
             f"each case; {c('results/d2.txt')} holds the dated output and the tick trace.</p>")
    s.append(table(["Case", "Test (StudentTests)", "Case", "Test (StudentTests)"], [
        ["D2-A", c("d2A_replayStopsAtPendingProposal…"), "D2-H", c("d2H_malformedAndApprovedField…") + ", parser tests"],
        ["D2-B", c("d2B_executingPendingProposal…"), "D2-I", c("d2I_unknownToolAndWrongBooking…")],
        ["D2-C", c("d2C_approvedProposalCommitsOnce"), "D2-J", c("d2J_sixInvalidResponsesStopAtStepLimit")],
        ["D2-D", c("d2D_repeatedExecutionIsDuplicate…"), "D2-K", c("d2K_providerFailureEndsWithProviderError")],
        ["D2-E", c("d2E_rejectedProposalNeverExecutes"), "D2-L", c("d2L_boundaryTicksEmitAtMostOneWorkItem")],
        ["D2-F", c("d2F_changedBookingVersionMakes…Stale"), "D2-M", c("d2M_backwardsClockIsRejected")],
        ["D2-G", c("d2G_occupiedTarget…") + ", " + c("d2G_closedTarget…"), "D2-N", c("results/d2-live.txt")
         + ": " + c("LIVE_RUN_PENDING_COURSE_ACCESS")],
    ]))

    s.append("<h2>8. Limitations</h2>")
    s.append("<ul>"
             f"<li><b>No live transcript yet.</b> No course-supported endpoint was configured, so D2-N is recorded as "
             f"{c('LIVE_RUN_PENDING_COURSE_ACCESS')}. The live route was exercised only against a local fake endpoint "
             f"during development; that is not a model run and is not reported as one.</li>"
             f"<li><b>Sequential and in memory.</b> One process, one fixture, nothing survives a restart; the "
             f"check-then-commit in {c('execute')} is not safe under concurrent callers.</li>"
             f"<li><b>Policy version cannot change in D2,</b> so only the booking-version half of {c('STALE')} is "
             f"exercised; D3's policy 2 will exercise the other half. Accessibility is still ignored, as policy 1 requires.</li>"
             f"<li><b>Work items are reported, not acted on.</b> {c('tick')} returns them; starting an agent run for "
             f"one is left to the operator.</li>"
             f"<li><b>D1's {c('propose')} still throws {c('IllegalArgumentException')}</b> for a bad target, because the "
             f"supplied public checks expect it; the loop converts it into a tool error.</li>"
             "</ul>")

    s.append("<h2>9. Reused infrastructure and corrections</h2>")
    s.append(f"<p><b>Reused unchanged from the starter:</b> {c('ChatMessage')}, {c('Fixture')}, {c('MiniJson')}, "
             f"{c('ModelProvider')}, {c('OllamaProvider')}, {c('ProposalView')}, {c('ReplayProvider')}, the build "
             f"scripts, {c('data/')} and the supplied checks. {c('Main')} keeps the starter's mode structure: "
             f"{c('scripted')} (and its alias {c('agent-scripted')}) is now the D2 demonstration, {c('scripted-d1')} "
             f"keeps the D1 regression demonstration, and {c('live')} asks the operator before anything is approved. "
             f"<b>D1 correction:</b> the D1 {c('DESIGN.pdf')} had been corrupted after printing (blank pages); it was "
             f"rebuilt from its unchanged build script, as recorded in {c('CHANGELOG.md')}. The D1 document is kept "
             f"separately as {c('design/DESIGN_D1.pdf')} (built by {c('design/build_d1.py')}); this file is the D2 "
             f"design.</p>")

    figs = [
        ("Figure 1.", "Class diagram: coordination, storage and domain rules.", class_diagram_core()),
        ("Figure 2.", "Class diagram: the model/tool loop, the protocol classes and the provider boundary.",
         class_diagram_agent()),
        ("Figure 3.", "Sequence diagram: the replayed run reads B1, lists B12 and C03 and proposes B12, stopping at "
                      "the approval boundary (<code>run scripted</code>, step 2).", sequence_run()),
        ("Figure 4.", "Sequence diagram: operator approval, execution with re-checks and commit, and the repeated "
                      "execution (<code>run scripted</code>, steps 4–5).", sequence_execute()),
        ("Figure 5.", "State diagram: proposal statuses and the outcome of each operator command.", state_diagram()),
    ]
    for num, cap, svg in figs:
        s.append(f'<section class="fig"><p class="cap"><b>{num}</b> {cap}</p>{svg}</section>')

    return ('<!doctype html><html lang="en"><head><meta charset="utf-8"><title>ParkOps D2 Design</title>'
            f"<style>{CSS}</style></head><body>{''.join(s)}</body></html>")


def main():
    HTML_OUT.write_text(document(), encoding="utf-8")
    print(f"wrote {HTML_OUT.relative_to(ROOT)}")
    if not pathlib.Path(CHROME).exists():
        sys.exit(f"Chrome not found at {CHROME}; open {HTML_OUT.name} in a browser and print it to PDF instead.")
    if PDF_OUT.exists():
        PDF_OUT.unlink()
    profile = tempfile.mkdtemp(prefix="design-chrome-")
    proc = subprocess.Popen([CHROME, "--headless=new", "--disable-gpu", "--no-pdf-header-footer",
                             f"--user-data-dir={profile}", f"--print-to-pdf={PDF_OUT}", HTML_OUT.as_uri()],
                            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    try:
        # Headless Chrome on macOS can stay open after printing; stop once the file stops growing.
        last, stable = -1, 0
        for _ in range(600):
            if proc.poll() is not None:
                break
            size = PDF_OUT.stat().st_size if PDF_OUT.exists() else -1
            stable = stable + 1 if size > 0 and size == last else 0
            last = size
            if stable >= 10:
                break
            time.sleep(0.1)
    finally:
        if proc.poll() is None:
            proc.terminate()
            try:
                proc.wait(timeout=5)
            except subprocess.TimeoutExpired:
                proc.kill()
        shutil.rmtree(profile, ignore_errors=True)
    if not PDF_OUT.exists():
        sys.exit("Chrome did not produce the PDF")
    print(f"wrote {PDF_OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
