#!/usr/bin/env python3
"""
diff-triage.py - split a large refactor diff into semantic change vs mechanical churn.

Runs `git diff --numstat` twice over the same range: once plain, once with
`-I<regex> -U0` so that hunks consisting only of import/package lines are
dropped. Comparing the two classifies every path into one of four buckets:

  moved      content byte-identical, path changed        (free for rebasers)
  imports    only import/package lines changed           (conflict hotspot)
  semantic   real code changed                           (needs review)
  binary     git could not produce a line count

Usage:
  ./diff-triage.py                             # summary
  ./diff-triage.py --list semantic             # paths in one bucket
  ./diff-triage.py --patch                     # filtered patch, semantic only
  ./diff-triage.py --by-dir                    # semantic churn rolled up by directory
  ./diff-triage.py --write-baseline .semantic-baseline
  ./diff-triage.py --baseline .semantic-baseline   # ratchet; exits 1 on drift

The REAL usage:
  ./diff-triage.py --list semantic  ':!*.md' ':!*/package-info.java' ':!*.txt'

Defaults to diffing the merge-base of --base against --head.
Everything after `--` is passed to git as a pathspec.
"""

import argparse
import re
import subprocess
import sys
from collections import defaultdict

# Hunks whose changed lines all match one of these are dropped.
DEFAULT_IGNORE = [
    r"^\s*import\s",
    r"^\s*package\s",
]

# Rename/copy detection tuned for a diff far larger than git's default limits.
RENAME_ARGS = ["-M", "-C", "-l0"]


def git(args):
    p = subprocess.run(["git"] + args, capture_output=True, text=True)
    if p.returncode != 0:
        sys.exit(f"git {' '.join(args)} failed:\n{p.stderr.strip()}")
    return p.stdout


NUMSTAT_RE = re.compile(r"^(\d+|-)\t(\d+|-)\t")


def numstat(base, head, extra, pathspec):
    """Return {path: (added, deleted, oldpath_or_None)}. None counts = binary."""
    # --no-patch must precede --numstat: passing -I otherwise makes git emit the
    # full patch after the numstat block, which corrupts parsing.
    out = git(["diff"] + RENAME_ARGS + extra +
              ["--no-patch", "--numstat", "-z", base, head, "--"] + pathspec)
    fields = out.split("\0")
    result = {}
    i = 0
    while i < len(fields):
        f = fields[i]
        i += 1
        if not f:
            continue
        if not NUMSTAT_RE.match(f):
            break  # defensive: anything unparseable ends the numstat block
        adds, dels, rest = f.split("\t", 2)
        if rest == "":
            # rename/copy: the two paths follow as separate NUL-delimited fields
            old, new = fields[i], fields[i + 1]
            i += 2
        else:
            old, new = None, rest
        counts = (None, None) if adds == "-" else (int(adds), int(dels))
        result[new] = (counts[0], counts[1], old)
    return result


def filter_args(ignores, whitespace, blank_lines):
    args = ["-U0", "--ignore-cr-at-eol"]
    for rx in ignores:
        args += ["-I", rx]
    if blank_lines:
        args.append("--ignore-blank-lines")
    if whitespace:
        args.append("-w")
    return args


def classify(raw, filtered):
    buckets = defaultdict(list)
    for path, (adds, dels, old) in raw.items():
        if adds is None:
            buckets["binary"].append((path, 0, old))
            continue
        if adds == 0 and dels == 0:
            buckets["moved"].append((path, 0, old))
            continue
        f_adds, f_dels, _ = filtered.get(path, (0, 0, old))
        f_adds = f_adds or 0
        f_dels = f_dels or 0
        if f_adds == 0 and f_dels == 0:
            buckets["imports"].append((path, adds + dels, old))
        else:
            buckets["semantic"].append((path, f_adds + f_dels, old))
    for b in buckets.values():
        b.sort(key=lambda e: (-e[1], e[0]))
    return buckets


