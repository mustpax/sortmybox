#!/usr/bin/env python
def main():
    import boto
    conn = boto.connect_s3()
    bucket = conn.get_bucket('static.sortmybox.com')
    print bucket.list()

if __name__ == '__main__':
    main()
