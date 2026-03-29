import { NextRequest, NextResponse } from 'next/server';

const BACKEND_BASE_URL = process.env.BACKEND_API_URL || process.env.NEXT_PUBLIC_API_URL;
const UPSTREAM_TIMEOUT_MS = 8000;
const MAX_RETRIES = 1;

function getBackendBaseUrl(): string {
  if (!BACKEND_BASE_URL) {
    throw new Error('BACKEND_API_URL or NEXT_PUBLIC_API_URL must be configured');
  }
  return BACKEND_BASE_URL;
}

function buildTargetUrl(path: string[], request: NextRequest): string {
  const upstream = new URL(getBackendBaseUrl());
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
  const retryableMethod = ['GET', 'HEAD', 'OPTIONS'].includes(method);
  const requestBody = hasBody ? await request.text() : undefined;

  let lastError: unknown = null;

  for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), UPSTREAM_TIMEOUT_MS);

    try {
      const response = await fetch(targetUrl, {
        method,
        headers,
        body: requestBody,
        cache: 'no-store',
        signal: controller.signal,
      });

      const shouldRetry = retryableMethod && response.status >= 500 && attempt < MAX_RETRIES;
      if (shouldRetry) {
        continue;
      }

      const responseText = await response.text();
      const contentType = response.headers.get('content-type') || 'application/json';

      return new NextResponse(responseText, {
        status: response.status,
        headers: {
          'content-type': contentType,
          'cache-control': 'no-store',
        },
      });
    } catch (error) {
      lastError = error;
      if (!(retryableMethod && attempt < MAX_RETRIES)) {
        break;
      }
    } finally {
      clearTimeout(timeoutId);
    }
  }

  const isAbort = lastError instanceof Error && lastError.name === 'AbortError';
  return NextResponse.json({
    error: isAbort ? 'Upstream timeout' : 'Upstream unavailable',
    detail: isAbort
      ? `Backend did not respond within ${UPSTREAM_TIMEOUT_MS}ms`
      : 'Failed to fetch backend response',
  }, {
    status: isAbort ? 504 : 502,
    headers: {
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
