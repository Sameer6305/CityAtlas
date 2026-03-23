import { NextRequest, NextResponse } from 'next/server';

const BACKEND_BASE_URL = process.env.BACKEND_API_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

function buildTargetUrl(path: string[], request: NextRequest): string {
  const upstream = new URL(BACKEND_BASE_URL);
  const cleanBasePath = upstream.pathname.replace(/\/$/, '');
  const joinedPath = path.join('/');

  upstream.pathname = `${cleanBasePath}/${joinedPath}`.replace(/\/+/g, '/');
  upstream.search = request.nextUrl.search;

  return upstream.toString();
}

async function proxyRequest(request: NextRequest, path: string[]) {
  const targetUrl = buildTargetUrl(path, request);

  const headers = new Headers();
  const incomingAuth = request.headers.get('authorization');
  const incomingContentType = request.headers.get('content-type');

  if (incomingAuth) headers.set('authorization', incomingAuth);
  if (incomingContentType) headers.set('content-type', incomingContentType);
  headers.set('accept', 'application/json');

  const method = request.method.toUpperCase();
  const hasBody = !['GET', 'HEAD'].includes(method);

  const response = await fetch(targetUrl, {
    method,
    headers,
    body: hasBody ? await request.text() : undefined,
    cache: 'no-store',
  });

  const responseText = await response.text();
  const contentType = response.headers.get('content-type') || 'application/json';

  return new NextResponse(responseText, {
    status: response.status,
    headers: {
      'content-type': contentType,
      'cache-control': 'no-store',
    },
  });
}

export async function GET(request: NextRequest, context: { params: { path: string[] } }) {
  return proxyRequest(request, context.params.path || []);
}

export async function POST(request: NextRequest, context: { params: { path: string[] } }) {
  return proxyRequest(request, context.params.path || []);
}

export async function PUT(request: NextRequest, context: { params: { path: string[] } }) {
  return proxyRequest(request, context.params.path || []);
}

export async function PATCH(request: NextRequest, context: { params: { path: string[] } }) {
  return proxyRequest(request, context.params.path || []);
}

export async function DELETE(request: NextRequest, context: { params: { path: string[] } }) {
  return proxyRequest(request, context.params.path || []);
}

export async function OPTIONS(request: NextRequest, context: { params: { path: string[] } }) {
  return proxyRequest(request, context.params.path || []);
}
