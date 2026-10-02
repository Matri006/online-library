#!/usr/bin/env python3
"""Check HTTP CRUD and shared sessions against an explicitly selected running stand."""
import argparse
import http.cookiejar
import json
import os
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('url', help='First instance, e.g. http://127.0.0.1:8080')
    parser.add_argument('--second-url', help='Second instance using the same database')
    parser.add_argument('--session-file', type=Path, help='Save/load a session to test a process restart')
    args = parser.parse_args()
    base = args.url.rstrip('/')
    second = (args.second_url or base).rstrip('/')
    jar = http.cookiejar.LWPCookieJar(str(args.session_file) if args.session_file else None)
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
    session = {}

    def request(path, method='GET', data=None, target=base, expected=200, form=False):
        headers = {}
        if session:
            headers[session['csrfHeader']] = session['csrfToken']
        if data is not None:
            headers['Content-Type'] = 'application/x-www-form-urlencoded' if form else 'application/json'
            data = (urllib.parse.urlencode(data) if form else json.dumps(data)).encode()
        req = urllib.request.Request(target + path, data=data, headers=headers, method=method)
        try:
            response = opener.open(req, timeout=20)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            body = response.read()
            assert response.status == expected, f'{method} {path}: {response.status}, {body.decode()}'
            return json.loads(body) if body else None

    assert request('/actuator/health')['status'] == 'UP'
    if args.session_file and args.session_file.exists():
        jar.load(ignore_discard=True)
        session = request('/api/session')
        assert session['authenticated'], 'Saved session did not survive restart or has expired'
        print('PASS: saved session remains authenticated')
    else:
        password = os.environ.get('SMOKE_PASSWORD')
        if not password:
            parser.error('Set SMOKE_PASSWORD to the stand administrator password')
        session = request('/api/session')
        request('/api/login', 'POST', {'username': os.environ.get('SMOKE_USERNAME', 'admin'),
                                     'password': password}, form=True)
        session = request('/api/session')
        assert session['authenticated']
    other_session = request('/api/session', target=second)
    assert other_session['authenticated'] and other_session['login'] == session['login']
    print('PASS: session accepted by both endpoints')

    authors = request('/api/references/authors')
    publishers = request('/api/references/publishers')
    assert authors and publishers, 'Enable demo data or create an author and publisher in the UI first'
    title = 'Smoke ' + uuid.uuid4().hex
    data = {'title': title, 'publisherId': publishers[0]['publisher_id'],
            'publicationYear': 2024, 'pagesCount': 100, 'illustrationsCount': 0,
            'price': 250, 'authorIds': [authors[0]['author_id']]}
    book_id = request('/api/books', 'POST', data)['id']
    deleted = False
    try:
        path = '/api/books?q=' + urllib.parse.quote(title)
        assert request(path, target=second)['items'][0]['book_id'] == book_id
        data['title'] = title + ' updated'
        request(f'/api/books/{book_id}', 'PUT', data, target=second)
        assert request(path)['items'][0]['title'] == data['title']
        request(f'/api/books/{book_id}', 'DELETE', target=second, expected=204)
        deleted = True
        assert request(path)['total'] == 0
        request(f'/api/books/{book_id}', 'DELETE', expected=404)
        print('PASS: create, read, update, delete; missing book returns 404')
    finally:
        if not deleted:
            request(f'/api/books/{book_id}', 'DELETE', expected=204)
    if args.session_file:
        # Cookies grant account access: create the local file with owner-only permissions.
        descriptor = os.open(args.session_file, os.O_WRONLY | os.O_CREAT, 0o600)
        os.close(descriptor)
        os.chmod(args.session_file, 0o600)
        jar.save(ignore_discard=True)
    else:
        request('/api/logout', 'POST', expected=204)
        assert not request('/api/session', target=second)['authenticated']
        print('PASS: logout invalidates the shared session')


if __name__ == '__main__':
    main()
