#!/usr/bin/env python

import sys

def group_lines(fd):
    buf = []
    for line in fd:
        line = line.rstrip()
        if len(line) and line[0] != '\t':
            yield buf
            buf = [];
        buf.append(line)

def main(pattern):
    import re
    pat = re.compile(pattern)
    for line in group_lines(sys.stdin):
        if any(map(lambda x: re.search(pat, x), line)):
            print '\n'.join(line)

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print 'Grep through Appengine logs, reading from stdin. Usage: ./loggrep.py [regular expression]'
        sys.exit(1)
    main(*sys.argv[1:])
