using Microsoft.Graphics.Canvas;
using System;
using Windows.Storage.Streams;

namespace com.codename1.impl
{
    public class SilverlightImageIO : ui.util.ImageIO
    {
        public SilverlightImageIO()
        {
        }

        public override bool isFormatSupported(String n1)
        {
            return FORMAT_JPEG.Equals(n1) || FORMAT_PNG.Equals(n1);
        }

        public override void save(java.io.InputStream image, java.io.OutputStream response, string format, int width, int height, float quality)
        {
            CanvasBitmapFileFormat fileFormat = CanvasBitmapFileFormat.Png;
            if (format.Equals(FORMAT_JPEG))
            {
                fileFormat = CanvasBitmapFileFormat.Jpeg;
            }
            CodenameOneImage img = (CodenameOneImage)SilverlightImplementation.instance.createImage(image);
            CodenameOneImage scaledImage = (CodenameOneImage)SilverlightImplementation.instance.scale(img, width, height);
            InMemoryRandomAccessStream ms = new InMemoryRandomAccessStream();
            scaledImage.image.SaveAsync(ms, fileFormat, quality).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
            ms.Seek(0);
            byte[] buf = new byte[ms.Size];
            DataReader dr = new DataReader(ms);
            dr.LoadAsync((uint)ms.Size).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
            dr.ReadBytes(buf);
            response.write(buf);
        }

        protected override void saveImage(ui.Image image, java.io.OutputStream response, string format, float quality)
        {
            CanvasBitmapFileFormat fileFormat = CanvasBitmapFileFormat.Png;
            if (format.Equals(FORMAT_JPEG))
            {
                fileFormat = CanvasBitmapFileFormat.Jpeg;
            }
            CodenameOneImage img = (CodenameOneImage)image.getImage();
            CanvasBitmap cb = img.image;
            InMemoryRandomAccessStream ms = new InMemoryRandomAccessStream();
            cb.SaveAsync(ms, fileFormat, quality).AsTask().ConfigureAwait(false).GetAwaiter().GetResult(); ;
            ms.Seek(0);
            byte[] buf = new byte[ms.Size];
            DataReader dr = new DataReader(ms);
            dr.LoadAsync((uint)ms.Size).AsTask().ConfigureAwait(false).GetAwaiter().GetResult(); ;
            dr.ReadBytes(buf);
            response.write(buf);
        }

    }
}
