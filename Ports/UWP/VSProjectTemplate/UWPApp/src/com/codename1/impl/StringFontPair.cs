using System;

namespace com.codename1.impl
{
    public class StringFontPair
    {
        public string str;
        public NativeFont font;

        public StringFontPair(string str, NativeFont font)
        {
            this.str = str;
            this.font = font;
        }

        public override bool Equals(Object stfpo)
        {
            StringFontPair stfp = (StringFontPair)stfpo;
            return str.Equals(stfp.str) && font.Equals(stfp.font);
        }

        public override int GetHashCode()
        {
            return str.GetHashCode();
        }
    }
}
