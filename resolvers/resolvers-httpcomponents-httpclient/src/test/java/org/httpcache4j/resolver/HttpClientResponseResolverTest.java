/*
 * Copyright (c) 2009. The Codehaus. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.httpcache4j.resolver;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.*;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.message.BasicHeader;
import org.apache.commons.io.input.NullInputStream;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import org.codehaus.httpcache4j.resolver.ResponseCreator;
import org.codehaus.httpcache4j.resolver.AbstractResponseCreator;
import org.codehaus.httpcache4j.resolver.StoragePolicy;
import org.codehaus.httpcache4j.*;
import org.codehaus.httpcache4j.payload.Payload;
import org.codehaus.httpcache4j.payload.ClosedInputStreamPayload;
import org.joda.time.DateTime;

import java.net.URI;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:erlend@hamnaberg.net">Erlend Hamnaberg</a>
 * @version $Revision: #5 $ $Date: 2008/09/15 $
 */
public class HttpClientResponseResolverTest {

    private HttpClient client;
    private ResponseCreator creator;

    @Before
    public void setUp() {
        client = mock(HttpClient.class);
        creator = new AbstractResponseCreator(StoragePolicy.NULL) {
            @Override
            protected Payload createCachedPayload(HTTPRequest request, Headers responseHeaders, InputStream stream, MIMEType type) throws IOException {
                return new ClosedInputStreamPayload(type);
            }
        };

    }


    @Test
    public void testSimpleGET() throws IOException {
        HttpClientResponseResolver resolver = new TestableResolver();
        HttpResponse mockedResponse = mock(HttpResponse.class);
        when(mockedResponse.getAllHeaders()).thenReturn(new org.apache.http.Header[0]);
        when(mockedResponse.getStatusLine()).thenReturn(new BasicStatusLine(new HttpVersion(1, 1),200, "OK"));
        when(client.execute(Mockito.<HttpUriRequest>anyObject())).thenReturn(mockedResponse);
        HTTPResponse response = resolver.resolve(new HTTPRequest(URI.create("http://www.vg.no")));
        assertNotNull("Response was null", response);
        assertEquals("Wrong header size", 0, response.getHeaders().size());
        assertFalse("Response did have payload", response.hasPayload());
    }

    @Test
    public void testNotSoSimpleGET() throws IOException {
        HttpClientResponseResolver resolver = new TestableResolver();
        HttpResponse mockedResponse = mock(HttpResponse.class);
        when(mockedResponse.getAllHeaders()).thenReturn(new org.apache.http.Header[] {new BasicHeader("Date", HeaderUtils.toHttpDate("Date", new DateTime()).getValue())});
        when(mockedResponse.getStatusLine()).thenReturn(new BasicStatusLine(new HttpVersion(1, 1),200, "OK"));
        when(mockedResponse.getEntity()).thenReturn(new InputStreamEntity(new NullInputStream(1), 1));
        when(client.execute(Mockito.<HttpUriRequest>anyObject())).thenReturn(mockedResponse);
        HTTPResponse response = resolver.resolve(new HTTPRequest(URI.create("http://www.vg.no")));
        assertNotNull("Response was null", response);
        assertEquals("Wrong header size", 1, response.getHeaders().size());
        assertTrue("Response did not have payload", response.hasPayload());
    }

    @Test
    public void testPUT() throws IOException {
        HttpClientResponseResolver resolver = new TestableResolver();
        HttpResponse mockedResponse = mock(HttpResponse.class);
        when(mockedResponse.getAllHeaders()).thenReturn(new org.apache.http.Header[] {new BasicHeader("Date", HeaderUtils.toHttpDate("Date", new DateTime()).getValue())});
        when(mockedResponse.getStatusLine()).thenReturn(new BasicStatusLine(new HttpVersion(1, 1), 200, "OK"));
        when(mockedResponse.getEntity()).thenReturn(null);
        when(client.execute(Mockito.<HttpUriRequest>anyObject())).thenReturn(mockedResponse);
        HTTPResponse response = resolver.resolve(new HTTPRequest(URI.create("http://www.vg.no"), HTTPMethod.PUT));
        assertNotNull("Response was null", response);
        assertEquals("Wrong header size", 1, response.getHeaders().size());
        assertFalse("Response did have payload", response.hasPayload());
    }

    private class TestableResolver extends HttpClientResponseResolver {
        public TestableResolver() {
            super(HttpClientResponseResolverTest.this.client, HttpClientResponseResolverTest.this.creator);
        }

        @Override
        HttpUriRequest getMethod(HTTPMethod method, URI requestURI) {
            HttpUriRequest request = mock(HttpUriRequest.class);
            when(request.getMethod()).thenReturn(method.name());
            when(request.getURI()).thenReturn(requestURI);
            return request;
        }
    }
}
