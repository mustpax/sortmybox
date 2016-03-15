#!/usr/bin/env python
import sys

def main(question):
    return 0 if (raw_input(question + ' (y/n) ') in ['y', 'Y']) else 1

if __name__ == '__main__':
    sys.exit(main(*sys.argv[1:]))