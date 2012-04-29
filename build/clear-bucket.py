#!/usr/bin/env python
# Delete all files in the static.sortmybox.com S3 bucket

def main(args):
    if (len(args) == 1) or (args[1] != '-f'):
        print "To delete all files in bucket type: ./clear-bucket -f"
        return
    import boto
    conn = boto.connect_s3()
    bucket = conn.get_bucket('static.sortmybox.com')
    bucket.delete_keys(list(bucket.list()))

if __name__ == '__main__':
    import sys
    main(sys.argv)
