#!/usr/bin/python

"""
Runs every day as crontab task to pull down previous day's log from 
Google app engine, and uploads it to S3.
"""

import os
import sys
import shutil
import subprocess
import string

from pytz import timezone
from datetime import datetime, timedelta

settings = {
   'appcfg'        : '<path to gae sdk>/bin/appcfg.sh',
   'email'         : '<gae account>',
   'pwd'           : '<gae account password>',
   'outdir'        : '/tmp/sortbox',
   'repo'          : 'git@github.com:mustpax/sortbox.git',
   'bucket'        : '<S3 bucket for logs>',
   'access_key'    : '',
   'secret_key'    : '',
}

outdir = settings['outdir']
sortboxdir = os.path.join(outdir, 'sortbox')
logdir = os.path.join(outdir, 'logs')
pacific_tz = timezone('US/Pacific')


def cleanup():
    """
    Deletes tmp dir.
    """
    if os.path.exists(outdir):
        print "Deleted %s" % outdir
        shutil.rmtree(outdir)

def clone_repo():
    """
    Clones the remote sortbox repository.
    """
    cleanup()
    subprocess.call("git clone %s %s" % (settings['repo'], sortboxdir), shell=True)


def build_war():
    def touch(fname, times=None):
        """
        Equivalent to unix touch command
        """
        with file(fname, 'a'):
            os.utime(fname, times)

    os.chdir(sortboxdir)
    # Switch to prod branch
    subprocess.call("git checkout prod", shell=True)

    # Create secret.conf
    secret = os.path.join(sortboxdir, 'conf', 'secret.conf')
    touch(secret)

    print "Make all"

    # Build all
    subprocess.call("make all", shell=True)

    war_path = os.path.join(outdir, "sortbox.war")

    print "Build war file"

    # Build war file
    subprocess.call("play war -o %s" % war_path, shell=True)

    if not os.path.exists(war_path):
        print "Failed to create war file"
        exit(2)

def export_log():
    """
    Exports logs from the last 2 days from GAE
    """
    os.chdir(outdir)
    if not os.path.exists(logdir):
        os.mkdir(logdir)

    target = os.path.join(logdir, "raw.txt")

    # Export log for the last 2 day
    subprocess.call("echo %s | %s --num_days=1 --email=%s --severity=1 request_logs sortbox.war %s" \
                        % (settings['pwd'], settings['appcfg'], settings['email'], target), shell=True)

    logfile = os.path.join(logdir, 'raw.txt')

    if not os.path.exists(logfile):
        print "Failed to download log file"
        exit(2)

    print "Saved exported log as %s" % logfile


def format_date(date):
    format = "%d/%b/%Y"
    return date.strftime(format)


def preprocess_log():
    os.chdir(logdir)
    today = format_date(datetime.now(pacific_tz))

    # Remove entries from the 1st day
    subprocess.call("grep -va %s raw.txt > log.tmp.txt" % today, shell=True)

    # Replace null byte delimiters with new line character
    subprocess.call("tr '\\0' '\n' < log.tmp.txt > log.tmp2.txt", shell=True);

    # Remove all lines that starts with ':'
    subprocess.call("sed '/^:/d' log.tmp2.txt > log.txt", shell=True);

    print "Saved preprocessed log as %s" % os.path.join(logdir, 'log.txt')


def upload_log():
    """
    Uploads log file to S3.
    """
    from boto.s3.connection import S3Connection
    from boto.s3.key import Key
    from itertools import takewhile

    yesterday = datetime.now(pacific_tz) - timedelta(1)
    logfile = "log_%s.txt" % string.replace(format_date(yesterday), '/', '_')

    conn = S3Connection(settings['access_key'], settings['secret_key'])
    bucket = conn.create_bucket(settings['bucket'])

    k = bucket.get_key(logfile)
    if not k:
        k = Key(bucket)
        k.key = logfile

        os.chdir(logdir)
        k.set_contents_from_filename('log.txt')

        bucket.set_acl('public-read', k)

        print "Uploaded log file as %s to S3" % k.name
    else:
        print "Log file already uploaded."


def pull_log():
    now = datetime.now()
    print "Start log export: ", now

    clone_repo()
    build_war()
    export_log()
    preprocess_log()
    upload_log()
    cleanup()


def main():
    import time

    start = time.time()
    pull_log()
    duration = time.time() - start

    print "Finished in %d second(s)." % duration


if __name__ == "__main__":
    main()
