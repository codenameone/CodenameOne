using System;
using System.IO;
using System.Net;
using System.Threading.Tasks;

namespace com.codename1.impl
{
    class NetworkOperation 
    {
        private bool responseCompleted;
        private bool postCompleted;
        public HttpWebRequest request;
        ///public HttpClientHandler handler;

        public Stream requestStream
        {
            get
            {
                if (postData == null)
                {
                    request.BeginGetRequestStream(PostCallback, request);
                    while (!postCompleted)
                    {
                        Task.Run(() => Task.Delay(TimeSpan.FromMilliseconds(5))).GetAwaiter().GetResult();
                    }
                }
                return postData;
            }
        }

        private Stream postData;

        public HttpWebResponse response
        {
            get
            {
                if (resp == null)
                {
                    if (postData != null)
                    {
                        postData.Dispose();
                    }
                    request.BeginGetResponse(ResponseCallback, request);

                    while (!responseCompleted)
                    {
                        Task.Run(() => Task.Delay(TimeSpan.FromMilliseconds(5))).GetAwaiter().GetResult();
                    }
                    if (resp == null)
                    {
                        java.io.IOException io = new java.io.IOException();
                        if (error != null)
                        {
                            io = new global::java.io.IOException(error.Message);
                        }
                        else
                        {
                            io = new global::java.io.IOException("Null response");
                        }
                        throw io;
                    }
                }
                return resp;
            }
        }

        private HttpWebResponse resp;
        private WebException error;

        private void ResponseCallback(IAsyncResult asyncResult)
        {
            try
            {
                resp = (HttpWebResponse)request.EndGetResponse(asyncResult);
            }
            catch (WebException we)
            {
                error = we;
                if (we.Response != null)
                {
                    resp = (HttpWebResponse)we.Response;
                }
            }
            responseCompleted = true;
        }

        private void PostCallback(IAsyncResult asyncResult)
        {
            postData = request.EndGetRequestStream(asyncResult);
            postCompleted = true;
        }
    }
}