def main():
    ap = argparse.ArgumentParser(add_help=True)
    ap.add_argument("--base", default="1.21.1", help="base branch or ref")
    ap.add_argument("--head", default="HEAD", help="ref under review")
    ap.add_argument("--no-merge-base", action="store_true",
                    help="diff base..head directly instead of merge-base..head")
    ap.add_argument("--ignore", action="append", default=None, metavar="REGEX",
                    help="hunk-ignore regex; repeatable, replaces the defaults")
    ap.add_argument("--ignore-whitespace", action="store_true",
                    help="also pass -w (hides reindentation)")
    ap.add_argument("--no-ignore-blank-lines", action="store_true")
    ap.add_argument("--list", metavar="BUCKET",
                    choices=["moved", "imports", "semantic", "binary"])
    ap.add_argument("--patch", action="store_true",
                    help="print the import-filtered patch for semantic files only")
    ap.add_argument("--by-dir", action="store_true",
                    help="roll semantic churn up by directory")
    ap.add_argument("--depth", type=int, default=4, help="--by-dir depth")
    ap.add_argument("--baseline", metavar="FILE",
                    help="compare the semantic set against FILE; exit 1 on drift")
    ap.add_argument("--write-baseline", metavar="FILE")
    ap.add_argument("pathspec", nargs="*", help="passed to git after --")
    args = ap.parse_args()

    ignores = args.ignore if args.ignore is not None else DEFAULT_IGNORE
    base = args.base
    if not args.no_merge_base:
        base = git(["merge-base", args.base, args.head]).strip()

    fargs = filter_args(ignores, args.ignore_whitespace,
                        not args.no_ignore_blank_lines)
    raw = numstat(base, args.head, [], args.pathspec)
    filtered = numstat(base, args.head, fargs, args.pathspec)
    buckets = classify(raw, filtered)

    semantic_paths = sorted(p for p, _, _ in buckets["semantic"])

    if args.write_baseline:
        with open(args.write_baseline, "w") as fh:
            fh.write("\n".join(semantic_paths) + "\n")
        print(f"wrote {len(semantic_paths)} paths to {args.write_baseline}")
        return 0

    if args.baseline:
        with open(args.baseline) as fh:
            old = {l.strip() for l in fh if l.strip()}
        new = set(semantic_paths)
        added, gone = sorted(new - old), sorted(old - new)
        for p in added:
            print(f"NEW      {p}")
        for p in gone:
            print(f"STALE    {p}")
        if added or gone:
            print(f"\n{len(added)} new, {len(gone)} stale. "
                  f"Re-run with --write-baseline once reviewed.")
            return 1
        print(f"baseline clean: {len(new)} files carry semantic change")
        return 0

    if args.list:
        for path, n, old in buckets[args.list]:
            src = f"  <- {old}" if old else ""
            print(f"{n:>6}  {path}{src}")
        return 0

    if args.by_dir:
        agg = defaultdict(lambda: [0, 0])
        for path, n, _ in buckets["semantic"]:
            d = "/".join(path.split("/")[:args.depth])
            agg[d][0] += n
            agg[d][1] += 1
        for d, (n, files) in sorted(agg.items(), key=lambda kv: -kv[1][0]):
            print(f"{n:>6} lines  {files:>4} files  {d}")
        return 0

    if args.patch:
        if not semantic_paths:
            return 0
        # both sides of a rename must be in the pathspec or git re-reports the
        # file as an addition instead of pairing it with its old path
        spec = set(semantic_paths)
        spec.update(old for _, _, old in buckets["semantic"] if old)
        sys.stdout.write(git(["diff"] + RENAME_ARGS + fargs +
                             [base, args.head, "--"] + sorted(spec)))
        return 0

    total_raw = sum(a + d for a, d, _ in raw.values() if a is not None)
    total_sem = sum(n for _, n, _ in buckets["semantic"])
    print(f"range      {base[:12]}..{args.head}")
    print(f"files      {len(raw)}")
    print(f"raw lines  {total_raw}\n")
    for name in ("semantic", "imports", "moved", "binary"):
        b = buckets[name]
        lines = sum(n for _, n, _ in b)
        print(f"  {name:<9} {len(b):>5} files  {lines:>7} lines")
    if total_raw:
        print(f"\nsemantic change is {100 * total_sem / total_raw:.1f}% "
              f"of the raw line count")
    return 0


if __name__ == "__main__":
    sys.exit(main())
