#!/usr/bin/env python3
"""Builds DESIGN.pdf: the D1 design explanation plus UML class and sequence diagrams.

Usage, from the project root:  python3 design/build.py
Writes design/DESIGN.html and DESIGN.pdf. The PDF step needs Google Chrome
(set CHROME to its executable if it is not in the default macOS location).
When the code changes, update the diagram data below so the figures still match it.
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


def class_diagram():
    p = "c-"
    fixture = Box(20, 30, "Fixture", starter=True, statics="all", ops=[
        "+ booking(): Map<String,Object>",
        "+ spaces(): List<Map<String,Object>>",
        "+ proposalRequest(String): String"])
    main = Box(300, 30, "Main", statics="all", ops=["+ main(String[]): void"])
    replay = Box(520, 30, "ReplayProvider", starter=True,
                 attrs=["- responses: List<String>"],
                 ops=["+ ReplayProvider(List<String>)", "+ next(List<ChatMessage>): String"])
    model = Box(775, 30, "ModelProvider", stereo="«interface»", starter=True,
                ops=["+ next(List<ChatMessage>): String"])
    policy = Box(20, 150, "EligibilityPolicy", statics=["- VERSION: int = 1"],
                 attrs=["- VERSION: int = 1"],
                 ops=["+ getVersion(): int", "+ isEligible(Booking, Space): boolean"])
    rules = Box(20, 250, "DomainRules", stereo="«utility»",
                attrs=["- MIN_HOURS: int = 1", "- MAX_HOURS: int = 24", "- ID_PATTERN: Pattern"],
                ops=["- DomainRules()", "+ requireDuration(int): void", "+ requireIdentifier(String): void"],
                statics=["- MIN_HOURS: int = 1", "- MAX_HOURS: int = 24", "- ID_PATTERN: Pattern",
                         "+ requireDuration(int): void", "+ requireIdentifier(String): void"])
    app = Box(330, 150, "StudentApplication",
              attrs=["- nextProposalNumber: int"],
              ops=["+ StudentApplication()",
                   "+ bookingSnapshot(): Map<String,Object>",
                   "+ spaceSnapshot(String): Map<String,Object>",
                   "+ proposals(): List<ProposalView>",
                   "+ propose(String): ProposalView",
                   "+ run(ModelProvider, int): String",
                   "- nextProposalId(): String",
                   "- bookingFrom(Map<String,Object>): Booking",
                   "- spaceFrom(Map<String,Object>): Space"],
              statics=["- bookingFrom(Map<String,Object>): Booking", "- spaceFrom(Map<String,Object>): Space"])
    view = Box(830, 150, "ProposalView", stereo="«record»", starter=True,
               attrs=["proposalId: String", "bookingId: String", "targetId: String",
                      "bookingVersion: int", "policyVersion: int", "status: String"])
    status = Box(830, 305, "ProposalStatus", stereo="«enumeration»",
                 literals=["PENDING", "APPROVED", "REJECTED", "EXECUTED"])
    booking = Box(20, 420, "Booking",
                  attrs=["- id: String", "- spaceId: String", "- durationHours: int", "- version: int",
                         "- requiresAccessible: boolean"],
                  ops=["+ Booking(String, String, int, int, boolean)", "+ getId(): String",
                       "+ getSpaceId(): String", "+ getDurationHours(): int", "+ getVersion(): int",
                       "+ requiresAccessible(): boolean"])
    space = Box(320, 420, "Space",
                attrs=["- id: String", "- open: boolean", "- occupied: boolean", "- accessible: boolean"],
                ops=["+ Space(String, boolean, boolean, boolean)", "+ getId(): String", "+ isOpen(): boolean",
                     "+ isOccupied(): boolean", "+ isAccessible(): boolean", "+ isAvailable(): boolean"])
    proposal = Box(610, 420, "Proposal",
                   attrs=["- proposalId: String", "- bookingId: String", "- targetId: String",
                          "- observedBookingVersion: int", "- observedPolicyVersion: int"],
                   ops=["+ Proposal(String, String, String, int, int)", "+ getProposalId(): String",
                        "+ getBookingId(): String", "+ getTargetId(): String",
                        "+ getObservedBookingVersion(): int", "+ getObservedPolicyVersion(): int",
                        "+ getStatus(): ProposalStatus", "+ toView(): ProposalView"])
    boxes = [fixture, main, replay, model, policy, rules, app, view, status, booking, space, proposal]

    links = [
        # StudentApplication owns its state
        link(p, app.pt("left", 0.25), policy.pt("right", 0.55), "comp", ends=[("-policy", 6, -6), ("1", 6, 14)]),
        link(p, app.pt("bottom", 0.15), booking.pt("top", 0.6), "comp", ends=[("-booking", -60, -6), ("1", 10, -6)]),
        link(p, app.pt("bottom", 0.45), space.pt("top", 0.5), "comp", ends=[("-spaces", 6, -40), ("1..*", 6, -8)]),
        link(p, app.pt("bottom", 0.85), proposal.pt("top", 0.25), "comp", ends=[("-proposals", 8, -40), ("0..*", 8, -8)]),
        link(p, proposal.pt("top", 0.9), status.pt("bottom", 0.3), "assoc", ends=[("-status", -46, 14), ("1", 6, 14)]),
        # dependencies
        link(p, app.pt("right", 0.35), view.pt("left", 0.45), "dep", "«use»", at=0.5),
        link(p, proposal.pt("top", 0.55), view.pt("left", 0.85), "dep", "«create»", at=0.55, dx=-26),
        link(p, app.pt("right", 0.1), model.pt("bottom", 0.4), "dep", "«use»", at=0.55),
        link(p, replay.pt("right", 0.35), model.pt("left", 0.5), "real"),
        link(p, main.pt("right", 0.5), replay.pt("left", 0.33), "dep", "«use»", at=0.5),
        link(p, main.pt("left", 0.5), fixture.pt("right", 0.33), "dep"),
        link(p, main.pt("bottom", 0.5), app.pt("top", 0.2), "dep", "«create»", at=0.45, dx=34),
        link(p, app.pt("top", 0.05), fixture.pt("bottom", 0.7), "dep", "«use»", at=0.5),
        link(p, app.pt("left", 0.8), rules.pt("right", 0.3), "dep", "«use»", at=0.5),
        link(p, booking.pt("top", 0.25), rules.pt("bottom", 0.3), "dep"),
        link(p, space.pt("top", 0.1), rules.pt("bottom", 0.85), "dep"),
        link(p, proposal.pt("top", 0.05), rules.pt("right", 0.75), "dep"),
    ]
    legend = [
        text(20, 664, "Grey header: supplied starter class, unchanged.  White header: written or completed for D1.", size=10),
        text(20, 680, "Underlined members are static.  Filled diamond: composition (StudentApplication owns the part).  "
                      "Solid line, open arrow: navigable association.", size=10),
        text(20, 696, "Dashed line, open arrow: dependency.  Dashed line, hollow triangle: realization of an interface.  "
                      "Booking, Space, Proposal and StudentApplication validate through DomainRules.", size=10),
    ]
    body = "\n".join([defs(p)] + links + [b.svg() for b in boxes] + legend)
    return f'<svg viewBox="0 0 1000 710" xmlns="http://www.w3.org/2000/svg">{body}</svg>'


# ------------------------------------------------------------- sequence diagram

def sequence_diagram():
    p = "s-"
    lanes = [("main", [":Main"], 50), ("replay", [":ReplayProvider"], 180),
             ("app", ["app:StudentApplication"], 312), ("rules", ["«utility»", "DomainRules"], 432),
             ("policy", [":EligibilityPolicy"], 560), ("b1", ["b1:Booking"], 680),
             ("b12", ["b12:Space"], 795), ("p1", ["p1:Proposal"], 922)]
    X = {k: x for k, _, x in lanes}
    head_y, head_h, step = 14, 34, 19
    msgs = [
        ("call", "main", "replay", "next(history)"),
        ("ret", "replay", "main", 'targetId "B12"'),
        ("self", "main", None, "parse JSON request (MiniJson)"),
        ("call", "main", "app", 'propose("B12")'),
        ("call", "app", "rules", 'requireIdentifier("B12")'),
        ("self", "app", None, 'target = spaces.get("B12")  → b12'),
        ("call", "app", "policy", "isEligible(b1, b12)"),
        ("call", "policy", "b12", "getId()"),
        ("ret", "b12", "policy", '"B12"'),
        ("call", "policy", "b1", "getSpaceId()"),
        ("ret", "b1", "policy", '"A17"'),
        ("call", "policy", "b12", "isAvailable()"),
        ("ret", "b12", "policy", "true (open, not occupied)"),
        ("ret", "policy", "app", "true"),
        ("self", "app", None, 'nextProposalId()  → "P1"'),
        ("call", "app", "b1", "getId()"),
        ("ret", "b1", "app", '"B1"'),
        ("call", "app", "b1", "getVersion()"),
        ("ret", "b1", "app", "0"),
        ("call", "app", "policy", "getVersion()"),
        ("ret", "policy", "app", "1"),
        ("create", "app", "p1", '«create» Proposal("P1", "B1", "B12", 0, 1)'),
        ("self", "app", None, "proposals.add(p1)"),
        ("call", "app", "p1", "toView()"),
        ("ret", "p1", "app", "ProposalView(P1, …, PENDING)"),
        ("ret", "app", "main", "ProposalView(P1, PENDING)"),
        ("self", "main", None, 'print "AWAITING_APPROVAL"'),
    ]
    # y position of each message
    ys, y = [], head_y + head_h + 24
    for kind, *_ in msgs:
        if kind == "create":
            y += 10
        ys.append(y)
        y += step + (10 if kind == "self" else 0) + (10 if kind == "create" else 0)
    boundary_y = y + 14
    bottom = boundary_y + 58
    create_y = ys[[m[0] for m in msgs].index("create")]

    # activation bars: lane -> list of (first message index, last message index)
    acts = {"main": [(0, len(msgs) - 1)], "replay": [(0, 1)], "app": [(3, 25)], "rules": [(4, 4)],
            "policy": [(6, 13), (19, 20)], "b12": [(7, 8), (11, 12)], "b1": [(9, 10), (15, 16), (17, 18)],
            "p1": [(23, 24)]}
    out = [defs(p)]
    for key, lines, x in lanes:
        top = create_y - head_h / 2 if key == "p1" else head_y
        w = max(len(s) for s in lines) * 6.0 + 16
        out.append(f'<line x1="{x}" y1="{top + head_h}" x2="{x}" y2="{bottom}" stroke="#666" stroke-dasharray="4,3"/>')
        out.append(f'<rect x="{x - w / 2:.1f}" y="{top:.1f}" width="{w:.1f}" height="{head_h}" fill="#fff" stroke="#222"/>')
        if len(lines) == 1:
            out.append(text(x, top + 21, lines[0], size=10.5, anchor="middle", bold=True))
        else:
            out.append(text(x, top + 14, lines[0], size=9.5, anchor="middle"))
            out.append(text(x, top + 27, lines[1], size=10.5, anchor="middle", bold=True))
    bars = []
    for key, spans in acts.items():
        for a, b in spans:
            y1 = ys[a] - 6
            y2 = ys[b] + (22 if msgs[b][0] == "self" else 6)
            bars.append(f'<rect x="{X[key] - 5}" y="{y1:.1f}" width="10" height="{y2 - y1:.1f}" fill="#f2f2f2" stroke="#222"/>')

    backgrounds, labels = [], []
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
        if kind == "create":
            end = X[b] - (len("p1:Proposal") * 6.0 + 16) / 2
        else:
            end = x2 - 5 * sign
        dash = ' stroke-dasharray="5,3"' if kind in ("ret", "create") else ""
        marker = "solid" if kind == "call" else "open"
        out.append(f'<line x1="{start:.1f}" y1="{my}" x2="{end:.1f}" y2="{my}" stroke="#222"{dash} marker-end="url(#{p}{marker})"/>')
        bg, fg = label((start + end) / 2, my - 4, s, split=True)
        backgrounds.append(bg)
        labels.append(fg)
    out += backgrounds + bars + labels

    out.append(f'<line x1="12" y1="{boundary_y}" x2="988" y2="{boundary_y}" stroke="#222" stroke-width="2.2" stroke-dasharray="10,5"/>')
    out.append(label(500, boundary_y + 19, "APPROVAL BOUNDARY: only a separate operator action may approve or reject P1 (D2).", size=11))
    out.append(label(500, boundary_y + 36, "D1 stops here. b1 remains at A17, version 0; nothing is approved, executed or moved.", size=10.5))
    out.append(text(12, bottom + 16, "Solid line, filled arrow: call.  Dashed line, open arrow: return or «create».  "
                                     "Shaded bars: activations.  Only calls with a visible effect on this path are shown.", size=10))
    return f'<svg viewBox="0 0 1000 {bottom + 24}" xmlns="http://www.w3.org/2000/svg">{"".join(out)}</svg>'


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
    s.append("<h1>ParkOps D1: Design</h1>")
    s.append('<div class="meta">EECS 3311 B, Fall 2026 · Deliverable 1: design and executable proposal path · '
             "Route A (ParkOps) · Bryan Maristanez · October 2026</div>")

    s.append("<h2>1. Scope</h2>")
    s.append(f"<p>D1 implements the validated domain model and the executable proposal path for the supplied "
             f"fixture. {c('StudentApplication.propose(String)')} reads booking B1, evaluates a target space under "
             f"policy version 1, and stores a {c('PENDING')} proposal that records the booking and policy versions "
             f"it observed. The booking never moves; approval and execution are D2 work. "
             f"{c('bash run.sh run scripted')} demonstrates the accepted target B12 and the rejected targets D09 and "
             f"Z99 using replayed input, which is not a live model run. Figure 1 (class diagram) and Figure 2 "
             f"(sequence diagram) follow the text on separate pages.</p>")

    s.append("<h2>2. Responsibilities</h2>")
    s.append(table(["Class", "Responsibility", "How its state is protected"], [
        [c("DomainRules") + "<br>(starter TODOs completed)",
         f"The single home for the identifier rule {c('[A-Z][A-Z0-9_-]{0,15}')} and the 1–24 hour duration rule.",
         "Private constructor; static guards throw " + c("IllegalArgumentException") + "; the pattern is compiled once as a constant."],
        [c("Booking"),
         "One booking: id, assigned space, duration, version and accessibility requirement.",
         "The constructor validates every argument before assigning; all fields are " + c("private final") + "; getters only."],
        [c("Space"),
         f"One space's id and open, occupied and accessible flags. {c('isAvailable()')} is the only definition of "
         f"“available”: open and not occupied.",
         "Validated id; " + c("private final") + " fields; no setters."],
        [c("ProposalStatus"), f"The four proposal states: {c('PENDING')}, {c('APPROVED')}, {c('REJECTED')}, {c('EXECUTED')}.",
         "An enum, so no other status value can exist."],
        [c("Proposal"),
         "A proposed reassignment, kept separate from the " + c("Booking") + " it would change. Records its id, the "
         "booking, the target space, the observed booking and policy versions, and its status.",
         "Ids and versions are validated; the constructor always sets " + c("PENDING") + " and there is no setter; "
         + c("toView()") + " hands out an immutable " + c("ProposalView") + "."],
        [c("EligibilityPolicy"),
         "Policy version 1: a target is eligible when it is not the booking's current space and is available. "
         "Carries its own version number.",
         "Stateless; " + c("isEligible") + " changes nothing."],
        [c("StudentApplication") + "<br>(starter class rewritten)",
         "Owns the in-memory state of one run: builds a " + c("Booking") + " and four " + c("Space") + " objects from "
         + c("Fixture") + ", stores proposals, generates ids " + c("P1") + ", " + c("P2") + ", …, and implements "
         + c("propose") + ".",
         "Collections are private. Callers receive read-only copies from " + c("bookingSnapshot()") + ", "
         + c("spaceSnapshot(String)") + " and " + c("proposals()") + ". Each instance builds its own objects, so "
         "two applications never share state."],
        [c("Main") + "<br>(starter, " + c("scripted") + " mode extended)",
         "Demonstration: feeds three replayed requests (B12, D09, Z99) to " + c("propose") + " and prints the result "
         "and the booking after each.",
         "Never approves or executes."],
    ]))

    s.append("<h2>3. Design decisions and the alternatives rejected</h2>")
    s.append("<h3>Alternative considered: where the eligibility rule lives</h3>")
    s.append(f"<p>The rule could have been an {c('if')} inside {c('StudentApplication.propose')}, or a method such as "
             f"{c('Space.isEligibleFor(Booking)')}. It was placed in its own class, {c('EligibilityPolicy')}, with its "
             f"version number beside it. D3 replaces policy version 1 with version 2, which also requires an accessible "
             f"target when the booking requires one, and must reject proposals created under the old version. With the "
             f"rule and its version in one object, {c('propose')} records {c('policy.getVersion()')} in every proposal, "
             f"D3 changes a single class, and D2's list-alternatives tool can reuse {c('isEligible')}. An inline "
             f"{c('if')} would have mixed the rule with id generation and storage, and a method on {c('Space')} would "
             f"have made a space depend on a booking policy it should not know about.</p>")
    s.append(table(["Decision", "Alternative rejected", "Reason"], [
        ["The status is the enum " + c("ProposalStatus"), "A " + c("String") + " status",
         "A misspelt status cannot compile, and no status outside the brief's four can exist. " + c("toView()")
         + " converts with " + c("status.name()") + " because the supplied " + c("ProposalView") + " stores a "
         + c("String") + "."],
        [c("Proposal") + "'s constructor always sets " + c("PENDING") + "; no setter",
         "Status as a constructor parameter, or a " + c("setStatus") + " method",
         "No caller can create an already-approved proposal; the brief says approval never comes from the model. "
         "D2 will add controlled transitions that are legal only from " + c("PENDING") + "."],
        ["Callers receive copies: snapshot maps and a list of " + c("ProposalView") + " records",
         "Returning the " + c("Booking") + " and " + c("Space") + " objects or the internal " + c("List<Proposal>"),
         "A caller cannot clear or add stored proposals, or later reach objects that D2 makes mutable "
         "(occupancy, proposal status). This is acceptance case D1-F."],
        ["Domain objects are built from the supplied " + c("Fixture") + " in the constructor",
         "Hard-coding " + c('new Booking("B1", …)') + " inside " + c("StudentApplication"),
         "The fixture stays the single source of truth, and its data passes through the same validating "
         "constructors as any other input."],
    ]))

    s.append("<h2>4. Operation contracts</h2>")
    s.append(f"<h3>4.1 {c('StudentApplication.propose(String targetId): ProposalView')}</h3>")
    s.append("<ul>"
             f"<li><b>Precondition:</b> none beyond the call. {c('targetId')} may be null, malformed or unknown; each "
             f"case is handled.</li>"
             f"<li><b>Postcondition on success:</b> {c('targetId')} is a valid identifier, names a known space, and "
             f"{c('EligibilityPolicy.isEligible(booking, target)')} is true. A new {c('Proposal')} is appended to the "
             f"stored proposals with id {c('\"P\" + n')} (n is the counter's previous value), booking id {c('B1')}, "
             f"target {c('targetId')}, observed booking version equal to the booking's current version (0), observed "
             f"policy version {c('policy.getVersion()')} (1), and status {c('PENDING')}. The method returns that "
             f"proposal's {c('ProposalView')}.</li>"
             f"<li><b>Postcondition on failure:</b> throws {c('IllegalArgumentException')} when {c('targetId')} is "
             f"malformed, unknown or ineligible.</li>"
             f"<li><b>Frame (what may change):</b> only the proposal list (one append) and the id counter (+1), and "
             f"only on success. The booking (its space, version and every other field), every {c('Space')} and every "
             f"existing proposal are unchanged in all cases.</li>"
             "</ul>")
    s.append(f"<h3>4.2 {c('EligibilityPolicy.isEligible(Booking booking, Space target): boolean')}</h3>")
    s.append("<ul>"
             f"<li><b>Precondition:</b> {c('booking')} and {c('target')} are non-null; otherwise "
             f"{c('IllegalArgumentException')}.</li>"
             f"<li><b>Postcondition:</b> returns true exactly when {c('!target.getId().equals(booking.getSpaceId())')}, "
             f"{c('target.isOpen()')} and {c('!target.isOccupied()')}. {c('booking.requiresAccessible()')} does not "
             f"affect the result under version 1. For the fixture, B12 and C03 are eligible; A17 and D09 are not.</li>"
             f"<li><b>Frame:</b> changes nothing; it is a query.</li>"
             "</ul>")

    s.append("<h2>5. Most important current limitation</h2>")
    s.append(f"<p>A proposal stops at {c('PENDING')} and nothing re-checks it. There is no operator approval, "
             f"rejection or execution yet, so the observed booking and policy versions stored in each proposal are "
             f"never compared with the current state, and a proposal that has gone stale cannot be detected. Every "
             f"rejection is also the same {c('IllegalArgumentException')}, distinguished only by its message. D2 must "
             f"add the operator actions, revalidate versions and eligibility inside one sequential boundary before "
             f"committing, and return distinct outcomes ({c('NOT_APPROVED')}, {c('STALE')}, "
             f"{c('UNAVAILABLE_OR_INELIGIBLE')}, {c('COMMITTED')}, {c('DUPLICATE')}).</p>")
    s.append("<p>Other limitations: one in-memory fixture with no persistence; a single sequential process, so no "
             "safety under concurrent access; and accessibility is deliberately ignored until policy version 2 in D3.</p>")

    s.append("<h2>6. Reused infrastructure and implemented parts</h2>")
    s.append(f"<p><b>Reused unchanged from the 2026-09-15 starter:</b> {c('ChatMessage')}, {c('Fixture')}, "
             f"{c('MiniJson')}, {c('ModelProvider')}, {c('OllamaProvider')}, {c('ProposalView')}, {c('ReplayProvider')}, "
             f"the build scripts ({c('run.sh')}, {c('run.ps1')}, {c('setup-junit.sh')}, {c('setup-junit.ps1')}), "
             f"{c('data/')} and the supplied checks ({c('PublicChecks')}, {c('InfrastructureChecks')}, "
             f"{c('SuppliedChecksTest')}, {c('InfrastructureTest')}).</p>")
    s.append(f"<p><b>Implemented for D1:</b> {c('Booking')}, {c('Space')}, {c('Proposal')}, {c('ProposalStatus')} and "
             f"{c('EligibilityPolicy')}; the two validators in {c('DomainRules')}; the domain storage, snapshots and "
             f"{c('propose')} in {c('StudentApplication')} (its {c('run')} method remains the D2 placeholder); the "
             f"three-target {c('scripted')} demonstration in {c('Main')}; and 40 JUnit tests in {c('StudentTests')}.</p>")

    s.append("<h2>7. Testing</h2>")
    s.append(f"<p>{c('bash run.sh check')} runs the 14 supplied public checks as plain Java, and {c('bash run.sh test')} "
             f"runs 55 JUnit Jupiter tests: the same 14 checks, the infrastructure test and 40 student tests. All pass "
             f"({c('results/d1.txt')}, 4 October 2026). Every rejection test asserts the unchanged booking, occupancy "
             f"and proposal list after the exception, not only that an exception was thrown. {c('TEST_PLAN.md')} maps "
             f"acceptance cases D1-A to D1-F to their tests.</p>")

    s.append('<section class="fig"><p class="cap"><b>Figure 1.</b> Class diagram: the D1 domain and application '
             'classes and the starter classes they use.</p>' + class_diagram() + "</section>")
    s.append('<section class="fig"><p class="cap"><b>Figure 2.</b> Sequence diagram: reading B1, evaluating B12 and '
             'creating pending proposal P1 (<code>bash run.sh run scripted</code>, first request), with the approval '
             'boundary.</p>' + sequence_diagram() + "</section>")

    return ('<!doctype html><html lang="en"><head><meta charset="utf-8"><title>ParkOps D1 Design</title>'
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
