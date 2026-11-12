#!/usr/bin/env python3
"""Read actual JaCoCo XML. Set --minimum 80 to enable the strict business quality gate."""
from pathlib import Path
from xml.etree import ElementTree
import argparse
import os
import sys

parser = argparse.ArgumentParser()
parser.add_argument("--minimum", type=float, default=0)
args = parser.parse_args()
source = Path("target/site/jacoco/jacoco.xml")
if not source.exists():
    print("JaCoCo XML not found. Run: mvn clean verify")
    sys.exit(1)

root = ElementTree.parse(source).getroot()
counters = {c.attrib["type"]: c for c in root.findall("counter")}
data = {}
for kind in ("LINE", "BRANCH", "INSTRUCTION", "METHOD"):
    counter = counters.get(kind)
    if counter is None:
        continue
    missed = int(counter.attrib["missed"])
    covered = int(counter.attrib["covered"])
    percent = 100 * covered / (covered + missed) if covered + missed else 100.0
    data[kind] = percent
    print(f"{kind:<13}: {percent:5.2f}% ({covered} covered, {missed} missed)")

summary = os.environ.get("GITHUB_STEP_SUMMARY")
if summary:
    with open(summary, "a", encoding="utf-8") as f:
        f.write("\n### JaCoCo - cobertura real backend\n\n")
        for k, v in data.items():
            f.write(f"- {k}: **{v:.2f}%**\n")
        f.write("\nLa meta de la rúbrica es al menos 80%; sin alcanzar 80% no declarar la meta cumplida.\n")

if args.minimum and (data.get("LINE", 0) < args.minimum or
                     data.get("BRANCH", 0) < args.minimum):
    print(f"Coverage gate FAIL: need >= {args.minimum:.1f}% lines AND branches.")
    sys.exit(2)
