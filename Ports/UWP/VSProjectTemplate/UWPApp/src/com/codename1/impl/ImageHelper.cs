using System.Collections.Generic;
using System.Linq;

namespace com.codename1.impl
{
    public class ImageHelper
    {
        public static string GetContentType(byte[] imageBytes)
        {
            imageFormatDecoders.Keys.OrderByDescending(x => x.Length);
            foreach (var kvPair in imageFormatDecoders)
            {
                if (IsMatch(imageBytes, kvPair.Key))
                {
                    return kvPair.Value;
                }
            }
            return "unknown";
        }

        static bool IsMatch(byte[] array, byte[] candidate)
        {
            if (candidate.Length > (array.Length))
                return false;

            for (int i = 0; i < candidate.Length; i++)
                if (array[i] != candidate[i])
                    return false;

            return true;
        }

        private static Dictionary<byte[], string> imageFormatDecoders = new Dictionary<byte[], string>()
        {
            { new byte[]{ 0x42, 0x4D }, "image/x-ms-bmp"},
            { new byte[]{ 0x47, 0x49, 0x46, 0x38, 0x37, 0x61 }, "image/gif" },
            { new byte[]{ 0x47, 0x49, 0x46, 0x38, 0x39, 0x61 }, "image/gif" },
            { new byte[]{ 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A }, "image/png" },
            { new byte[]{ 0xff, 0xd8 }, "image/jpeg" },
        };
    }
}
