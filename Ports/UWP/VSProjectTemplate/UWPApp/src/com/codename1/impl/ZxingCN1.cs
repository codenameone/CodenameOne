using Windows.Media.Capture;

namespace com.codename1.impl
{
    class ZxingCN1 : com.codename1.codescan.CodeScanner
    {

        private WindowsPhone8Demo.Extensions.AsyncPictureDecoderExtension asyncPictureDecoder;
        private codescan.ScanResult result;

        public override void scanBarCode(codescan.ScanResult n1)
        {
            result = n1;
            MediaCapture capture = new MediaCapture();          
        }
        public override void scanQRCode(codescan.ScanResult n1)
        {
            scanBarCode(n1);
        }
    }
}
